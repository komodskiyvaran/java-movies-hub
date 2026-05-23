package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.model.Movie;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoviesApiTest {
    private static final String BASE = "http://localhost:8080";
    private static MoviesServer server;
    private static HttpClient client;
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    private static final Gson gson = new Gson();


    @BeforeAll
    static void beforeAll() {
        server = new MoviesServer();
        server.start();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @BeforeEach
    void beforeEach() {
        server.getStore().clear();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));

        assertEquals(200, resp.statusCode());
        assertContentType(resp);


        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"));
    }

    @Test
    void getMovies_returnArrayMovies() throws Exception {
        Movie movie1 = new Movie("1+1", 2011);
        Movie movie2 = new Movie("Форрест Гамп", 1994);

        server.getStore().add(movie1);
        server.getStore().add(movie2);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");
        assertContentType(resp);

        Movie[] movies = gson.fromJson(resp.body(), Movie[].class);

        assertEquals(2, movies.length);

        assertTrue(List.of(movies).contains(movie1));
        assertTrue(List.of(movies).contains(movie2));
    }

    @Test
    void getMoviesById_returnsMovie() throws Exception {
        server.getStore().add(new Movie("1+1",2011));
        server.getStore().add(new Movie("Форрест Гамп",1994));
        server.getStore().add(new Movie("Зелёная миля",1990));
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/2"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));

        assertEquals(200, resp.statusCode());
        assertContentType(resp);

        JsonObject jsonObject = JsonParser.parseString(resp.body()).getAsJsonObject();

        String title = jsonObject.get("title").getAsString();
        int year = jsonObject.get("year").getAsInt();

        assertEquals("Зелёная миля", title);
        assertEquals(1990, year);
    }

    @Test
    void getMoviesById_returnsNoFound() throws Exception {
        server.getStore().add(new Movie("1+1", 2011));
        server.getStore().add(new Movie("Форрест Гамп", 1994));
        server.getStore().add(new Movie("Зелёная миля", 1990));
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/3"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));

        assertEquals(404, resp.statusCode());
        assertContentType(resp);

        JsonObject jsonObject = JsonParser.parseString(resp.body()).getAsJsonObject();
        String error = jsonObject.get("error").getAsString();
        assertEquals("Фильм не найден", error);
    }

    @Test
    void getMoviesById_returnsBadRequest() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/3d2"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));

        assertEquals(400, resp.statusCode());
        assertContentType(resp);

        JsonObject jsonObject = JsonParser.parseString(resp.body()).getAsJsonObject();
        String error = jsonObject.get("error").getAsString();
        assertEquals("Некорректный ID", error);
    }

    @Test
    void getMoviesByYear_returnsMovies() throws Exception {
        Movie movie1 = new Movie("1+1", 2011);
        Movie movie2 = new Movie("Тор", 2011);
        Movie movie3 = new Movie("Тор", 2011);

        server.getStore().add(movie1);
        server.getStore().add(new Movie("Форрест Гамп",1994));
        server.getStore().add(movie2);
        server.getStore().add(new Movie("Зелёная миля",1990));
        server.getStore().add(movie3);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2011"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));

        assertEquals(200, resp.statusCode());
        assertContentType(resp);

        Movie[] movies = gson.fromJson(resp.body(), Movie[].class);

        assertEquals(3, movies.length);

        assertTrue(List.of(movies).contains(movie1));
        assertTrue(List.of(movies).contains(movie2));
        assertTrue(List.of(movies).contains(movie3));
    }
    @Test
    void getMoviesByYear_returnsBadRequest() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2011f"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));

        assertEquals(400, resp.statusCode());
        assertContentType(resp);

        JsonObject jsonObject = JsonParser.parseString(resp.body()).getAsJsonObject();
        String error = jsonObject.get("error").getAsString();
        assertEquals("Некорректный параметр запроса — 'year'", error);
    }

    //@Test
    void postMovie_whenValid_returns201AndMovie() throws Exception {
        String jsonMovie = "{\"title\":\"Интерстеллар\",\"year\":2014}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonMovie))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));
        assertContentType(resp);

        JsonObject jsonObject = JsonParser.parseString(resp.body()).getAsJsonObject();

        assertTrue(jsonObject.has("id"));
        assertEquals("Интерстеллар", jsonObject.get("title").getAsString());
        assertEquals(2014, jsonObject.get("year").getAsInt());

        assertEquals(1, server.getStore().getMovies().size());
    }


    private void assertContentType(HttpResponse<?> response) {
        String contentType = response.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentType);
    }
}