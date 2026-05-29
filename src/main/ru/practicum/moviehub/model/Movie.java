package ru.practicum.moviehub.model;

import java.util.Objects;

public class Movie {
    private final String title;
    private final int year;
    private final int id;

    public Movie(int id, String title, int year) {
        this.id = id;
        this.title = title;
        this.year = year;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String toString() {
        return "Movie{" +
                "ID ='" + id + '\'' +
                "title='" + title + '\'' +
                ", year=" + year +
                '}';
    }

    public int getId() {
        return id;
    }

    public int getYear() {
        return year;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Movie movie = (Movie) o;
        return year == movie.year &&
                id == movie.id &&
                Objects.equals(title, movie.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, year, id);
    }
}