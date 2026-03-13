package com.example.paparellena.network;

import java.io.IOException;
import java.net.Socket;

public class Client {
    private String serverIp;
    private int port;
    private Socket socket;

    public Client(String serverIp, int port) {
        this.serverIp = serverIp;
        this.port = port;
    }

    public void connect() {
        new Thread(() -> {
            try {
                socket = new Socket(serverIp, port);
                // Lógica para recibir mensajes del servidor
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void sendMessage(String message) {
        // Enviar mensaje al servidor
    }

    public void disconnect() {
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
