package com.garticphone.shared;

public class GameMessage {
    public String type;
    public Object payload;
    public int fromPlayer;
    public int toPlayer;

    public GameMessage(String type, Object payload, int fromPlayer, int toPlayer) {
        this.type = type;
        this.payload = payload;
        this.fromPlayer = fromPlayer;
        this.toPlayer = toPlayer;
    }
}
