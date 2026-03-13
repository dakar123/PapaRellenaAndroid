package com.example.paparellena.network;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private static final int PORT = 8888;
    private ServerSocket serverSocket;
    private boolean isRunning;
    private List<ClientHandler> clients = new ArrayList<>();
    private Gson gson = new Gson();

    public interface ServerCallback {
        void onMessageReceived(GameMessage message);
        void onClientConnected(String ip);
    }

    private ServerCallback callback;

    public Server(ServerCallback callback) {
        this.callback = callback;
    }

    public void start() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                isRunning = true;
                while (isRunning) {
                    Socket socket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(socket);
                    clients.add(handler);
                    new Thread(handler).start();
                    if (callback != null) callback.onClientConnected(socket.getInetAddress().getHostAddress());
                }
            } catch (IOException e) {
                if (isRunning) e.printStackTrace();
            }
        }).start();
    }

    public void stop() {
        isRunning = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (ClientHandler client : clients) {
            client.stop();
        }
        clients.clear();
    }

    public void broadcast(GameMessage message) {
        String json = gson.toJson(message);
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.sendMessage(json);
            }
        }
    }

    private class ClientHandler implements Runnable {
        private Socket socket;
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
                    GameMessage msg = gson.fromJson(line, GameMessage.class);
                    if (callback != null) callback.onMessageReceived(msg);
                    broadcast(msg); // Forward to everyone
                }
            } catch (IOException e) {
                if (active) e.printStackTrace();
            } finally {
                stop();
            }
        }

        public void sendMessage(String json) {
            if (out != null) out.println(json);
        }

        public void stop() {
            active = false;
            try {
                if (socket != null) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
