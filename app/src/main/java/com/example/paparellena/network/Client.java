package com.example.paparellena.network;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
    private String serverIp;
    private int port;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean isRunning;
    private Gson gson = new Gson();

    public interface ClientCallback {
        void onMessageReceived(GameMessage message);
        void onConnected();
        void onDisconnected();
    }

    private ClientCallback callback;

    public Client(String serverIp, int port, ClientCallback callback) {
        this.serverIp = serverIp;
        this.port = port;
        this.callback = callback;
    }

    public void connect() {
        new Thread(() -> {
            try {
                socket = new Socket(serverIp, port);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                isRunning = true;
                
                if (callback != null) callback.onConnected();

                String line;
                while (isRunning && (line = in.readLine()) != null) {
                    GameMessage msg = gson.fromJson(line, GameMessage.class);
                    if (callback != null) callback.onMessageReceived(msg);
                }
            } catch (IOException e) {
                e.printStackTrace();
                if (callback != null) callback.onDisconnected();
            }
        }).start();
    }

    public void sendMessage(GameMessage message) {
        new Thread(() -> {
            if (out != null) {
                out.println(gson.toJson(message));
            }
        }).start();
    }

    public void disconnect() {
        isRunning = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
