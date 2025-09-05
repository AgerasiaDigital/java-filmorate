package ru.yandex.practicum.filmorate.storage.film;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@Qualifier("filmDbStorage")
@RequiredArgsConstructor
public class FilmDbStorage implements FilmStorage {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Film> filmRowMapper = new RowMapper<Film>() {
        @Override
        public Film mapRow(ResultSet rs, int rowNum) throws SQLException {
            Film film = new Film();
            film.setId(rs.getInt("id"));
            film.setName(rs.getString("name"));
            film.setDescription(rs.getString("description"));
            film.setReleaseDate(rs.getDate("release_date").toLocalDate());
            film.setDuration(rs.getInt("duration"));

            Integer mpaId = rs.getInt("mpa_id");
            if (mpaId != 0) {
                Mpa mpa = new Mpa();
                mpa.setId(mpaId);
                mpa.setName(rs.getString("mpa_name"));
                film.setMpa(mpa);
            }

            return film;
        }
    };

    @Override
    public Film add(Film film) {
        String sql = "INSERT INTO films (name, description, release_date, duration, mpa_id) VALUES (?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setString(1, film.getName());
            ps.setString(2, film.getDescription());
            ps.setDate(3, Date.valueOf(film.getReleaseDate()));
            ps.setInt(4, film.getDuration());
            if (film.getMpa() != null) {
                ps.setInt(5, film.getMpa().getId());
            } else {
                ps.setNull(5, java.sql.Types.INTEGER);
            }
            return ps;
        }, keyHolder);

        film.setId(keyHolder.getKey().intValue());

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            saveFilmGenresBatch(film.getId(), film.getGenres());
        }

        log.debug("Добавлен фильм в БД: {}", film);
        return enrichFilmWithDetails(film);
    }

    @Override
    public Film update(Film film) {
        String sql = "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, mpa_id = ? WHERE id = ?";

        int rowsUpdated = jdbcTemplate.update(sql,
                film.getName(),
                film.getDescription(),
                Date.valueOf(film.getReleaseDate()),
                film.getDuration(),
                film.getMpa() != null ? film.getMpa().getId() : null,
                film.getId());

        if (rowsUpdated == 0) {
            log.warn("Фильм с id = {} не найден при обновлении", film.getId());
            throw new ru.yandex.practicum.filmorate.exception.NotFoundException("Фильм с id = " + film.getId() + " не найден");
        }

        deleteFilmGenres(film.getId());
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            saveFilmGenresBatch(film.getId(), film.getGenres());
        }

        log.debug("Обновлён фильм в БД: {}", film);
        return enrichFilmWithDetails(film);
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM films WHERE id = ?";
        jdbcTemplate.update(sql, id);
        log.debug("Удалён фильм с id: {}", id);
    }

    @Override
    public Optional<Film> findById(int id) {
        String sql = "SELECT f.*, m.name as mpa_name FROM films f LEFT JOIN mpa m ON f.mpa_id = m.id WHERE f.id = ?";
        try {
            Film film = jdbcTemplate.queryForObject(sql, filmRowMapper, id);
            if (film != null) {
                film = enrichFilmWithDetails(film);
            }
            return Optional.ofNullable(film);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public Collection<Film> findAll() {
        String sql = "SELECT f.*, m.name as mpa_name FROM films f LEFT JOIN mpa m ON f.mpa_id = m.id ORDER BY f.id";
        List<Film> films = jdbcTemplate.query(sql, filmRowMapper);

        if (!films.isEmpty()) {
            enrichFilmsWithDetailsBatch(films);
        }

        return films;
    }

    @Override
    public void addLike(int filmId, int userId) {
        String sql = "MERGE INTO film_likes (film_id, user_id) KEY(film_id, user_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, filmId, userId);
    }

    @Override
    public void removeLike(int filmId, int userId) {
        String sql = "DELETE FROM film_likes WHERE film_id = ? AND user_id = ?";
        jdbcTemplate.update(sql, filmId, userId);
    }

    @Override
    public List<Film> getPopularFilms(int count) {
        String sql = "SELECT f.*, m.name as mpa_name, COUNT(fl.user_id) as likes_count " +
                "FROM films f " +
                "LEFT JOIN mpa m ON f.mpa_id = m.id " +
                "LEFT JOIN film_likes fl ON f.id = fl.film_id " +
                "GROUP BY f.id, f.name, f.description, f.release_date, f.duration, f.mpa_id, m.name " +
                "ORDER BY likes_count DESC, f.id " +
                "LIMIT ?";

        List<Film> films = jdbcTemplate.query(sql, filmRowMapper, count);

        if (!films.isEmpty()) {
            enrichFilmsWithDetailsBatch(films);
        }

        return films;
    }

    private Film enrichFilmWithDetails(Film film) {
        film.setGenres(loadFilmGenres(film.getId()));
        film.setLikes(loadFilmLikes(film.getId()));
        return film;
    }

    private void enrichFilmsWithDetailsBatch(List<Film> films) {
        if (films.isEmpty()) return;

        List<Integer> filmIds = films.stream().map(Film::getId).collect(Collectors.toList());

        Map<Integer, Set<Genre>> filmsGenres = loadAllFilmsGenres(filmIds);

        Map<Integer, Set<Integer>> filmsLikes = loadAllFilmsLikes(filmIds);

        for (Film film : films) {
            film.setGenres(filmsGenres.getOrDefault(film.getId(), new LinkedHashSet<>()));
            film.setLikes(filmsLikes.getOrDefault(film.getId(), new HashSet<>()));
        }
    }

    private Map<Integer, Set<Genre>> loadAllFilmsGenres(List<Integer> filmIds) {
        if (filmIds.isEmpty()) return new HashMap<>();

        String placeholders = filmIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = "SELECT fg.film_id, g.id, g.name " +
                "FROM film_genres fg " +
                "JOIN genres g ON fg.genre_id = g.id " +
                "WHERE fg.film_id IN (" + placeholders + ") " +
                "ORDER BY fg.film_id, g.id";

        Map<Integer, Set<Genre>> result = new HashMap<>();

        jdbcTemplate.query(sql, filmIds.toArray(), (rs) -> {
            int filmId = rs.getInt("film_id");
            Genre genre = new Genre();
            genre.setId(rs.getInt("id"));
            genre.setName(rs.getString("name"));

            result.computeIfAbsent(filmId, k -> new LinkedHashSet<>()).add(genre);
        });

        return result;
    }

    private Map<Integer, Set<Integer>> loadAllFilmsLikes(List<Integer> filmIds) {
        if (filmIds.isEmpty()) return new HashMap<>();

        String placeholders = filmIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = "SELECT film_id, user_id FROM film_likes WHERE film_id IN (" + placeholders + ")";

        Map<Integer, Set<Integer>> result = new HashMap<>();

        jdbcTemplate.query(sql, filmIds.toArray(), (rs) -> {
            int filmId = rs.getInt("film_id");
            int userId = rs.getInt("user_id");

            result.computeIfAbsent(filmId, k -> new HashSet<>()).add(userId);
        });

        return result;
    }

    private Set<Genre> loadFilmGenres(int filmId) {
        String sql = "SELECT g.id, g.name FROM genres g JOIN film_genres fg ON g.id = fg.genre_id WHERE fg.film_id = ? ORDER BY g.id";

        List<Genre> genres = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Genre genre = new Genre();
            genre.setId(rs.getInt("id"));
            genre.setName(rs.getString("name"));
            return genre;
        }, filmId);

        return new LinkedHashSet<>(genres);
    }

    private Set<Integer> loadFilmLikes(int filmId) {
        String sql = "SELECT user_id FROM film_likes WHERE film_id = ?";
        List<Integer> likes = jdbcTemplate.queryForList(sql, Integer.class, filmId);
        return new HashSet<>(likes);
    }

    // Batch операция для сохранения жанров
    private void saveFilmGenresBatch(int filmId, Set<Genre> genres) {
        String sql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";

        List<Object[]> batchArgs = genres.stream()
                .map(genre -> new Object[]{filmId, genre.getId()})
                .collect(Collectors.toList());

        jdbcTemplate.batchUpdate(sql, batchArgs);
    }

    private void deleteFilmGenres(int filmId) {
        String sql = "DELETE FROM film_genres WHERE film_id = ?";
        jdbcTemplate.update(sql, filmId);
    }
}