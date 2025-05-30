package com.garticphone.client;

import java.io.BufferedReader;
import java.io.PrintWriter;

import javax.swing.SwingUtilities;

import com.garticphone.shared.GameMessage;
import com.google.gson.Gson;

public class ClientMessageHandler implements Runnable {
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

    @Override
    public void run() {
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[CLIENT] Received: " + line);
                GameMessage msg = gson.fromJson(line, GameMessage.class);

                switch (msg.type) {
                    case "submitted_update": {
                        String[] parts = ((String) msg.payload).split("/");
                        int submitted = Integer.parseInt(parts[0]);
                        int total = Integer.parseInt(parts[1]);
                        TextInputScreen.broadcastSubmittedCount(submitted, total);

                        break;
                    }
                    case "sentence_next": {
                        com.google.gson.JsonObject payloadObj = gson.toJsonTree(msg.payload).getAsJsonObject();
                        String sentence = payloadObj.get("data").getAsString();
                        int currentStep = payloadObj.get("currentStep").getAsInt();
                        int maxStep = payloadObj.get("maxStep").getAsInt();
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                TextInputScreen.closeAll();

                            }
                        });
                        break;
                    }
                    case "drawing_next": {

                        break;
                    }
                    case "replay_data": {

                        break;
                    }
                    case "start": {
                        int currentStep = 0, maxStep = 1;
                        if (msg.payload != null) {
                            com.google.gson.JsonObject obj = gson.toJsonTree(msg.payload).getAsJsonObject();
                            if (obj.has("currentStep"))
                                currentStep = obj.get("currentStep").getAsInt();
                            if (obj.has("maxStep"))
                                maxStep = obj.get("maxStep").getAsInt();
                        }
                        final int finalCurrentStep = currentStep;
                        final int finalMaxStep = maxStep;
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                TextInputScreen.closeAll();
                                // SentencePromptScreen.closeAll();
                                new TextInputScreen(writer, playerId, totalPlayers, playerName, finalMaxStep,
                                        finalCurrentStep);
                            }
                        });
                        break;
                    }
                    case "step_update": {
                        int step = 0, max = 1;
                        if (msg.payload != null) {
                            com.google.gson.JsonObject obj = gson.toJsonTree(msg.payload).getAsJsonObject();
                            if (obj.has("currentStep"))
                                step = obj.get("currentStep").getAsInt();
                            if (obj.has("maxStep"))
                                max = obj.get("maxStep").getAsInt();
                        }

                        this.currentStep = step;
                        this.maxStep = max;

                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[CLIENT] Disconnected or error: " + e.getMessage());
        }
    }

}
