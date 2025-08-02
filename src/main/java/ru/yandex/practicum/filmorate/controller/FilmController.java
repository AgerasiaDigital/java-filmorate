package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.validation.Create;
import ru.yandex.practicum.filmorate.validation.Update;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/films")
public class FilmController {

    private final Map<Long, Film> films = new HashMap<>();
    private long currentId = 1;

    @GetMapping
    public Collection<Film> getAllFilms() {
        log.info("Получен запрос на получение всех фильмов");
        return films.values();
    }

    @PostMapping
    public Film createFilm(@Validated(Create.class) @RequestBody Film film) {
        log.info("Получен запрос на создание фильма: {}", film.getName());

        film.setId(currentId++);
        films.put(film.getId(), film);

        log.info("Фильм создан с ID: {}", film.getId());
        return film;
    }

    @PutMapping
    public Film updateFilm(@Validated(Update.class) @RequestBody Film film) {
        log.info("Получен запрос на обновление фильма с ID: {}", film.getId());

        if (!films.containsKey(film.getId())) {
            throw new NotFoundException("Фильм с ID " + film.getId() + " не найден");
        }

        Film existingFilm = films.get(film.getId());

        // Обновляем только переданные поля
        if (film.getName() != null && !film.getName().isBlank()) {
            existingFilm.setName(film.getName());
        }
        if (film.getDescription() != null) {
            existingFilm.setDescription(film.getDescription());
        }
        if (film.getReleaseDate() != null) {
            existingFilm.setReleaseDate(film.getReleaseDate());
        }
        if (film.getDuration() != null) {
            existingFilm.setDuration(film.getDuration());
        }

        films.put(existingFilm.getId(), existingFilm);
        log.info("Фильм с ID {} обновлен", existingFilm.getId());
        return existingFilm;
    }
}