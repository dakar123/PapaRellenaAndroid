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
    private int initialGameTime = 30; // Tiempo por defecto
    
    private Server server;
    private Client client;
    private Handler hostTimerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    private long lastPotatoReceivedTime;
    private Handler turnTimerHandler = new Handler(Looper.getMainLooper());
    private Runnable turnTimerRunnable;
    private static final long MIN_HOLD_TIME = 2500; // 2.5s
    private static final long MAX_HOLD_TIME = 5000; // 5s

    public interface GameEventListener {
        void onPlayerJoined(Player player);
        void onGameStarted(String starterId);
        void onPotatoReceived();
        void onPotatoPassed();
        void onGameOver(String loserId);
        void onTick(int seconds);
        void onHoldTimeUpdate(long elapsedMs);
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

    public void reset() {
        stopTimer();
        stopTurnTimer();
        if (server != null) {
            server.stop();
            server = null;
        }
        if (client != null) {
            client.disconnect();
            client = null;
        }
        players.clear();
        isGameRunning = false;
        localPlayer = null;
    }

    public void setInitialTime(int seconds) {
        this.initialGameTime = seconds;
    }

    public void initAsHost(String name, String ip) {
        this.isHost = true;
        this.localPlayer = new Player(ip, name);
        players.clear();
        players.add(localPlayer);
        
        server = new Server(new Server.ServerCallback() {
            @Override
            public void onMessageReceived(GameMessage message) {
                handleMessage(message);
            }

            @Override
            public void onClientConnected(String ip) {
            }
        });
        server.start();
        
        initAsClient(name, ip, "localhost");
    }

    public void initAsClient(String name, String myIp, String serverIp) {
        this.isHost = (serverIp.equals("localhost") || serverIp.equals(myIp));
        if (this.localPlayer == null) {
            this.localPlayer = new Player(myIp, name);
        }
        
        client = new Client(serverIp, 8888, new Client.ClientCallback() {
            @Override
            public void onMessageReceived(GameMessage message) {
                if (!isHost) handleMessage(message);
            }

            @Override
            public void onConnected() {
                client.sendMessage(new GameMessage(GameMessage.TYPE_JOIN, localPlayer.getId(), localPlayer.getName(), ""));
            }

            @Override
            public void onDisconnected() {}
        });
        client.connect();
    }

    public void handleMessage(GameMessage msg) {
        switch (msg.getType()) {
            case GameMessage.TYPE_JOIN:
                Player newPlayer = new Player(msg.getSenderId(), msg.getSenderName());
                boolean exists = false;
                for (Player p : players) {
                    if (p.getId().equals(newPlayer.getId())) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    players.add(newPlayer);
                    if (listener != null) listener.onPlayerJoined(newPlayer);
                    // Si soy host, debo enviar la lista actualizada a todos (Opcional según tu Server.java)
                }
                break;
            case GameMessage.TYPE_START:
                isGameRunning = true;
                String starterId = msg.getContent();
                for (Player p : players) {
                    p.setHasPotato(p.getId().equals(starterId));
                }
                if (listener != null) listener.onGameStarted(starterId);
                if (starterId.equals(localPlayer.getId())) {
                    onReceivePotato();
                }
                break;
            case GameMessage.TYPE_PASS_POTATO:
                String receiverId = msg.getContent();
                for (Player p : players) {
                    p.setHasPotato(p.getId().equals(receiverId));
                }
                if (receiverId.equals(localPlayer.getId())) {
                    onReceivePotato();
                } else {
                    stopTurnTimer();
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
                stopTurnTimer();
                if (listener != null) listener.onGameOver(msg.getContent());
                break;
        }
    }

    private void onReceivePotato() {
        localPlayer.setHasPotato(true);
        lastPotatoReceivedTime = System.currentTimeMillis();
        startTurnTimer();
        if (listener != null) listener.onPotatoReceived();
    }

    private void startTurnTimer() {
        stopTurnTimer();
        turnTimerRunnable = new Runnable() {
            @Override
            public void run() {
                if (localPlayer != null && localPlayer.isHasPotato() && isGameRunning) {
                    long elapsed = System.currentTimeMillis() - lastPotatoReceivedTime;
                    if (listener != null) listener.onHoldTimeUpdate(elapsed);
                    
                    if (elapsed >= MAX_HOLD_TIME) {
                        broadcast(new GameMessage(GameMessage.TYPE_GAME_OVER, localPlayer.getId(), localPlayer.getId()));
                    } else {
                        turnTimerHandler.postDelayed(this, 50); // Update freq for UI
                    }
                }
            }
        };
        turnTimerHandler.post(turnTimerRunnable);
    }

    private void stopTurnTimer() {
        if (turnTimerRunnable != null) {
            turnTimerHandler.removeCallbacks(turnTimerRunnable);
        }
        if (listener != null) listener.onHoldTimeUpdate(0);
    }

    public void startGame() {
        if (!isHost || players.size() < 2) return;
        
        isGameRunning = true;
        remainingTime = initialGameTime;
        
        int starterIdx = new Random().nextInt(players.size());
        String starterId = players.get(starterIdx).getId();
        
        broadcast(new GameMessage(GameMessage.TYPE_START, localPlayer.getId(), starterId));
        startHostTimer();
    }

    private void startHostTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (remainingTime > 0 && isGameRunning) {
                    remainingTime--;
                    broadcast(new GameMessage(GameMessage.TYPE_TICK, localPlayer.getId(), String.valueOf(remainingTime)));
                    hostTimerHandler.postDelayed(this, 1000);
                } else if (isGameRunning) {
                    // Si el tiempo global acaba, el que tiene la papa pierde
                    String loserId = "";
                    for(Player p : players) if(p.isHasPotato()) loserId = p.getId();
                    broadcast(new GameMessage(GameMessage.TYPE_GAME_OVER, localPlayer.getId(), loserId));
                }
            }
        };
        hostTimerHandler.post(timerRunnable);
    }

    public void passPotatoToNext() {
        if (localPlayer == null || !localPlayer.isHasPotato() || !isGameRunning) return;
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastPotatoReceivedTime < MIN_HOLD_TIME) return;

        if (players.size() < 2) return;

        int myIdx = -1;
        for(int i=0; i<players.size(); i++) {
            if(players.get(i).getId().equals(localPlayer.getId())) {
                myIdx = i;
                break;
            }
        }
        
        Random random = new Random();
        int nextIdx;
        do {
            nextIdx = random.nextInt(players.size());
        } while (nextIdx == myIdx);
        
        String nextPlayerId = players.get(nextIdx).getId();
        broadcast(new GameMessage(GameMessage.TYPE_PASS_POTATO, localPlayer.getId(), nextPlayerId));
    }

    private void broadcast(GameMessage msg) {
        if (isHost && server != null) {
            server.broadcast(msg);
            handleMessage(msg);
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
    public boolean isGameRunning() { return isGameRunning; }
    
    public boolean canPassPotato() {
        if (localPlayer == null || !localPlayer.isHasPotato()) return false;
        long elapsed = System.currentTimeMillis() - lastPotatoReceivedTime;
        return elapsed >= MIN_HOLD_TIME;
    }

    public long getRemainingTurnTime() {
        if (localPlayer == null || !localPlayer.isHasPotato()) return 0;
        long elapsed = System.currentTimeMillis() - lastPotatoReceivedTime;
        return Math.max(0, MAX_HOLD_TIME - elapsed);
    }
}
