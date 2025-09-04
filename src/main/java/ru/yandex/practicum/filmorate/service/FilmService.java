package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public FilmService(@Qualifier("filmDbStorage") FilmStorage filmStorage,
                       @Qualifier("userDbStorage") UserStorage userStorage,
                       JdbcTemplate jdbcTemplate) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.jdbcTemplate = jdbcTemplate;
    }

    public Film createFilm(Film film) {
        return filmStorage.add(film);
    }

    public Film updateFilm(Film film) {
        Film existingFilm = filmStorage.findById(film.getId())
                .orElseThrow(() -> new NotFoundException("Фильм с id = " + film.getId() + " не найден"));

        Film updatedFilm = new Film();
        updatedFilm.setId(existingFilm.getId());
        updatedFilm.setName(existingFilm.getName());
        updatedFilm.setDescription(existingFilm.getDescription());
        updatedFilm.setReleaseDate(existingFilm.getReleaseDate());
        updatedFilm.setDuration(existingFilm.getDuration());
        updatedFilm.setMpa(existingFilm.getMpa());
        updatedFilm.setGenres(existingFilm.getGenres());

        if (film.getName() != null) {
            updatedFilm.setName(film.getName());
        }
        if (film.getDescription() != null) {
            updatedFilm.setDescription(film.getDescription());
        }
        if (film.getReleaseDate() != null) {
            updatedFilm.setReleaseDate(film.getReleaseDate());
        }
        if (film.getDuration() != null) {
            updatedFilm.setDuration(film.getDuration());
        }
        if (film.getMpa() != null) {
            updatedFilm.setMpa(film.getMpa());
        }
        if (film.getGenres() != null) {
            updatedFilm.setGenres(film.getGenres());
        }

        return filmStorage.update(updatedFilm);
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

        // Добавляем лайк в БД (используем MERGE для H2)
        String sql = "MERGE INTO film_likes (film_id, user_id) KEY(film_id, user_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, filmId, userId);

        log.info("Пользователь {} поставил лайк фильму {}", userId, filmId);
    }

    public void removeLike(int filmId, int userId) {
        Film film = getFilmById(filmId);
        if (userStorage.findById(userId).isEmpty()) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден");
        }

        String sql = "DELETE FROM film_likes WHERE film_id = ? AND user_id = ?";
        jdbcTemplate.update(sql, filmId, userId);

        log.info("Пользователь {} удалил лайк фильму {}", userId, filmId);
    }

    public List<Film> getPopularFilms(int count) {
        String sql = "SELECT f.*, m.name as mpa_name, COUNT(fl.user_id) as likes_count " +
                "FROM films f " +
                "LEFT JOIN mpa m ON f.mpa_id = m.id " +
                "LEFT JOIN film_likes fl ON f.id = fl.film_id " +
                "GROUP BY f.id, f.name, f.description, f.release_date, f.duration, f.mpa_id, m.name " +
                "ORDER BY likes_count DESC, f.id " +
                "LIMIT ?";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
                    Film film = new Film();
                    film.setId(rs.getInt("id"));
                    film.setName(rs.getString("name"));
                    film.setDescription(rs.getString("description"));
                    film.setReleaseDate(rs.getDate("release_date").toLocalDate());
                    film.setDuration(rs.getInt("duration"));

                    Integer mpaId = rs.getInt("mpa_id");
                    if (mpaId != 0) {
                        ru.yandex.practicum.filmorate.model.Mpa mpa = new ru.yandex.practicum.filmorate.model.Mpa();
                        mpa.setId(mpaId);
                        mpa.setName(rs.getString("mpa_name"));
                        film.setMpa(mpa);
                    }

                    return film;
                }, count).stream()
                .map(film -> {
                    return filmStorage.findById(film.getId()).orElse(film);
                })
                .collect(Collectors.toList());
    }
}