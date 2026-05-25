package ru.practicum.moviehub.http;

import com.google.gson.*;
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
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static ru.practicum.moviehub.http.MoviesServer.MOVIES_PATH;

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
                .uri(URI.create(BASE + MOVIES_PATH))
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
        Movie added1 = server.getStore().add(new Movie(0, "1+1", 2011));
        Movie added2 = server.getStore().add(new Movie(0, "Форрест Гамп", 1994));
        Movie added3 = server.getStore().add(new Movie(0, "Человек Дождя", 1988));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + MOVIES_PATH))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));

        assertEquals(200, resp.statusCode());
        assertContentType(resp);

        List<Movie> movieList = gson.fromJson(resp.body(), new ListOfMoviesTypeToken().getType());

        assertEquals(3, movieList.size());

        List<String> titles = movieList.stream().map(Movie::getTitle).toList();
        assertTrue(titles.contains(added1.getTitle()));
        assertTrue(titles.contains(added2.getTitle()));
        assertTrue(titles.contains(added3.getTitle()));
    }

    @Test
    void getMoviesById_returnsMovie() throws Exception {
        Movie added1 = server.getStore().add(new Movie(0, "1+1", 2011));
        Movie added2 = server.getStore().add(new Movie(0, "Форрест Гамп", 1994));
        Movie added3 = server.getStore().add(new Movie(0, "Зелёная миля", 1990));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + MOVIES_PATH + "/" + added3.getId()))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));

        assertEquals(200, resp.statusCode());
        assertContentType(resp);

        Movie receivedMovie = gson.fromJson(resp.body(), Movie.class);
        assertEquals(added3.getTitle(), receivedMovie.getTitle());
        assertEquals(added3.getYear(), receivedMovie.getYear());
    }

    @Test
    void getMoviesById_returnsNoFound() throws Exception {
        Movie added1 = server.getStore().add(new Movie(0, "1+1", 2011));
        Movie added2 = server.getStore().add(new Movie(0, "Форрест Гамп", 1994));

        int nonExistentId = added2.getId() + 100;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + MOVIES_PATH + "/" + nonExistentId))
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
                .uri(URI.create(BASE + MOVIES_PATH + "/3d2"))
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
        Movie added1 = server.getStore().add(new Movie(0, "1+1", 2011));
        Movie added2 = server.getStore().add(new Movie(0, "Тор", 2011));
        server.getStore().add(new Movie(0, "Форрест Гамп", 1994));
        Movie added3 = server.getStore().add(new Movie(0, "Прислуга", 2011));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + MOVIES_PATH + "?year=2011"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));

        assertEquals(200, resp.statusCode());
        assertContentType(resp);

        List<Movie> movies = gson.fromJson(resp.body(), new ListOfMoviesTypeToken().getType());

        assertEquals(3, movies.size());
        List<String> titles = movies.stream().map(Movie::getTitle).toList();
        assertTrue(titles.contains(added1.getTitle()));
        assertTrue(titles.contains(added2.getTitle()));
        assertTrue(titles.contains(added3.getTitle()));
    }

    @Test
    void getMoviesByYear_404returns_NotFound() throws Exception {
        server.getStore().add(new Movie(0, "1+1", 2011));
        server.getStore().add(new Movie(0, "Тор", 2011));
        server.getStore().add(new Movie(0, "Прислуга", 2011));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + MOVIES_PATH + "?year=2013"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));

        assertEquals(200, resp.statusCode());
        assertContentType(resp);

        List<Movie> movies = gson.fromJson(resp.body(), new ListOfMoviesTypeToken().getType());

        assertEquals(0, movies.size());
    }

    @Test
    void getMoviesByYear_returnsBadRequest() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + MOVIES_PATH + "?year=2011f"))
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

    @Test
    void postMovie_whenValid_returns201_Create() throws Exception {
        Movie movie = new Movie(0, "Интерстеллар", 2014);
        String jsonMovie = gson.toJson(movie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + MOVIES_PATH))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonMovie))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));
        assertContentType(resp);
        assertEquals(201, resp.statusCode());

        Movie createdMovie = gson.fromJson(resp.body(), Movie.class);

        assertEquals("Интерстеллар", createdMovie.getTitle());
        assertEquals(2014, createdMovie.getYear());
        assertEquals(1, server.getStore().getMovies().size());
    }

    @Test
    void postMovie_returns422_EmptyTitle() throws Exception {
        Movie movie = new Movie(0, " ", 2001);
        String jsonMovie = gson.toJson(movie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + MOVIES_PATH))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonMovie))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));
        assertContentType(resp);
        assertEquals(422, resp.statusCode());

        JsonObject jsonObject = JsonParser.parseString(resp.body()).getAsJsonObject();
        String error = jsonObject.get("error").getAsString();

        assertEquals("Ошибка валидации", error);

        JsonArray details = jsonObject.get("details").getAsJsonArray();

        assertEquals(1, details.size());
        assertTrue(details.toString().contains("название не должно быть пустым"));
        assertEquals(0, server.getStore().getMovies().size());
    }

    @Test
    void postMovie_returns422_TooMachTitle() throws Exception {
        char[] chars = new char[101];
        Arrays.fill(chars, 'f');
        String title = new String(chars);

        Movie movie = new Movie(0, title, 2001);
        String jsonMovie = gson.toJson(movie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + MOVIES_PATH))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonMovie))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));
        assertContentType(resp);
        assertEquals(422, resp.statusCode());

        JsonObject jsonObject = JsonParser.parseString(resp.body()).getAsJsonObject();
        String error = jsonObject.get("error").getAsString();
        assertEquals("Ошибка валидации", error);

        JsonArray details = jsonObject.get("details").getAsJsonArray();

        assertEquals(1, details.size());
        assertTrue(details.toString().contains("название слишком большое"));
        assertEquals(0, server.getStore().getMovies().size());
    }

    @Test
    void postMovie_returns422_InvalidYearAndEmptyTitle() throws Exception {
        Movie movie = new Movie(0, " ", 2042);
        String jsonMovie = gson.toJson(movie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + MOVIES_PATH))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonMovie))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));
        assertContentType(resp);

        assertEquals(422, resp.statusCode());

        JsonObject jsonObject = JsonParser.parseString(resp.body()).getAsJsonObject();
        String error = jsonObject.get("error").getAsString();
        assertEquals("Ошибка валидации", error);

        JsonArray details = jsonObject.get("details").getAsJsonArray();

        assertEquals(2, details.size());
        assertTrue(details.toString().contains("название не должно быть пустым"));
        assertTrue(details.toString().contains("год должен быть между 1888 и " + (LocalDate.now().getYear() + 1)));

        assertEquals(0, server.getStore().getMovies().size());
    }

    @Test
    void postMovie_returns415_UnsupportedMediaType() throws Exception {
        Movie movie = new Movie(0,"Прислуга", 2011);
        String jsonMovie = gson.toJson(movie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + MOVIES_PATH))
                .header("Content-Type", "application/html")
                .POST(HttpRequest.BodyPublishers.ofString(jsonMovie))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));

        assertEquals(415, resp.statusCode());

        JsonObject jsonObject = JsonParser.parseString(resp.body()).getAsJsonObject();
        String error = jsonObject.get("error").getAsString();

        assertEquals("Неподдерживаемый Content-Type", error);
        assertEquals(0, server.getStore().getMovies().size());
    }

    @Test
    void postMovie_returns400_InvalidJson() throws Exception {
        String invalidJson = "{\"title\":\"Интерстеллар\", \"year\":}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + MOVIES_PATH))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(invalidJson))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));

        assertEquals(400, resp.statusCode());
        assertContentType(resp);

        JsonObject jsonObject = JsonParser.parseString(resp.body()).getAsJsonObject();
        String error = jsonObject.get("error").getAsString();

        assertEquals("Некорректный JSON", error);
        assertEquals(0, server.getStore().getMovies().size());
    }

    @Test
    void deleteMovie_returns204_NoContent() throws  Exception {
        Movie added1 = server.getStore().add(new Movie(0, "1+1", 2011));
        Movie added2 = server.getStore().add(new Movie(0, "Корпорация монстров", 2001));
        Movie added3 = server.getStore().add(new Movie(0, "Валли", 2008));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + MOVIES_PATH + "/" + added2.getId()))
                .DELETE()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));

        assertEquals(204, resp.statusCode());
        assertTrue(resp.body().isEmpty());

        List<Movie> movies = server.getStore().getMovies();

        assertEquals(2, movies.size());
        assertTrue(movies.contains(added1));
        assertFalse(movies.contains(added2));
        assertTrue(movies.contains(added3));
    }

    @Test
    void deleteMovie_returns404_NotFound() throws  Exception {
        Movie movie1 = new Movie(0, "1+1", 2011);
        Movie movie2 = new Movie(0, "Корпорация монстров", 2001);

        Movie movieNotMovieStore = new Movie(2, "Валли", 2008);

        server.getStore().add(movie1);
        server.getStore().add(movie2);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + MOVIES_PATH + "/" + movieNotMovieStore.getId()))
                .DELETE()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));

        assertEquals(404, resp.statusCode());
        assertEquals(2, server.getStore().getMovies().size());

        JsonObject jsonObject = JsonParser.parseString(resp.body()).getAsJsonObject();
        String error = jsonObject.get("error").getAsString();

        assertEquals("Фильм не найден", error);
    }

    @Test
    void deleteMovie_returns400_invalidID() throws Exception {
        Movie movie1 = new Movie(0, "1+1", 2011);
        Movie movie2 = new Movie(1, "Корпорация монстров", 2001);

        server.getStore().add(movie1);
        server.getStore().add(movie2);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + MOVIES_PATH + "/2F"))
                .DELETE()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));

        assertEquals(400, resp.statusCode());
        assertEquals(2, server.getStore().getMovies().size());

        JsonObject jsonObject = JsonParser.parseString(resp.body()).getAsJsonObject();
        String error = jsonObject.get("error").getAsString();

        assertEquals("Некорректный ID", error);
    }

    @Test
    void methodNotAllowed_returns405() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + MOVIES_PATH))
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));

        assertEquals(405, resp.statusCode());
        assertEquals("GET, POST, DELETE", resp.headers().firstValue("Allow").orElse(""));

        JsonObject jsonObject = JsonParser.parseString(resp.body()).getAsJsonObject();
        String error = jsonObject.get("error").getAsString();

        assertEquals("Метод не поддерживается", error);
    }

    private void assertContentType(HttpResponse<?> response) {
        String contentType = response.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentType);
    }
}