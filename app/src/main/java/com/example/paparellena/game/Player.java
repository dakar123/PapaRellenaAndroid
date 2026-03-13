package com.example.paparellena.game;

import java.util.Objects;

public class Player {
    private String id;
    private String name;
    private boolean hasPotato;

    public Player(String id, String name) {
        this.id = id;
        this.name = name;
        this.hasPotato = false;
    }

    public Player(String id, String name, boolean hasPotato) {
        this.id = id;
        this.name = name;
        this.hasPotato = hasPotato;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public boolean isHasPotato() { return hasPotato; }
    public void setHasPotato(boolean hasPotato) { this.hasPotato = hasPotato; }

    public Player copy(boolean hasPotato) {
        return new Player(this.id, this.name, hasPotato);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return Objects.equals(id, player.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
