package com.example.paparellena.network;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int PORT = 8888;
    private ServerSocket serverSocket;
    private boolean isRunning;
    private final List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
    private final Gson gson = new Gson();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public interface ServerCallback {
        void onMessageReceived(GameMessage message);
        void onClientConnected(String ip);
    }

    private final ServerCallback callback;

    public Server(ServerCallback callback) {
        this.callback = callback;
    }

    public void start() {
        executor.execute(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                isRunning = true;
                while (isRunning) {
                    Socket socket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(socket);
                    clients.add(handler);
                    executor.execute(handler);
                    if (callback != null) {
                        callback.onClientConnected(socket.getInetAddress().getHostAddress());
                    }
                }
            } catch (IOException e) {
                if (isRunning) e.printStackTrace();
            }
        });
    }

    public void stop() {
        isRunning = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.stop();
            }
            clients.clear();
        }
        executor.shutdownNow();
    }

    public void broadcast(GameMessage message) {
        if (message == null) return;
        String json = gson.toJson(message);
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.sendMessage(json);
            }
        }
    }

    private class ClientHandler implements Runnable {
        private final Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private boolean active = true;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String line;
                while (active && (line = in.readLine()) != null) {
                    try {
                        GameMessage msg = gson.fromJson(line, GameMessage.class);
                        if (msg != null && callback != null) {
                            callback.onMessageReceived(msg);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                if (active) e.printStackTrace();
            } finally {
                stop();
            }
        }

        public void sendMessage(String json) {
            if (active && out != null) {
                executor.execute(() -> {
                    synchronized (this) {
                        if (out != null) {
                            out.println(json);
                        }
                    }
                });
            }
        }

        public void stop() {
            if (!active) return;
            active = false;
            clients.remove(this);
            try {
                if (socket != null) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
