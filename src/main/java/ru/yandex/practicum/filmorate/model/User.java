package ru.yandex.practicum.filmorate.model;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import ru.yandex.practicum.filmorate.validation.Create;
import ru.yandex.practicum.filmorate.validation.Update;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Data
public class User {

    @NotNull(groups = Update.class, message = "ID обязателен для обновления")
    private Long id;

    @NotBlank(groups = {Create.class}, message = "Электронная почта не может быть пустой")
    @Email(groups = {Create.class, Update.class}, message = "Электронная почта должна содержать символ @")
    private String email;

    @NotBlank(groups = {Create.class}, message = "Логин не может быть пустым")
    @Pattern(regexp = "^\\S+$", groups = {Create.class, Update.class}, message = "Логин не может содержать пробелы")
    private String login;

    private String name;

    @PastOrPresent(groups = {Create.class, Update.class}, message = "Дата рождения не может быть в будущем")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;
}