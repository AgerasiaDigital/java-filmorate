package ru.yandex.practicum.filmorate.model;

import lombok.Data;
import ru.yandex.practicum.filmorate.validation.Create;
import ru.yandex.practicum.filmorate.validation.Update;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Data
public class User {

    @NotNull(groups = Update.class, message = "Id должен быть указан")
    @Null(groups = Create.class, message = "Id должен отсутствовать")
    private Integer id;

    @NotBlank(groups = Create.class, message = "Email не может быть пустым")
    @Email(groups = {Create.class, Update.class}, message = "Email должен быть корректным")
    private String email;

    @NotBlank(groups = Create.class, message = "Логин не может быть пустым")
    @Pattern(regexp = "^\\S+$", groups = {Create.class, Update.class},
            message = "Логин не может содержать пробелы")
    private String login;

    private String name;

    @NotNull(groups = Create.class, message = "Дата рождения обязательна")
    @PastOrPresent(groups = {Create.class, Update.class},
            message = "Дата рождения не может быть в будущем")
    private LocalDate birthday;
}