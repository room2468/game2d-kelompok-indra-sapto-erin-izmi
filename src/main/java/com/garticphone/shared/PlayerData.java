package com.garticphone.shared;

public class PlayerData {
    public String name;
    public int id;
    public boolean isHost;

    public PlayerData(String name, int id, boolean isHost) {
        this.name = name;
        this.id = id;
        this.isHost = isHost;
    }
}
