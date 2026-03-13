package com.example.paparellena.network;

public class GameMessage {
    public static final String TYPE_JOIN = "JOIN";
    public static final String TYPE_START = "START";
    public static final String TYPE_PASS_POTATO = "PASS_POTATO";
    public static final String TYPE_GAME_OVER = "GAME_OVER";
    public static final String TYPE_TICK = "TICK";

    private String type;
    private String senderId;
    private String content;

    public GameMessage(String type, String senderId, String content) {
        this.type = type;
        this.senderId = senderId;
        this.content = content;
    }

    public String getType() { return type; }
    public String getSenderId() { return senderId; }
    public String getContent() { return content; }
}
