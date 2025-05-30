package com.garticphone.client;

import java.awt.BorderLayout;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.garticphone.shared.GameMessage;
import com.garticphone.shared.PlayerData;
import com.google.gson.Gson;

public class WaitingRoom extends JFrame {
    private final DefaultListModel<String> playerListModel = new DefaultListModel<>();
    private final JLabel countLabel = new JLabel("Players: 0/8");
    private BufferedReader reader;
    private PrintWriter writer;
    private final Gson gson = new Gson();

    private boolean isHost;
    private String playerName;
    private int totalPlayers = 0;
    private int playerId = -1;

    public WaitingRoom(String name, String host, int port, boolean isHost) {
        this.playerName = name;
        this.isHost = isHost;

        setTitle("Waiting Room - " + name + " • " + host + ":" + port);
        setLayout(new BorderLayout());
        setSize(300, 450);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JList<String> playerList = new JList<>(playerListModel);
        JButton startButton = new JButton("Start Game");
        startButton.setEnabled(isHost);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(countLabel, BorderLayout.WEST);
        topPanel.add(startButton, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(playerList), BorderLayout.CENTER);
        JButton copyIpButton = new JButton("Copy IP Address: " + host + ":" + port);
        copyIpButton.addActionListener(e -> {
            StringSelection selection = new StringSelection(host + ":" + port);
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
            JOptionPane.showMessageDialog(this, "IP Address copied to clipboard!");
        });
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(copyIpButton, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
        setVisible(true);

        startButton.addActionListener(e -> {
            showLoadingAndInput(0, 1);
            GameMessage startMsg = new GameMessage("start", null, -1, -1);
            writer.println(gson.toJson(startMsg));
        });

        new Thread(() -> {
            try {
                Socket socket = new Socket(host, port);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

                PlayerData player = new PlayerData(name, -1, isHost);
                writer.println(gson.toJson(player));

                String line = reader.readLine();
                java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<PlayerData>>() {
                }.getType();
                List<PlayerData> firstList = gson.fromJson(line, listType);

                for (PlayerData p : firstList) {
                    if (p.name.equals(playerName)) {
                        this.playerId = p.id;
                        break;
                    }
                }
                SwingUtilities.invokeLater(() -> updatePlayerList(firstList));

                while ((line = reader.readLine()) != null) {
                    if (line.trim().startsWith("[{")) {
                        List<PlayerData> playerListGame = gson.fromJson(line, listType);
                        SwingUtilities.invokeLater(() -> updatePlayerList(playerListGame));
                    } else {
                        GameMessage msg = gson.fromJson(line, GameMessage.class);
                        if ("start".equals(msg.type)) {
                            // Parse currentStep & maxStep dari payload
                            int currentStep = 0, maxStep = 1;
                            if (msg.payload != null) {
                                com.google.gson.JsonObject obj = gson.toJsonTree(msg.payload).getAsJsonObject();
                                if (obj.has("currentStep"))
                                    currentStep = obj.get("currentStep").getAsInt();
                                if (obj.has("maxStep"))
                                    maxStep = obj.get("maxStep").getAsInt();
                            }
                            int finalCurrentStep = currentStep;
                            int finalMaxStep = maxStep;
                            SwingUtilities.invokeLater(() -> showLoadingAndInput(finalCurrentStep, finalMaxStep));
                            break;
                        }
                    }
                }

            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Connection lost: " + e.getMessage());
                    dispose();
                });
            }
        }).start();
    }

    private void updatePlayerList(List<PlayerData> list) {
        playerListModel.clear();
        totalPlayers = list.size();
        for (int i = 0; i < list.size(); i++) {
            PlayerData p = list.get(i);
            boolean isFirst = (i == 0);
            String label = p.name + (isFirst ? " (Host)" : "");
            playerListModel.addElement(label);
        }
        countLabel.setText("Players: " + totalPlayers + "/8");
    }

    private void showLoadingAndInput(int currentStep, int maxStep) {
        dispose();
        JFrame loading = new JFrame("Loading");
        JLabel countdownLabel = new JLabel("Game will start in 5 seconds...", SwingConstants.CENTER);
        loading.setSize(300, 100);
        loading.setLocationRelativeTo(null);
        loading.setDefaultCloseOperation(EXIT_ON_CLOSE);
        loading.add(countdownLabel);
        loading.setVisible(true);

        Timer countdownTimer = new Timer(1000, null);
        countdownTimer.addActionListener(new java.awt.event.ActionListener() {
            int secondsLeft = 5;

            public void actionPerformed(java.awt.event.ActionEvent e) {
                secondsLeft--;
                if (secondsLeft > 0) {
                    countdownLabel.setText("Game will start in " + secondsLeft + " seconds...");
                } else {
                    countdownTimer.stop();
                    loading.dispose();
                    new TextInputScreen(writer, playerId, totalPlayers, playerName, maxStep, currentStep);

                }
            }
        });
        countdownTimer.start();
    }

}
