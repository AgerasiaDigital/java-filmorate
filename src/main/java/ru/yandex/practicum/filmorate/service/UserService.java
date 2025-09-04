package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

@Slf4j
@Service
public class UserService {
    private final UserStorage userStorage;
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<User> userRowMapper = new RowMapper<User>() {
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
    };

    @Autowired
    public UserService(@Qualifier("userDbStorage") UserStorage userStorage,
                       JdbcTemplate jdbcTemplate) {
        this.userStorage = userStorage;
        this.jdbcTemplate = jdbcTemplate;
    }

    public User createUser(User user) {
        validateUserName(user);
        return userStorage.add(user);
    }

    public User updateUser(User user) {
        User existingUser = userStorage.findById(user.getId())
                .orElseThrow(() -> new NotFoundException("Пользователь с id = " + user.getId() + " не найден"));

        if (user.getEmail() != null) {
            existingUser.setEmail(user.getEmail());
        }
        if (user.getLogin() != null) {
            existingUser.setLogin(user.getLogin());
        }
        if (user.getName() != null) {
            existingUser.setName(user.getName());
        } else if (user.getLogin() != null) {
            existingUser.setName(user.getLogin());
        }
        if (user.getBirthday() != null) {
            existingUser.setBirthday(user.getBirthday());
        }

        validateUserName(existingUser);

        return userStorage.update(existingUser);
    }

    public User getUserById(int id) {
        return userStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id = " + id + " не найден"));
    }

    public Collection<User> getAllUsers() {
        return userStorage.findAll();
    }

    public void addFriend(int userId, int friendId) {
        if (userId == friendId) {
            throw new ValidationException("Нельзя добавить себя в друзья");
        }

        getUserById(userId);
        getUserById(friendId);

        String sql = "MERGE INTO friendships (user_id, friend_id, confirmed) KEY(user_id, friend_id) VALUES (?, ?, false)";
        jdbcTemplate.update(sql, userId, friendId);

        log.info("Пользователь {} отправил заявку в друзья пользователю {}", userId, friendId);
    }

    public void removeFriend(int userId, int friendId) {
        getUserById(userId);
        getUserById(friendId);

        String sql = "DELETE FROM friendships WHERE (user_id = ? AND friend_id = ?) OR (user_id = ? AND friend_id = ?)";
        jdbcTemplate.update(sql, userId, friendId, friendId, userId);

        log.info("Дружба между пользователями {} и {} удалена", userId, friendId);
    }

    public List<User> getFriends(int userId) {
        // Проверяем существование пользователя
        getUserById(userId);

        String sql = "SELECT u.* FROM users u " +
                "JOIN friendships f ON u.id = f.friend_id " +
                "WHERE f.user_id = ? " +
                "ORDER BY u.id";

        return jdbcTemplate.query(sql, userRowMapper, userId);
    }

    public List<User> getCommonFriends(int userId, int otherId) {
        // Проверяем существование пользователей
        getUserById(userId);
        getUserById(otherId);

        String sql = "SELECT u.* FROM users u " +
                "WHERE u.id IN ( " +
                "    SELECT f1.friend_id " +
                "    FROM friendships f1 " +
                "    JOIN friendships f2 ON f1.friend_id = f2.friend_id " +
                "    WHERE f1.user_id = ? AND f2.user_id = ? " +
                ") " +
                "ORDER BY u.id";

        return jdbcTemplate.query(sql, userRowMapper, userId, otherId);
    }

    private void validateUserName(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
    }
}