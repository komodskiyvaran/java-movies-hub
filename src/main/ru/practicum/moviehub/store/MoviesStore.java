package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class MoviesStore {
    private int count;
    private final HashMap<Integer, Movie> store;

    public MoviesStore() {
        this.store = new HashMap<>();
        count = 0;
    }

    public void add(Movie movie) {
        store.put(count, movie);
        count++;
    }

    public Movie getById(int id) {
        return store.get(id);
    }

    public List<Movie> getMovies() {
        return new ArrayList<>(store.values());
    }

    public List<Movie> getByYear(int year) {
        List<Movie> resList = new ArrayList<>();
        for (Movie movie : store.values()){
            if (movie.getYear() == year) {
                resList.add(movie);
            }
        }
        return resList;
    }

    public boolean delete(int id) {
        if (store.containsKey(id)) {
            store.remove(id);
            return true;
        } else return false;
    }
    public void clear() {
        store.clear();
    }
}