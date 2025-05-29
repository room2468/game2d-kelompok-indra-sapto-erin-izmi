package com.garticphone.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import com.garticphone.shared.PlayerData;
import com.google.gson.Gson;

public class GameServer {
    private static final int MAX_PLAYERS = 8;
    private static final List<PlayerData> players = new ArrayList<>();
    private static final List<PrintWriter> writers = new ArrayList<>();
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

    private static class ClientHandler implements Runnable {
        private final BufferedReader in;
        private final int playerId;

        public ClientHandler(BufferedReader in, int playerId) {
            this.in = in;
            this.playerId = playerId;
        }

        @Override
        public void run() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}
