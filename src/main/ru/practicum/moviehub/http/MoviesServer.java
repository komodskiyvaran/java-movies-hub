package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpServer;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MoviesServer {
    private final HttpServer server;
    private final MoviesStore store;
    private static final int PORT = 8080;
    public static final String MOVIES_PATH = "/movies";


    public MoviesServer() {
        try {
            server = HttpServer.create(new InetSocketAddress(PORT), 0);

            this.store = new MoviesStore();
            MoviesHandler handler = new MoviesHandler(this.store);
            server.createContext(MOVIES_PATH, handler);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать HTTP-сервер", e);
        }
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(1);
    }

    public MoviesStore getStore() {
        return store;
    }
}