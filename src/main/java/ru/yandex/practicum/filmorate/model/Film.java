package ru.yandex.practicum.filmorate.model;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import ru.yandex.practicum.filmorate.validation.Create;
import ru.yandex.practicum.filmorate.validation.Update;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Data
public class Film {

    @NotNull(groups = Update.class, message = "ID обязателен для обновления")
    private Long id;

    @NotBlank(groups = Create.class, message = "Название не может быть пустым")
    private String name;

    @Size(max = 200, message = "Максимальная длина описания — 200 символов")
    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate releaseDate;

    @Positive(message = "Продолжительность фильма должна быть положительным числом")
    private Integer duration;
}