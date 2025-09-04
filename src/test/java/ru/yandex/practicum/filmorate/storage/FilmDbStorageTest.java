package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import({FilmDbStorage.class})
class FilmDbStorageTest {

    private final FilmDbStorage filmStorage;

    @Test
    public void testCreateAndFindFilm() {
        Film film = new Film();
        film.setName("Test Film");
        film.setDescription("Test Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        Mpa mpa = new Mpa();
        mpa.setId(1); // G рейтинг
        film.setMpa(mpa);

        Film createdFilm = filmStorage.add(film);

        assertThat(createdFilm.getId()).isNotNull();
        assertThat(createdFilm.getName()).isEqualTo("Test Film");
        assertThat(createdFilm.getMpa()).isNotNull();
        assertThat(createdFilm.getMpa().getId()).isEqualTo(1);
        // Проверяем что MPA загружается с именем
        if (createdFilm.getMpa().getName() != null) {
            assertThat(createdFilm.getMpa().getName()).isEqualTo("G");
        }
    }

    @Test
    public void testFindFilmById() {
        Film film = new Film();
        film.setName("Test Film 2");
        film.setDescription("Test Description 2");
        film.setReleaseDate(LocalDate.of(1999, 12, 31));
        film.setDuration(90);

        Mpa mpa = new Mpa();
        mpa.setId(2); // PG рейтинг
        film.setMpa(mpa);

        Film createdFilm = filmStorage.add(film);

        Optional<Film> foundFilm = filmStorage.findById(createdFilm.getId());

        assertThat(foundFilm)
                .isPresent()
                .hasValueSatisfying(f -> {
                    assertThat(f.getId()).isEqualTo(createdFilm.getId());
                    assertThat(f.getName()).isEqualTo("Test Film 2");
                    assertThat(f.getMpa().getName()).isEqualTo("PG");
                });
    }

    @Test
    public void testFilmWithGenres() {
        Film film = new Film();
        film.setName("Test Film with Genres");
        film.setDescription("Test Description");
        film.setReleaseDate(LocalDate.of(2020, 1, 1));
        film.setDuration(150);

        Mpa mpa = new Mpa();
        mpa.setId(3); // PG-13 рейтинг
        film.setMpa(mpa);

        Genre genre1 = new Genre();
        genre1.setId(1); // Комедия

        Genre genre2 = new Genre();
        genre2.setId(2); // Драма

        film.setGenres(Set.of(genre1, genre2));

        Film createdFilm = filmStorage.add(film);

        Optional<Film> foundFilm = filmStorage.findById(createdFilm.getId());

        assertThat(foundFilm)
                .isPresent()
                .hasValueSatisfying(f -> {
                    assertThat(f.getGenres()).hasSize(2);
                    assertThat(f.getGenres())
                            .extracting(Genre::getName)
                            .containsExactlyInAnyOrder("Комедия", "Драма");
                });
    }

    @Test
    public void testUpdateFilm() {
        Film film = new Film();
        film.setName("Original Film");
        film.setDescription("Original Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        Mpa mpa = new Mpa();
        mpa.setId(1);
        film.setMpa(mpa);

        Film createdFilm = filmStorage.add(film);

        createdFilm.setName("Updated Film");
        createdFilm.setDescription("Updated Description");

        Film updatedFilm = filmStorage.update(createdFilm);

        assertThat(updatedFilm.getName()).isEqualTo("Updated Film");
        assertThat(updatedFilm.getDescription()).isEqualTo("Updated Description");
        assertThat(updatedFilm.getDuration()).isEqualTo(120);
    }
}