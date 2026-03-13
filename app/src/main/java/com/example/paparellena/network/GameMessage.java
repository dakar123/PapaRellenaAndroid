package com.example.paparellena.network;

public class GameMessage {
    public static final String TYPE_JOIN = "JOIN";
    public static final String TYPE_START = "START";
    public static final String TYPE_PASS_POTATO = "PASS_POTATO";
    public static final String TYPE_GAME_OVER = "GAME_OVER";
    public static final String TYPE_TICK = "TICK";
    public static final String TYPE_PLAYER_LIST = "PLAYER_LIST";

    private String type;
    private String senderId;
    private String senderName;
    private String content;

    public GameMessage(String type, String senderId, String senderName, String content) {
        this.type = type;
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
    }

    public GameMessage(String type, String senderId, String content) {
        this(type, senderId, "", content);
    }

    public String getType() { return type; }
    public String getSenderId() { return senderId; }
    public String getSenderName() { return senderName; }
    public String getContent() { return content; }
}
