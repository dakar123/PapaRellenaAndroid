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

<<<<<<< HEAD
    // Nuevas variables para la lógica de la papa
    private long lastPotatoReceivedTime;
    private Handler turnTimerHandler = new Handler(Looper.getMainLooper());
    private Runnable turnTimerRunnable;
    private static final long MIN_HOLD_TIME = 2500; // 2.5s
    private static final long MAX_HOLD_TIME = 5000; // 5s
=======
    // Logic for potato holding constraints
    private long potatoReceivedTime = 0;
    private static final long MIN_HOLD_TIME_MS = 2000;
    private static final long MAX_HOLD_TIME_MS = 5000;
    private Handler burningHandler = new Handler(Looper.getMainLooper());
    private Runnable burningRunnable;
>>>>>>> baef31133ddc6b574186c50655db8bbb199c8abe

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
<<<<<<< HEAD
                    localPlayer.setHasPotato(false);
                    stopTurnTimer();
                    if (listener != null) listener.onPotatoPassed();
=======
                    onReleasePotato();
                }
                for (Player p : players) {
                    p.setHasPotato(p.getId().equals(msg.getContent()));
>>>>>>> baef31133ddc6b574186c50655db8bbb199c8abe
                }
                break;
            case GameMessage.TYPE_TICK:
                remainingTime = Integer.parseInt(msg.getContent());
                if (listener != null) listener.onTick(remainingTime);
                break;
            case GameMessage.TYPE_GAME_OVER:
                isGameRunning = false;
                stopTimer();
<<<<<<< HEAD
                stopTurnTimer();
=======
                stopBurningCheck();
>>>>>>> baef31133ddc6b574186c50655db8bbb199c8abe
                if (listener != null) listener.onGameOver(msg.getContent());
                break;
        }
    }

    private void onReceivePotato() {
        localPlayer.setHasPotato(true);
<<<<<<< HEAD
        lastPotatoReceivedTime = System.currentTimeMillis();
        startTurnTimer();
        if (listener != null) listener.onPotatoReceived();
    }

    private void startTurnTimer() {
        stopTurnTimer();
        turnTimerRunnable = new Runnable() {
            @Override
            public void run() {
                if (localPlayer.isHasPotato() && isGameRunning) {
                    // Explota automáticamente si pasa de 5s
                    broadcast(new GameMessage(GameMessage.TYPE_GAME_OVER, localPlayer.getId(), localPlayer.getId()));
                }
            }
        };
        turnTimerHandler.postDelayed(turnTimerRunnable, MAX_HOLD_TIME);
    }

    private void stopTurnTimer() {
        if (turnTimerRunnable != null) {
            turnTimerHandler.removeCallbacks(turnTimerRunnable);
=======
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
>>>>>>> baef31133ddc6b574186c50655db8bbb199c8abe
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
<<<<<<< HEAD
                } else {
                    // El tiempo total se acabó, pero la lógica de 5s individuales sigue activa.
                    // Podríamos forzar un perdedor aquí si el tiempo total es el que manda,
                    // pero según el requerimiento, la explosión es por los 5s.
=======
                } else if (isGameRunning) {
                    String loserId = getPlayerWithPotatoId();
                    broadcast(new GameMessage(GameMessage.TYPE_GAME_OVER, localPlayer.getId(), loserId));
>>>>>>> baef31133ddc6b574186c50655db8bbb199c8abe
                }
            }
        };
        hostTimerHandler.post(timerRunnable);
    }

<<<<<<< HEAD
    public void passPotatoToNext() {
        if (!localPlayer.isHasPotato() || !isGameRunning) return;
=======
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
>>>>>>> baef31133ddc6b574186c50655db8bbb199c8abe
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastPotatoReceivedTime < MIN_HOLD_TIME) {
            // No se puede pasar antes de 2.5s
            return;
        }

        if (players.size() < 2) return;

        int myIdx = -1;
        for(int i=0; i<players.size(); i++) {
            if(players.get(i).getId().equals(localPlayer.getId())) {
                myIdx = i;
                break;
            }
        }
        
        // Seleccionar usuario de forma aleatoria (que no sea el actual)
        Random random = new Random();
        int nextIdx;
        do {
            nextIdx = random.nextInt(players.size());
        } while (nextIdx == myIdx && players.size() > 1);
        
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
<<<<<<< HEAD
    public boolean isGameRunning() { return isGameRunning; }
    
    public long getRemainingTurnTime() {
        if (!localPlayer.isHasPotato()) return 0;
        long elapsed = System.currentTimeMillis() - lastPotatoReceivedTime;
        return Math.max(0, MAX_HOLD_TIME - elapsed);
    }
    
    public boolean canPassPotato() {
        if (!localPlayer.isHasPotato()) return false;
        long elapsed = System.currentTimeMillis() - lastPotatoReceivedTime;
        return elapsed >= MIN_HOLD_TIME;
=======
    public void reset() {
        isGameRunning = false;
        stopTimer();
        stopBurningCheck();
        players.clear();
        if (server != null) server.stop();
        if (client != null) client.disconnect();
>>>>>>> baef31133ddc6b574186c50655db8bbb199c8abe
    }
}
