package ru.yandex.practicum.filmorate.storage.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
@Qualifier("userDbStorage")
public class UserDbStorage implements UserStorage {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public UserDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public User add(User user) {
        String sql = "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getLogin());
            ps.setString(3, user.getName());
            ps.setDate(4, Date.valueOf(user.getBirthday()));
            return ps;
        }, keyHolder);

        user.setId(keyHolder.getKey().intValue());
        log.debug("Добавлен пользователь с id: {}", user.getId());
        return user;
    }

    @Override
    public User update(User user) {
        String sql = "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE id = ?";
        jdbcTemplate.update(sql, user.getEmail(), user.getLogin(), user.getName(),
                Date.valueOf(user.getBirthday()), user.getId());
        log.debug("Обновлён пользователь с id: {}", user.getId());
        return user;
    }

    @Override
    public void delete(int id) {
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", id);
        log.debug("Удалён пользователь с id: {}", id);
    }

    @Override
    public Optional<User> findById(int id) {
        String sql = "SELECT id, email, login, name, birthday FROM users WHERE id = ?";
        try {
            User user = jdbcTemplate.queryForObject(sql, new UserRowMapper(), id);
            return Optional.ofNullable(user);
        } catch (DataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Collection<User> findAll() {
        String sql = "SELECT id, email, login, name, birthday FROM users ORDER BY id";
        return jdbcTemplate.query(sql, new UserRowMapper());
    }

    @Override
    public void addFriend(int userId, int friendId) {
        // Добавляем дружбу в обе стороны одной транзакцией
        String sql = "MERGE INTO friendships (user_id, friend_id, confirmed) KEY(user_id, friend_id) VALUES (?, ?, true)";
        jdbcTemplate.update(sql, userId, friendId);
        jdbcTemplate.update(sql, friendId, userId);
        log.debug("Пользователь {} добавил в друзья пользователя {}", userId, friendId);
    }

    @Override
    public void removeFriend(int userId, int friendId) {
        String sql = "DELETE FROM friendships WHERE (user_id = ? AND friend_id = ?) OR (user_id = ? AND friend_id = ?)";
        jdbcTemplate.update(sql, userId, friendId, friendId, userId);
        log.debug("Пользователь {} удалил из друзей пользователя {}", userId, friendId);
    }

    @Override
    public List<User> getFriends(int userId) {
        String sql = """
                SELECT u.id, u.email, u.login, u.name, u.birthday
                FROM users u
                INNER JOIN friendships f ON u.id = f.friend_id
                WHERE f.user_id = ? AND f.confirmed = true
                ORDER BY u.id
                """;
        return jdbcTemplate.query(sql, new UserRowMapper(), userId);
    }

    @Override
    public List<User> getCommonFriends(int userId, int otherId) {
        String sql = """
                SELECT u.id, u.email, u.login, u.name, u.birthday
                FROM users u
                INNER JOIN friendships f1 ON u.id = f1.friend_id
                INNER JOIN friendships f2 ON u.id = f2.friend_id
                WHERE f1.user_id = ? AND f1.confirmed = true
                  AND f2.user_id = ? AND f2.confirmed = true
                ORDER BY u.id
                """;
        return jdbcTemplate.query(sql, new UserRowMapper(), userId, otherId);
    }

    private static class UserRowMapper implements RowMapper<User> {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User user = new User();
            user.setId(rs.getInt("id"));
            user.setEmail(rs.getString("email"));
            user.setLogin(rs.getString("login"));
            user.setName(rs.getString("name"));
            user.setBirthday(rs.getDate("birthday").toLocalDate());
            return user;
        }
    }
}