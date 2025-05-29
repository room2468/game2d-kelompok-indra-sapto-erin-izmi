package com.garticphone.client;

import java.io.BufferedReader;
import java.io.PrintWriter;

import com.google.gson.Gson;

public class ClientMessageHandler {
    private final BufferedReader reader;
    private final PrintWriter writer;
    private final int playerId;
    private final String playerName;
    private final int totalPlayers;
    private final Gson gson = new Gson();
    private int maxStep = 0;
    private int currentStep = 0;

    public ClientMessageHandler(BufferedReader reader, PrintWriter writer, int playerId, String playerName,
            int totalPlayers) {
        this.reader = reader;
        this.writer = writer;
        this.playerId = playerId;
        this.playerName = playerName;
        this.totalPlayers = totalPlayers;
    }

}
