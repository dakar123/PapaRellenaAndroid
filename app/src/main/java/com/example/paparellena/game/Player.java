package com.example.paparellena.game;

public class Player {
    private String id;
    private String name;
    private String ipAddress;
    private boolean hasPotato;

    public Player(String id, String name, String ipAddress) {
        this.id = id;
        this.name = name;
        this.ipAddress = ipAddress;
        this.hasPotato = false;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getIpAddress() { return ipAddress; }
    public boolean isHasPotato() { return hasPotato; }
    public void setHasPotato(boolean hasPotato) { this.hasPotato = hasPotato; }
}
