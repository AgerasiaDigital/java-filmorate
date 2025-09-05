package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
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
@Repository
@Qualifier("filmDbStorage")
public class FilmDbStorage implements FilmStorage {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public FilmDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

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
            ps.setInt(5, film.getMpa().getId());
            return ps;
        }, keyHolder);

        film.setId(keyHolder.getKey().intValue());

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            addGenresToFilm(film.getId(), film.getGenres());
        }

        log.debug("Добавлен фильм с id: {}", film.getId());
        return findById(film.getId()).orElse(film);
    }

    @Override
    public Film update(Film film) {
        String sql = "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, mpa_id = ? WHERE id = ?";
        jdbcTemplate.update(sql, film.getName(), film.getDescription(),
                Date.valueOf(film.getReleaseDate()), film.getDuration(),
                film.getMpa().getId(), film.getId());

        updateFilmGenres(film.getId(), film.getGenres());

        log.debug("Обновлён фильм с id: {}", film.getId());
        return findById(film.getId()).orElse(film);
    }

    @Override
    public void delete(int id) {
        jdbcTemplate.update("DELETE FROM films WHERE id = ?", id);
        log.debug("Удалён фильм с id: {}", id);
    }

    @Override
    public Optional<Film> findById(int id) {
        String sql = """
                SELECT f.id, f.name, f.description, f.release_date, f.duration,
                       m.id as mpa_id, m.name as mpa_name,
                       g.id as genre_id, g.name as genre_name
                FROM films f
                LEFT JOIN mpa m ON f.mpa_id = m.id
                LEFT JOIN film_genres fg ON f.id = fg.film_id
                LEFT JOIN genres g ON fg.genre_id = g.id
                WHERE f.id = ?
                ORDER BY g.id
                """;

        List<Film> films = jdbcTemplate.query(sql, new FilmWithGenresExtractor(), id);
        return films.isEmpty() ? Optional.empty() : Optional.of(films.get(0));
    }

    @Override
    public Collection<Film> findAll() {
        String sql = """
                SELECT f.id, f.name, f.description, f.release_date, f.duration,
                       m.id as mpa_id, m.name as mpa_name,
                       g.id as genre_id, g.name as genre_name
                FROM films f
                LEFT JOIN mpa m ON f.mpa_id = m.id
                LEFT JOIN film_genres fg ON f.id = fg.film_id
                LEFT JOIN genres g ON fg.genre_id = g.id
                ORDER BY f.id, g.id
                """;

        return jdbcTemplate.query(sql, new FilmWithGenresExtractor());
    }

    @Override
    public void addLike(int filmId, int userId) {
        String sql = "MERGE INTO film_likes (film_id, user_id) KEY(film_id, user_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, filmId, userId);
        log.debug("Добавлен лайк от пользователя {} к фильму {}", userId, filmId);
    }

    @Override
    public void removeLike(int filmId, int userId) {
        String sql = "DELETE FROM film_likes WHERE film_id = ? AND user_id = ?";
        jdbcTemplate.update(sql, filmId, userId);
        log.debug("Удалён лайк от пользователя {} к фильму {}", userId, filmId);
    }

    @Override
    public List<Film> getPopularFilms(int count) {
        String sql = """
                SELECT f.id, f.name, f.description, f.release_date, f.duration,
                       m.id as mpa_id, m.name as mpa_name,
                       g.id as genre_id, g.name as genre_name,
                       COUNT(fl.user_id) as likes_count
                FROM films f
                LEFT JOIN mpa m ON f.mpa_id = m.id
                LEFT JOIN film_genres fg ON f.id = fg.film_id
                LEFT JOIN genres g ON fg.genre_id = g.id
                LEFT JOIN film_likes fl ON f.id = fl.film_id
                GROUP BY f.id, f.name, f.description, f.release_date, f.duration, 
                         m.id, m.name, g.id, g.name
                ORDER BY likes_count DESC, f.id, g.id
                LIMIT ?
                """;

        return jdbcTemplate.query(sql, new FilmWithGenresExtractor(), count);
    }

    private void addGenresToFilm(int filmId, Set<Genre> genres) {
        if (genres == null || genres.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
        List<Object[]> batchArgs = genres.stream()
                .map(genre -> new Object[]{filmId, genre.getId()})
                .collect(Collectors.toList());

        jdbcTemplate.batchUpdate(sql, batchArgs);
    }

    private void updateFilmGenres(int filmId, Set<Genre> genres) {
        jdbcTemplate.update("DELETE FROM film_genres WHERE film_id = ?", filmId);

        addGenresToFilm(filmId, genres);
    }

    private static class FilmWithGenresExtractor implements ResultSetExtractor<List<Film>> {
        @Override
        public List<Film> extractData(ResultSet rs) throws SQLException, DataAccessException {
            Map<Integer, Film> filmMap = new LinkedHashMap<>();

            while (rs.next()) {
                int filmId = rs.getInt("id");

                Film film = filmMap.get(filmId);
                if (film == null) {
                    film = new Film();
                    film.setId(filmId);
                    film.setName(rs.getString("name"));
                    film.setDescription(rs.getString("description"));
                    film.setReleaseDate(rs.getDate("release_date").toLocalDate());
                    film.setDuration(rs.getInt("duration"));

                    // MPA
                    Mpa mpa = new Mpa();
                    mpa.setId(rs.getInt("mpa_id"));
                    mpa.setName(rs.getString("mpa_name"));
                    film.setMpa(mpa);

                    film.setGenres(new LinkedHashSet<>());
                    filmMap.put(filmId, film);
                }

                int genreId = rs.getInt("genre_id");
                if (!rs.wasNull()) {
                    Genre genre = new Genre();
                    genre.setId(genreId);
                    genre.setName(rs.getString("genre_name"));
                    film.getGenres().add(genre);
                }
            }

            return new ArrayList<>(filmMap.values());
        }
    }
}