package ru.yandex.practicum.filmorate.model;

import lombok.Data;
import ru.yandex.practicum.filmorate.validation.Create;
import ru.yandex.practicum.filmorate.validation.ReleaseDateConstraint;
import ru.yandex.practicum.filmorate.validation.Update;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
public class Film {
    @NotNull(groups = Update.class, message = "Id должен быть указан")
    @Null(groups = Create.class, message = "Id должен отсутствовать")
    private Integer id;

    @NotBlank(groups = Create.class, message = "Название не может быть пустым")
    private String name;

    @Size(max = 200, groups = {Create.class, Update.class}, message = "Максимальная длина описания — 200 символов")
    private String description;

    @NotNull(groups = Create.class, message = "Дата релиза обязательна")
    @ReleaseDateConstraint(groups = {Create.class, Update.class})
    private LocalDate releaseDate;

    @NotNull(groups = Create.class, message = "Продолжительность обязательна")
    @Positive(groups = {Create.class, Update.class}, message = "Продолжительность должна быть положительным числом")
    private Integer duration;

    private Set<Integer> likes = new HashSet<>();

    public void addLike(Integer userId) {
        likes.add(userId);
    }

    public void removeLike(Integer userId) {
        likes.remove(userId);
    }
}