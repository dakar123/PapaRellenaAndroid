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
    private int initialTime = 30; // Default time selected by user
    
    private Server server;
    private Client client;
    private Handler hostTimerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    // Logic for potato holding constraints
    private long potatoReceivedTime = 0;
    private static final long MIN_HOLD_TIME_MS = 2000;
    private static final long MAX_HOLD_TIME_MS = 5000;
    private Handler burningHandler = new Handler(Looper.getMainLooper());
    private Runnable burningRunnable;

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

    public void setInitialTime(int seconds) {
        this.initialTime = seconds;
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
        this.localPlayer = new Player(myIp, name);
        
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
                Player newPlayer = new Player(msg.getSenderId(), msg.getContent());
                if (!players.contains(newPlayer)) {
                    players.add(newPlayer);
                    if (listener != null) listener.onPlayerJoined(newPlayer);
                }
                if (isHost) {
                   for(Player p : players) {
                       broadcast(new GameMessage(GameMessage.TYPE_JOIN, p.getId(), p.getName()));
                   }
                }
                break;
            case GameMessage.TYPE_START:
                isGameRunning = true;
                if (listener != null) listener.onGameStarted(msg.getContent());
                if (msg.getContent().equals(localPlayer.getId())) {
                    onReceivePotato();
                }
                break;
            case GameMessage.TYPE_PASS_POTATO:
                if (msg.getContent().equals(localPlayer.getId())) {
                    onReceivePotato();
                } else {
                    onReleasePotato();
                }
                for (Player p : players) {
                    p.setHasPotato(p.getId().equals(msg.getContent()));
                }
                break;
            case GameMessage.TYPE_TICK:
                remainingTime = Integer.parseInt(msg.getContent());
                if (listener != null) listener.onTick(remainingTime);
                break;
            case GameMessage.TYPE_GAME_OVER:
                isGameRunning = false;
                stopTimer();
                stopBurningCheck();
                if (listener != null) listener.onGameOver(msg.getContent());
                break;
        }
    }

    private void onReceivePotato() {
        localPlayer.setHasPotato(true);
        potatoReceivedTime = System.currentTimeMillis();
        if (listener != null) listener.onPotatoReceived();
        startBurningCheck();
    }

    private void onReleasePotato() {
        localPlayer.setHasPotato(false);
        stopBurningCheck();
        if (listener != null) listener.onPotatoPassed();
    }

    private void startBurningCheck() {
        burningRunnable = new Runnable() {
            @Override
            public void run() {
                if (!localPlayer.isHasPotato()) return;
                
                long elapsed = System.currentTimeMillis() - potatoReceivedTime;
                if (listener != null) listener.onHoldTimeUpdate(elapsed);

                if (elapsed >= MAX_HOLD_TIME_MS) {
                    broadcast(new GameMessage(GameMessage.TYPE_GAME_OVER, localPlayer.getId(), localPlayer.getId()));
                } else {
                    burningHandler.postDelayed(this, 100);
                }
            }
        };
        burningHandler.post(burningRunnable);
    }

    private void stopBurningCheck() {
        if (burningRunnable != null) {
            burningHandler.removeCallbacks(burningRunnable);
        }
    }

    public void startGame() {
        if (!isHost || players.size() < 2) return;
        
        isGameRunning = true;
        
        // Randomized duration: between (initialTime - 10) and initialTime
        // If initialTime is less than 10, random between 1 and initialTime
        int maxReduction = Math.min(10, initialTime - 1);
        int reduction = (maxReduction > 0) ? new Random().nextInt(maxReduction + 1) : 0;
        remainingTime = initialTime - reduction;
        
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
                    // Still broadcast tick for internal logic if needed, 
                    // but UI will be hidden.
                    broadcast(new GameMessage(GameMessage.TYPE_TICK, localPlayer.getId(), String.valueOf(remainingTime)));
                    hostTimerHandler.postDelayed(this, 1000);
                } else if (isGameRunning) {
                    String loserId = getPlayerWithPotatoId();
                    broadcast(new GameMessage(GameMessage.TYPE_GAME_OVER, localPlayer.getId(), loserId));
                }
            }
        };
        hostTimerHandler.post(timerRunnable);
    }

    private String getPlayerWithPotatoId() {
        for (Player p : players) {
            if (p.isHasPotato()) return p.getId();
        }
        return localPlayer.getId(); 
    }

    public boolean canPassPotato() {
        if (!localPlayer.isHasPotato()) return false;
        long elapsed = System.currentTimeMillis() - potatoReceivedTime;
        return elapsed >= MIN_HOLD_TIME_MS;
    }

    public void passPotatoToNext() {
        if (!canPassPotato()) return;
        
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
    public void reset() {
        isGameRunning = false;
        stopTimer();
        stopBurningCheck();
        players.clear();
        if (server != null) server.stop();
        if (client != null) client.disconnect();
    }
}
