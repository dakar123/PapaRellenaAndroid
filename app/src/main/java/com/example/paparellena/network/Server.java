package com.example.paparellena.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private static final int PORT = 8888;
    private ServerSocket serverSocket;
    private boolean isRunning;
    private List<Socket> clientSockets;

    public Server() {
        this.clientSockets = new ArrayList<>();
    }

    public void startServer() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                isRunning = true;
                while (isRunning) {
                    Socket clientSocket = serverSocket.accept();
                    clientSockets.add(clientSocket);
                    // Aquí se manejaría la conexión de cada cliente en un hilo separado
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void stopServer() {
        isRunning = false;
        try {
            if (serverSocket != null) serverSocket.close();
            for (Socket s : clientSockets) s.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void broadcastMessage(String message) {
        // Lógica para enviar mensaje a todos los clientes
    }
}
