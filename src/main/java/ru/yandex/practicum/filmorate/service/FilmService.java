package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreStorage;
import ru.yandex.practicum.filmorate.storage.mpa.MpaStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final MpaStorage mpaStorage;
    private final GenreStorage genreStorage;

    @Autowired
    public FilmService(@Qualifier("filmDbStorage") FilmStorage filmStorage,
                       @Qualifier("userDbStorage") UserStorage userStorage,
                       MpaStorage mpaStorage,
                       GenreStorage genreStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.mpaStorage = mpaStorage;
        this.genreStorage = genreStorage;
    }

    public Film createFilm(Film film) {
        validateFilmData(film);
        Film createdFilm = filmStorage.add(film);
        log.info("Создан фильм с id: {}", createdFilm.getId());
        return createdFilm;
    }

    public Film updateFilm(Film film) {
        if (filmStorage.findById(film.getId()).isEmpty()) {
            throw new NotFoundException("Фильм с id = " + film.getId() + " не найден");
        }
        validateFilmData(film);
        Film updatedFilm = filmStorage.update(film);
        log.info("Обновлён фильм с id: {}", updatedFilm.getId());
        return updatedFilm;
    }

    private void validateFilmData(Film film) {
        if (film.getMpa() != null && film.getMpa().getId() != null) {
            if (mpaStorage.findById(film.getMpa().getId()).isEmpty()) {
                throw new NotFoundException("Рейтинг MPA с id = " + film.getMpa().getId() + " не найден");
            }
        }

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            validateGenres(film.getGenres());
        }
    }

    private void validateGenres(Set<Genre> genres) {
        Set<Integer> genreIds = genres.stream()
                .map(Genre::getId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        if (genreIds.isEmpty()) {
            return;
        }

        Collection<Genre> existingGenres = genreStorage.findAll();
        Set<Integer> existingGenreIds = existingGenres.stream()
                .map(Genre::getId)
                .collect(Collectors.toSet());

        for (Integer genreId : genreIds) {
            if (!existingGenreIds.contains(genreId)) {
                throw new NotFoundException("Жанр с id = " + genreId + " не найден");
            }
        }
    }

    public Film getFilmById(int id) {
        return filmStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Фильм с id = " + id + " не найден"));
    }

    public Collection<Film> getAllFilms() {
        return filmStorage.findAll();
    }

    public void addLike(int filmId, int userId) {
        if (filmStorage.findById(filmId).isEmpty()) {
            throw new NotFoundException("Фильм с id = " + filmId + " не найден");
        }
        if (userStorage.findById(userId).isEmpty()) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден");
        }

        filmStorage.addLike(filmId, userId);
        log.info("Пользователь {} поставил лайк фильму {}", userId, filmId);
    }

    public void removeLike(int filmId, int userId) {
        if (filmStorage.findById(filmId).isEmpty()) {
            throw new NotFoundException("Фильм с id = " + filmId + " не найден");
        }
        if (userStorage.findById(userId).isEmpty()) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден");
        }

        filmStorage.removeLike(filmId, userId);
        log.info("Пользователь {} удалил лайк фильму {}", userId, filmId);
    }

    public List<Film> getPopularFilms(int count) {
        return filmStorage.getPopularFilms(count);
    }
}