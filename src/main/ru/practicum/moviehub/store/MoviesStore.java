package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MoviesStore {
    private final List<Movie> store = new ArrayList<>();

    public Movie add(Movie movie) {
        store.add(movie);
        return movie;
    }

    public Optional<Movie> getById(int id) {
        return store.stream()
                .filter(movie -> movie.getId() == id)
                .findFirst();
    }

    public List<Movie> getMovies() {
        return new ArrayList<>(store);
    }

    public List<Movie> getByYear(int year) {
        return store.stream()
                .filter(movie -> movie.getYear() == year)
                .toList();
    }

    public boolean delete(int id) {
        return store.removeIf(movie -> movie.getId() == id);
    }

    public void clear() {
        store.clear();
        Movie.resetNextId();  // нужен метод в Movie
    }
}