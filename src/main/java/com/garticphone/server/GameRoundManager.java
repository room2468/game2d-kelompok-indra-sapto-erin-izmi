package com.garticphone.server;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.garticphone.shared.GameMessage;
import com.garticphone.shared.ReplayPayload;
import com.google.gson.Gson;

public class GameRoundManager {
    private final int playerCount;
    private int currentStep = 0; // Step 0 = sentence, 1 = drawing, 2 = guess, 3 = drawing, dst
    private final int maxStep;
    private final List<PrintWriter> writers;
    private final Gson gson = new Gson();

    // Chain: tiap player punya urutan hasil (String text/gambar base64)
    private final List<List<String>> chains = new ArrayList<>();
    // Temp store submission per phase
    private final Map<Integer, String> submissions = new HashMap<>();

    public GameRoundManager(int playerCount, List<PrintWriter> writers) {
        this.playerCount = playerCount;
        this.writers = writers;
        this.maxStep = playerCount; // Contoh: 3 pemain → 5 step (sentence, draw, guess, draw, guess)
        for (int i = 0; i < playerCount; i++) {
            chains.add(new ArrayList<>());
        }
    }

    public void submit(int playerId, String data) {
        submissions.put(playerId, data);
        broadcastProgress();

        if (submissions.size() == playerCount) {
            rotateChains();
            submissions.clear();
            nextPhaseOrReplay();
        }
    }

    private void rotateChains() {
        for (int chainIdx = 0; chainIdx < playerCount; chainIdx++) {
            int submitterIdx = (chainIdx + currentStep) % playerCount;
            chains.get(chainIdx).add(submissions.get(submitterIdx));
        }
    }

    private void nextPhaseOrReplay() {
        if (currentStep < maxStep - 1) {
            currentStep++;
            sendPromptToNextPlayer();
        } else {
            sendReplayToAll();
        }
    }

    private void sendPromptToNextPlayer() {
        String type;
        if (currentStep == 0) {
            type = "sentence_next";
        } else if (currentStep % 2 == 0) {
            type = "drawing_next";
        } else {
            type = "sentence_next";
        }

        for (int chainIdx = 0; chainIdx < playerCount; chainIdx++) {
            int fillingPlayerIdx = (chainIdx + currentStep) % playerCount;
            String last = chains.get(chainIdx).get(chains.get(chainIdx).size() - 1);

            Map<String, Object> payload = new HashMap<>();
            payload.put("data", last);
            payload.put("currentStep", currentStep);
            payload.put("maxStep", maxStep);

            GameMessage msg = new GameMessage(type, payload, chainIdx, fillingPlayerIdx);
            writers.get(fillingPlayerIdx).println(gson.toJson(msg));
            writers.get(fillingPlayerIdx).flush();
        }
    }

    private void sendReplayToAll() {
        ReplayPayload payload = new ReplayPayload(chains, GameServer.getPlayers());
        GameMessage replayMsg = new GameMessage("replay_data", payload, -1, -1);
        for (PrintWriter out : writers) {
            out.println(gson.toJson(replayMsg));
            out.flush();
        }
        System.out.println("[SERVER] Game selesai, replay dikirim.");
    }

    private void broadcastProgress() {
        String progress = submissions.size() + "/" + playerCount;
        GameMessage progressMsg = new GameMessage("submitted_update", progress, -1, -1);
        for (PrintWriter out : writers) {
            out.println(gson.toJson(progressMsg));
            out.flush();
        }
    }

    public Map<String, Integer> getStepInfo() {
        Map<String, Integer> map = new HashMap<>();
        map.put("currentStep", getCurrentStep());
        map.put("maxStep", getMaxStep());
        return map;
    }

    public int getCurrentStep() {
        return currentStep;
    }

    public int getMaxStep() {
        return maxStep;
    }
}
