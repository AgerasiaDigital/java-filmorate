package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;
import java.util.List;

@Slf4j
@Service
public class UserService {
    private final UserStorage userStorage;

    @Autowired
    public UserService(@Qualifier("userDbStorage") UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public User createUser(User user) {
        if (user.getName() == null || user.getName().trim().isEmpty()) {
            user.setName(user.getLogin());
        }
        User createdUser = userStorage.add(user);
        log.info("Создан пользователь с id: {}", createdUser.getId());
        return createdUser;
    }

    public User updateUser(User user) {
        if (userStorage.findById(user.getId()).isEmpty()) {
            throw new NotFoundException("Пользователь с id = " + user.getId() + " не найден");
        }
        if (user.getName() == null || user.getName().trim().isEmpty()) {
            user.setName(user.getLogin());
        }
        User updatedUser = userStorage.update(user);
        log.info("Обновлён пользователь с id: {}", updatedUser.getId());
        return updatedUser;
    }

    public User getUserById(int id) {
        return userStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id = " + id + " не найден"));
    }

    public Collection<User> getAllUsers() {
        return userStorage.findAll();
    }

    public void addFriend(int userId, int friendId) {
        if (userStorage.findById(userId).isEmpty()) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден");
        }
        if (userStorage.findById(friendId).isEmpty()) {
            throw new NotFoundException("Пользователь с id = " + friendId + " не найден");
        }

        userStorage.addFriend(userId, friendId);
        log.info("Пользователь {} добавил в друзья пользователя {}", userId, friendId);
    }

    public void removeFriend(int userId, int friendId) {
        if (userStorage.findById(userId).isEmpty()) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден");
        }
        if (userStorage.findById(friendId).isEmpty()) {
            throw new NotFoundException("Пользователь с id = " + friendId + " не найден");
        }

        userStorage.removeFriend(userId, friendId);
        log.info("Пользователь {} удалил из друзей пользователя {}", userId, friendId);
    }

    public List<User> getFriends(int userId) {
        if (userStorage.findById(userId).isEmpty()) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден");
        }
        return userStorage.getFriends(userId);
    }

    public List<User> getCommonFriends(int userId, int otherId) {
        if (userStorage.findById(userId).isEmpty()) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден");
        }
        if (userStorage.findById(otherId).isEmpty()) {
            throw new NotFoundException("Пользователь с id = " + otherId + " не найден");
        }
        return userStorage.getCommonFriends(userId, otherId);
    }
}