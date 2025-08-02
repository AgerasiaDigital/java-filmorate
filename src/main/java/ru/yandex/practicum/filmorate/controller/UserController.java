package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.validation.Create;
import ru.yandex.practicum.filmorate.validation.Update;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {

    private final Map<Long, User> users = new HashMap<>();
    private long currentId = 1;

    @GetMapping
    public Collection<User> getAllUsers() {
        log.info("Получен запрос на получение всех пользователей");
        return users.values();
    }

    @PostMapping
    public User createUser(@Validated(Create.class) @RequestBody User user) {
        log.info("Получен запрос на создание пользователя: {}", user.getLogin());

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }

        user.setId(currentId++);
        users.put(user.getId(), user);

        log.info("Пользователь создан с ID: {}", user.getId());
        return user;
    }

    @PutMapping
    public User updateUser(@Validated(Update.class) @RequestBody User user) {
        log.info("Получен запрос на обновление пользователя с ID: {}", user.getId());

        if (!users.containsKey(user.getId())) {
            throw new NotFoundException("Пользователь с ID " + user.getId() + " не найден");
        }

        User existingUser = users.get(user.getId());

        // Обновляем только переданные поля
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            existingUser.setEmail(user.getEmail());
        }
        if (user.getLogin() != null && !user.getLogin().isBlank()) {
            existingUser.setLogin(user.getLogin());
        }
        if (user.getName() != null) {
            if (user.getName().isBlank()) {
                existingUser.setName(existingUser.getLogin());
            } else {
                existingUser.setName(user.getName());
            }
        }
        if (user.getBirthday() != null) {
            existingUser.setBirthday(user.getBirthday());
        }

        users.put(existingUser.getId(), existingUser);
        log.info("Пользователь с ID {} обновлен", existingUser.getId());
        return existingUser;
    }
}