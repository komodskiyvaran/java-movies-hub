package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.util.List;


public class MoviesHandler extends BaseHttpHandler implements HttpHandler {
    private final MoviesStore store;
    private final Gson gson = new Gson();


    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        Endpoint endpoint = getEndpoint(ex.getRequestURI().getPath(), ex.getRequestMethod(),
                ex.getRequestURI().getQuery());

        switch (endpoint) {
            case GET_MOVIES -> handleGetMovies(ex);
            //case GET_MOVIES_BY_YEAR -> handleGetMoviesByYear(ex, ex.getRequestURI().getQuery());
        }
    }

    private void handleGetMovies(HttpExchange ex) throws IOException {
        List<Movie> movies = store.getMovies();  // или store.getMovies()
        String json = gson.toJson(movies);
        sendJson(ex, 200, json);
    }

    //private void handleGetMoviesByYear(HttpExchange ex, String query) {}

    private Endpoint getEndpoint(String requestPath, String requestMethod, String query) {
        String[] pathParts = requestPath.split("/");

        switch (requestMethod) {
            case "GET":
                if (pathParts.length == 2 && pathParts[1].equals("movies")) {
                    if (query != null && query.startsWith("year=")) {
                        return Endpoint.GET_MOVIES_BY_YEAR;
                    }
                    return Endpoint.GET_MOVIES;
                }
                if (pathParts.length == 3 && pathParts[1].equals("movies")) {
                    return Endpoint.GET_MOVIE_BY_ID;
                }
                break;
            case "POST":
                if (pathParts.length == 2 && pathParts[1].equals("movies")) {
                    return Endpoint.POST_MOVIE;
                }
                break;
            case "DELETE":
                if (pathParts.length == 3 && pathParts[1].equals("movies")) {
                    return Endpoint.DELETE_MOVIE;
                }
                break;
        }
        return Endpoint.UNKNOWN;
    }

    enum Endpoint {
        GET_MOVIES, POST_MOVIE, GET_MOVIE_BY_ID, DELETE_MOVIE, GET_MOVIES_BY_YEAR, UNKNOWN
    }
}
