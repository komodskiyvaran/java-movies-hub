package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.*;

public class MoviesStore {
    private final Map<Integer, Movie> store = new HashMap<>();
    private int nextID = 0;

    public Movie add(Movie movie) {
        Movie newMovie = new Movie(nextID, movie.getTitle(), movie.getYear());
        store.put(nextID, newMovie);
        nextID++;
        return newMovie;
    }

    public Optional<Movie> getById(int id) {
        return Optional.ofNullable(store.get(id));
    }

    public List<Movie> getMovies() {
        return new ArrayList<>(store.values());
    }

    public List<Movie> getByYear(int year) {
        return store.values().stream()
                .filter(movie -> movie.getYear() == year)
                .toList();
    }

    public boolean delete(int id) {
        return store.remove(id) != null;
    }

    public void clear() {
        store.clear();
        nextID = 0;
    }
}