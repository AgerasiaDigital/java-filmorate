package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import({UserDbStorage.class})
class UserDbStorageTest {

    private final UserDbStorage userStorage;

    @Test
    public void testCreateAndFindUser() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setLogin("testuser");
        user.setName("Test User");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        User createdUser = userStorage.add(user);

        assertThat(createdUser.getId()).isNotNull();
        assertThat(createdUser.getEmail()).isEqualTo("test@example.com");
        assertThat(createdUser.getLogin()).isEqualTo("testuser");
        assertThat(createdUser.getName()).isEqualTo("Test User");
    }

    @Test
    public void testFindUserById() {
        User user = new User();
        user.setEmail("test2@example.com");
        user.setLogin("testuser2");
        user.setName("Test User 2");
        user.setBirthday(LocalDate.of(1985, 5, 15));

        User createdUser = userStorage.add(user);

        Optional<User> foundUser = userStorage.findById(createdUser.getId());

        assertThat(foundUser)
                .isPresent()
                .hasValueSatisfying(u -> {
                    assertThat(u.getId()).isEqualTo(createdUser.getId());
                    assertThat(u.getEmail()).isEqualTo("test2@example.com");
                    assertThat(u.getLogin()).isEqualTo("testuser2");
                });
    }

    @Test
    public void testUpdateUser() {
        User user = new User();
        user.setEmail("original@example.com");
        user.setLogin("original");
        user.setName("Original Name");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        User createdUser = userStorage.add(user);

        createdUser.setName("Updated Name");
        createdUser.setEmail("updated@example.com");

        User updatedUser = userStorage.update(createdUser);

        assertThat(updatedUser.getName()).isEqualTo("Updated Name");
        assertThat(updatedUser.getEmail()).isEqualTo("updated@example.com");
        assertThat(updatedUser.getLogin()).isEqualTo("original");
    }

    @Test
    public void testFindAllUsers() {
        User user1 = new User();
        user1.setEmail("user1@example.com");
        user1.setLogin("user1");
        user1.setName("User 1");
        user1.setBirthday(LocalDate.of(1990, 1, 1));

        User user2 = new User();
        user2.setEmail("user2@example.com");
        user2.setLogin("user2");
        user2.setName("User 2");
        user2.setBirthday(LocalDate.of(1985, 5, 15));

        userStorage.add(user1);
        userStorage.add(user2);

        assertThat(userStorage.findAll()).hasSize(2);
    }
}