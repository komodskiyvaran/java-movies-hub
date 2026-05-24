package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


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
            case GET_MOVIE_BY_ID -> handleGetMovieByID(ex);
            case GET_MOVIES_BY_YEAR -> handleGetMoviesByYear(ex);
            case POST_MOVIE -> handlePostMovie(ex);
            case DELETE_MOVIE -> handleDeleteMovie(ex);
            case UNKNOWN -> sendMethodAllowed(ex);
        }
    }

    private void handleGetMovies(HttpExchange ex) throws IOException {
        List<Movie> movies = store.getMovies();
        sendJson(ex, 200, gson.toJson(movies));
    }

    private void handleGetMovieByID(HttpExchange ex) throws IOException {
        String[] pathParts = ex.getRequestURI().getPath().split("/");
        try {
            int id = Integer.parseInt(pathParts[2]);
            Optional<Movie> movie = store.getById(id);

            if (movie.isEmpty()) {
                String errorJson = gson.toJson(new ErrorResponse("Фильм не найден"));
                sendJson(ex, 404, errorJson);
                return;
            }
            sendJson(ex, 200, gson.toJson(movie.get()));

        } catch (NumberFormatException e) {
            String errorJson = gson.toJson(new ErrorResponse("Некорректный ID"));
            sendJson(ex, 400, errorJson);
        }
    }

    private void handleGetMoviesByYear(HttpExchange ex) throws IOException {
        String query = ex.getRequestURI().getQuery();
        try {
            int year = Integer.parseInt(query.substring(query.indexOf('=') + 1));
            List<Movie> movies = store.getByYear(year);
            sendJson(ex, 200, gson.toJson(movies));
        } catch (NumberFormatException e) {
            String errorJson = gson.toJson(new ErrorResponse("Некорректный параметр запроса — 'year'"));
            sendJson(ex, 400, errorJson);
        }
    }

    private void handlePostMovie(HttpExchange ex) throws IOException {
        String contentType = ex.getRequestHeaders().getFirst("Content-Type");
        if (!"application/json".equals(contentType)) {
            sendJson(ex, 415, gson.toJson(new ErrorResponse("Неподдерживаемый Content-Type")));
            return;
        }

        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

        Movie newMovie;
        try {
            newMovie = gson.fromJson(body, Movie.class);
        } catch (JsonSyntaxException e) {
            sendJson(ex, 400, gson.toJson(new ErrorResponse("Некорректный JSON")));
            return;
        }

        List<String> details = new ArrayList<>();
        if (newMovie.getTitle() == null || newMovie.getTitle().trim().isEmpty()) {
            details.add("название не должно быть пустым");
        }
        if (newMovie.getTitle().length() > 100) {
            details.add("название слишком большое");
        }

        int maxYear = LocalDate.now().getYear() + 1;
        if (newMovie.getYear() < 1888 || newMovie.getYear() > maxYear) {
            details.add("год должен быть между 1888 и " + maxYear);
        }

        if (!details.isEmpty()) {
            sendJson(ex, 422, gson.toJson(new ErrorResponse("Ошибка валидации", details)));
            return;
        }

        Movie savedMovie = store.add(newMovie);
        sendJson(ex, 201, gson.toJson(savedMovie));
    }

    private void handleDeleteMovie(HttpExchange ex) throws IOException {
        String[] pathParts = ex.getRequestURI().getPath().split("/");
        try {
            int id = Integer.parseInt(pathParts[2]);
            Optional<Movie> movie = store.getById(id);

            if (movie.isEmpty()) {
                String errorJson = gson.toJson(new ErrorResponse("Фильм не найден"));
                sendJson(ex, 404, errorJson);
                return;
            }
            store.delete(id);
            sendNoContent(ex);

        } catch (NumberFormatException e) {
            String errorJson = gson.toJson(new ErrorResponse("Некорректный ID"));
            sendJson(ex, 400, errorJson);
        }
    }

    private void sendMethodAllowed(HttpExchange ex) throws IOException {
        ex.getResponseHeaders().set("Allow", "GET, POST, DELETE");
        String errorJson = gson.toJson(new ErrorResponse("Метод не поддерживается"));
        sendJson(ex, 405, errorJson);
    }

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