package com.example.paparellena.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameManager {
    private List<Player> players;
    private Player currentPlayerWithPotato;
    private boolean isGameRunning;
    private int remainingTime;

    public GameManager() {
        this.players = new ArrayList<>();
        this.isGameRunning = false;
    }

    public void addPlayer(Player player) {
        players.add(player);
    }

    public void startGame(int minTime, int maxTime) {
        if (players.size() < 2) return;
        
        isGameRunning = true;
        remainingTime = new Random().nextInt(maxTime - minTime + 1) + minTime;
        
        // Asignar papa aleatoriamente al inicio
        int randomIndex = new Random().nextInt(players.size());
        currentPlayerWithPotato = players.get(randomIndex);
        currentPlayerWithPotato.setHasPotato(true);
    }

    public void passPotato(String targetPlayerId) {
        if (currentPlayerWithPotato == null) return;

        for (Player p : players) {
            if (p.getId().equals(targetPlayerId)) {
                currentPlayerWithPotato.setHasPotato(false);
                p.setHasPotato(true);
                currentPlayerWithPotato = p;
                break;
            }
        }
    }

    public boolean tick() {
        if (remainingTime > 0) {
            remainingTime--;
            return false;
        }
        isGameRunning = false;
        return true; // Game Over
    }

    public Player getCurrentPlayerWithPotato() { return currentPlayerWithPotato; }
    public boolean isGameRunning() { return isGameRunning; }
}
