package com.example.paparellena.game;

import com.example.paparellena.network.Client;
import com.example.paparellena.network.GameMessage;
import com.example.paparellena.network.Server;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import android.os.Handler;
import android.os.Looper;

public class GameManager {
    private static GameManager instance;
    private List<Player> players = new ArrayList<>();
    private Player localPlayer;
    private boolean isHost;
    private boolean isGameRunning;
    private int remainingTime;
    
    private Server server;
    private Client client;
    private Handler hostTimerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    public interface GameEventListener {
        void onPlayerJoined(Player player);
        void onGameStarted(String starterId);
        void onPotatoReceived();
        void onPotatoPassed();
        void onGameOver(String loserId);
        void onTick(int seconds);
    }
    
    private GameEventListener listener;

    private GameManager() {}

    public static synchronized GameManager getInstance() {
        if (instance == null) {
            instance = new GameManager();
        }
        return instance;
    }

    public void setListener(GameEventListener listener) {
        this.listener = listener;
    }

    public void initAsHost(String name, String ip) {
        this.isHost = true;
        this.localPlayer = new Player(ip, name, ip);
        players.clear();
        players.add(localPlayer);
        
        server = new Server(new Server.ServerCallback() {
            @Override
            public void onMessageReceived(GameMessage message) {
                handleMessage(message);
            }

            @Override
            public void onClientConnected(String ip) {
                // El cliente se unirá enviando un mensaje TYPE_JOIN
            }
        });
        server.start();
        
        // El host también es su propio cliente
        initAsClient(name, ip, "localhost");
    }

    public void initAsClient(String name, String myIp, String serverIp) {
        this.isHost = (serverIp.equals("localhost") || serverIp.equals(myIp));
        this.localPlayer = new Player(myIp, name, myIp);
        
        client = new Client(serverIp, 8888, new Client.ClientCallback() {
            @Override
            public void onMessageReceived(GameMessage message) {
                if (!isHost) handleMessage(message);
            }

            @Override
            public void onConnected() {
                client.sendMessage(new GameMessage(GameMessage.TYPE_JOIN, localPlayer.getId(), localPlayer.getName()));
            }

            @Override
            public void onDisconnected() {}
        });
        client.connect();
    }

    public void handleMessage(GameMessage msg) {
        switch (msg.getType()) {
            case GameMessage.TYPE_JOIN:
                Player newPlayer = new Player(msg.getSenderId(), msg.getContent(), msg.getSenderId());
                if (!players.contains(newPlayer)) {
                    players.add(newPlayer);
                    if (listener != null) listener.onPlayerJoined(newPlayer);
                }
                break;
            case GameMessage.TYPE_START:
                isGameRunning = true;
                if (listener != null) listener.onGameStarted(msg.getContent());
                if (msg.getContent().equals(localPlayer.getId())) {
                    localPlayer.setHasPotato(true);
                    if (listener != null) listener.onPotatoReceived();
                }
                break;
            case GameMessage.TYPE_PASS_POTATO:
                if (msg.getContent().equals(localPlayer.getId())) {
                    localPlayer.setHasPotato(true);
                    if (listener != null) listener.onPotatoReceived();
                } else {
                    localPlayer.setHasPotato(false);
                    if (listener != null) listener.onPotatoPassed();
                }
                break;
            case GameMessage.TYPE_TICK:
                remainingTime = Integer.parseInt(msg.getContent());
                if (listener != null) listener.onTick(remainingTime);
                break;
            case GameMessage.TYPE_GAME_OVER:
                isGameRunning = false;
                stopTimer();
                if (listener != null) listener.onGameOver(msg.getContent());
                break;
        }
    }

    public void startGame() {
        if (!isHost || players.size() < 2) return;
        
        isGameRunning = true;
        remainingTime = new Random().nextInt(20) + 10; // 10 a 30 segundos
        
        int starterIdx = new Random().nextInt(players.size());
        String starterId = players.get(starterIdx).getId();
        
        broadcast(new GameMessage(GameMessage.TYPE_START, localPlayer.getId(), starterId));
        startHostTimer();
    }

    private void startHostTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (remainingTime > 0) {
                    remainingTime--;
                    broadcast(new GameMessage(GameMessage.TYPE_TICK, localPlayer.getId(), String.valueOf(remainingTime)));
                    hostTimerHandler.postDelayed(this, 1000);
                } else {
                    String loserId = getPlayerWithPotatoId();
                    broadcast(new GameMessage(GameMessage.TYPE_GAME_OVER, localPlayer.getId(), loserId));
                }
            }
        };
        hostTimerHandler.post(timerRunnable);
    }

    private String getPlayerWithPotatoId() {
        // En una implementación real, el Host debería trackear quién tiene la papa
        // o los clientes deberían reportar. Para simplificar, asumimos que el último PASS_POTATO define al perdedor.
        return "current_holder_id"; 
    }

    public void passPotatoToNext() {
        if (!localPlayer.isHasPotato()) return;
        
        int myIdx = -1;
        for(int i=0; i<players.size(); i++) {
            if(players.get(i).getId().equals(localPlayer.getId())) {
                myIdx = i;
                break;
            }
        }
        
        int nextIdx = (myIdx + 1) % players.size();
        String nextPlayerId = players.get(nextIdx).getId();
        
        broadcast(new GameMessage(GameMessage.TYPE_PASS_POTATO, localPlayer.getId(), nextPlayerId));
    }

    private void broadcast(GameMessage msg) {
        if (isHost && server != null) {
            server.broadcast(msg);
            handleMessage(msg); // Host procesa su propio mensaje
        } else if (client != null) {
            client.sendMessage(msg);
        }
    }

    public void stopTimer() {
        if (timerRunnable != null) hostTimerHandler.removeCallbacks(timerRunnable);
    }

    public Player getLocalPlayer() { return localPlayer; }
    public List<Player> getPlayers() { return players; }
    public boolean isHost() { return isHost; }
}
