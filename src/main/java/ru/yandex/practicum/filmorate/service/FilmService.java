package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;
import java.util.List;

@Slf4j
@Service
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    @Autowired
    public FilmService(@Qualifier("filmDbStorage") FilmStorage filmStorage,
                       @Qualifier("userDbStorage") UserStorage userStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
    }

    public Film createFilm(Film film) {
        return filmStorage.add(film);
    }

    public Film updateFilm(Film film) {
        log.debug("Начало обновления фильма: {}", film);

        if (film.getId() == null) {
            throw new NotFoundException("ID фильма не указан");
        }

        Film existingFilm = filmStorage.findById(film.getId())
                .orElseThrow(() -> new NotFoundException("Фильм с id = " + film.getId() + " не найден"));
        log.debug("Найден существующий фильм: {}", existingFilm);

        // Создаем обновленную копию фильма
        Film updatedFilm = new Film();
        updatedFilm.setId(existingFilm.getId());
        updatedFilm.setName(existingFilm.getName());
        updatedFilm.setDescription(existingFilm.getDescription());
        updatedFilm.setReleaseDate(existingFilm.getReleaseDate());
        updatedFilm.setDuration(existingFilm.getDuration());
        updatedFilm.setMpa(existingFilm.getMpa());
        updatedFilm.setGenres(existingFilm.getGenres());
        updatedFilm.setLikes(existingFilm.getLikes());

        // Обновляем только переданные поля
        if (film.getName() != null && !film.getName().isBlank()) {
            updatedFilm.setName(film.getName());
            log.debug("Обновляем название: {}", film.getName());
        }
        if (film.getDescription() != null) {
            updatedFilm.setDescription(film.getDescription());
            log.debug("Обновляем описание: {}", film.getDescription());
        }
        if (film.getReleaseDate() != null) {
            updatedFilm.setReleaseDate(film.getReleaseDate());
            log.debug("Обновляем дату релиза: {}", film.getReleaseDate());
        }
        if (film.getDuration() != null && film.getDuration() > 0) {
            updatedFilm.setDuration(film.getDuration());
            log.debug("Обновляем продолжительность: {}", film.getDuration());
        }
        if (film.getMpa() != null) {
            updatedFilm.setMpa(film.getMpa());
            log.debug("Обновляем MPA: {}", film.getMpa());
        }
        if (film.getGenres() != null) {
            updatedFilm.setGenres(film.getGenres());
            log.debug("Обновляем жанры: {}", film.getGenres());
        }

        Film result = filmStorage.update(updatedFilm);
        log.debug("Фильм успешно обновлен: {}", result);
        return result;
    }

    public Film getFilmById(int id) {
        return filmStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Фильм с id = " + id + " не найден"));
    }

    public Collection<Film> getAllFilms() {
        return filmStorage.findAll();
    }

    public void addLike(int filmId, int userId) {
        Film film = getFilmById(filmId);
        if (userStorage.findById(userId).isEmpty()) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден");
        }

        filmStorage.addLike(filmId, userId);
        log.info("Пользователь {} поставил лайк фильму {}", userId, filmId);
    }

    public void removeLike(int filmId, int userId) {
        Film film = getFilmById(filmId);
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