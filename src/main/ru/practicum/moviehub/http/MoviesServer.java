package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpServer;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MoviesServer {
    private final HttpServer server;
    private final MoviesStore store;
    private static final int PORT = 8080;

    public MoviesServer() {
        try {
            // создайте сервер
            server = HttpServer.create(new InetSocketAddress(PORT), 0);

            this.store = new MoviesStore();
            MoviesHandler handler = new MoviesHandler(this.store);
            server.createContext("/movies", handler);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать HTTP-сервер", e);
        }
    }

    public void start() {
        server.start();
        System.out.println("Сервер запущен");
    }

    public void stop() {
        server.stop(1);
        System.out.println("Сервер остановлен");
    }

    public MoviesStore getStore() {
        return store;
    }
}