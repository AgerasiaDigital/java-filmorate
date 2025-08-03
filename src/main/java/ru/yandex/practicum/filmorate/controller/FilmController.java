package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.validation.Create;
import ru.yandex.practicum.filmorate.validation.Update;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/films")
@Validated
public class FilmController {

    private final Map<Integer, Film> films = new HashMap<>();
    private int nextId = 1;

    @GetMapping
    public List<Film> getAllFilms() {
        log.info("Получение списка всех фильмов. Количество: {}", films.size());
        return new ArrayList<>(films.values());
    }

    @PostMapping
    public Film createFilm(@Validated(Create.class) @RequestBody Film film) {
        log.info("Создание нового фильма: {}", film.getName());

        film.setId(nextId++);
        films.put(film.getId(), film);

        log.info("Фильм создан с ID: {}", film.getId());
        return film;
    }

    @PutMapping
    public Film updateFilm(@Validated(Update.class) @RequestBody Film film) {
        log.info("Обновление фильма с ID: {}", film.getId());

        if (!films.containsKey(film.getId())) {
            log.error("Фильм с ID {} не найден", film.getId());
            throw new NotFoundException("Фильм с id = " + film.getId() + " не найден");
        }

        Film existingFilm = films.get(film.getId());

        if (film.getName() != null) {
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