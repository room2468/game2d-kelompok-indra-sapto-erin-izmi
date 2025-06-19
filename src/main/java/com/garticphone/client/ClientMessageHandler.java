package com.garticphone.client;

import java.io.BufferedReader;
import java.io.PrintWriter;

import javax.swing.SwingUtilities;

import com.garticphone.shared.GameMessage;
import com.garticphone.shared.ReplayPayload;
import com.google.gson.Gson;

public class ClientMessageHandler implements Runnable {
    private final BufferedReader reader;
    private final PrintWriter writer;
    private final int playerId;
    private final String playerName;
    private final int totalPlayers;
    private int maxStep = 0;
    private int currentStep = 0;

    private final Gson gson = new Gson();

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
                        SentencePromptScreen.broadcastSubmittedCount(submitted, total);
                        GuessFromDrawingScreen.broadcastSubmittedCount(submitted, total);
                        break;
                    }
                    case "sentence_next": {
                        com.google.gson.JsonObject payloadObj = gson.toJsonTree(msg.payload).getAsJsonObject();
                        String sentence = payloadObj.get("data").getAsString();
                        int currentStep = payloadObj.get("currentStep").getAsInt();
                        int maxStep = payloadObj.get("maxStep").getAsInt();

                        this.currentStep = currentStep;
                        this.maxStep = maxStep;
                        SwingUtilities.invokeLater(() -> {
                            TextInputScreen.closeAll();
                            SentencePromptScreen.closeAll();
                            GuessFromDrawingScreen.closeAll();
                            new SentencePromptScreen(writer, sentence, playerName, totalPlayers, this.maxStep,
                                    this.currentStep);
                        });
                        break;
                    }
                    case "drawing_next": {
                        com.google.gson.JsonObject payloadObj = gson.toJsonTree(msg.payload).getAsJsonObject();
                        int currentStep = payloadObj.get("currentStep").getAsInt();
                        int maxStep = payloadObj.get("maxStep").getAsInt();
                        String base64Image = payloadObj.get("data").getAsString();

                        this.currentStep = currentStep;
                        this.maxStep = maxStep;
                        SwingUtilities.invokeLater(() -> {
                            TextInputScreen.closeAll();
                            SentencePromptScreen.closeAll();
                            GuessFromDrawingScreen.closeAll();
                            new GuessFromDrawingScreen(writer, playerId, totalPlayers, playerName, base64Image,
                                    this.maxStep, this.currentStep);
                        });
                        break;
                    }

                    case "replay_data": {
                        SwingUtilities.invokeLater(() -> {
                            ReplayScreen.closeAll();
                            TextInputScreen.closeAll();
                            SentencePromptScreen.closeAll();
                            GuessFromDrawingScreen.closeAll();
                            ReplayPayload payload = gson.fromJson(gson.toJson(msg.payload), ReplayPayload.class);
                            new ReplayScreen(payload.chains, payload.players);
                        });
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

                        this.currentStep = currentStep;
                        this.maxStep = maxStep;
                        int finalCurrentStep = this.currentStep;
                        int finalMaxStep = this.maxStep;
                        SwingUtilities.invokeLater(() -> {
                            TextInputScreen.closeAll();
                            SentencePromptScreen.closeAll();
                            GuessFromDrawingScreen.closeAll();
                            new TextInputScreen(writer, playerId, totalPlayers, playerName, finalMaxStep,
                                    finalCurrentStep);
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
                        SentencePromptScreen.broadcastStepUpdate(step, max);
                        break;
                    }

                }
            }
        } catch (Exception e) {
            System.out.println("[CLIENT] Disconnected or error: " + e.getMessage());
        }
    }
}
