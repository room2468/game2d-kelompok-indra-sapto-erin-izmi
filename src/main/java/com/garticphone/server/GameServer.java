package com.garticphone.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.garticphone.shared.GameMessage;
import com.garticphone.shared.PlayerData;
import com.google.gson.Gson;

public class GameServer {
    private static final int MAX_PLAYERS = 8;
    private static final List<PlayerData> players = new ArrayList<>();
    private static final List<PrintWriter> writers = new ArrayList<>();
    private static GameRoundManager roundManager = null;
    private static final Gson gson = new Gson();

    public static void start(int port) {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("[SERVER] Listening on port " + port);

                while (players.size() < MAX_PLAYERS) {
                    Socket socket = serverSocket.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                    String json = in.readLine();
                    PlayerData player = gson.fromJson(json, PlayerData.class);
                    player.id = players.size();
                    players.add(player);
                    writers.add(out);

                    new Thread(new ClientHandler(in, player.id)).start();
                    broadcastPlayerList();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void broadcastPlayerList() {
        String json = gson.toJson(players);
        for (PrintWriter out : writers) {
            out.println(json);
            out.flush();
        }
    }

    private static void broadcast(GameMessage msg) {
        String json = gson.toJson(msg);
        for (PrintWriter out : writers) {
            out.println(json);
            out.flush();
        }
    }

    public static List<PlayerData> getPlayers() {
        return players;
    }

    private static class ClientHandler implements Runnable {
        private final BufferedReader in;
        private final int playerId;

        public ClientHandler(BufferedReader in, int playerId) {
            this.in = in;
            this.playerId = playerId;
        }

        @Override
        public void run() {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    GameMessage msg = gson.fromJson(line, GameMessage.class);
                    switch (msg.type) {
                        case "start":
                            System.out.println("[SERVER] Host started the game.");
                            roundManager = new GameRoundManager(players.size(), writers);
                            int curStep = roundManager.getCurrentStep();
                            int maxStep = roundManager.getMaxStep();
                            Map<String, Integer> payload = new HashMap<>();
                            payload.put("currentStep", curStep);
                            payload.put("maxStep", maxStep);
                            broadcast(new GameMessage("start", payload, -1, -1));
                            break;
                        case "sentence":
                            if (roundManager != null) {
                                roundManager.submit(playerId, (String) msg.payload);

                            }
                            break;
                        case "drawing":
                            if (roundManager != null) {
                                roundManager.submit(playerId, (String) msg.payload);

                            }
                            break;
                        default:
                            System.out.println("[SERVER] Unknown message type: " + msg.type);
                            break;
                    }
                }
            } catch (IOException e) {
                System.out.println("[SERVER] Client " + playerId + " disconnected: " + e.getMessage());
            }
        }
    }
}
