package com.garticphone.client;

import java.awt.BorderLayout;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.garticphone.shared.GameMessage;
import com.google.gson.Gson;

public class TextInputScreen extends JFrame {
    private static final long serialVersionUID = 1L;
    private static boolean alreadyOpened = false;
    private static final List<TextInputScreen> instances = new ArrayList<>();

    private final PrintWriter writer;
    private final int playerId;
    private final String playerName;
    private final int totalPlayers;

    private final Gson gson = new Gson();

    private boolean submitted = false;
    private int submittedCount = 0;
    private int maxStep = 0;
    private int currentStep = 0;
    private JLabel countdownLabel;
    private JLabel submittedLabel;
    private Timer timer;
    private JTextArea inputArea;

    public TextInputScreen(PrintWriter writer, int playerId, int totalPlayers, String playerName, int maxStep,
            int currentStep) {
        this.writer = writer;
        this.playerId = playerId;
        this.playerName = playerName;
        this.totalPlayers = totalPlayers;
        this.maxStep = maxStep;
        this.currentStep = currentStep + 1;
        instances.add(this);

        initUI();
        startCountdown();
    }

    private void initUI() {
        setTitle(playerName + " Turn - Write a Sentence" + "(" + currentStep + "/" + maxStep + ")");
        setSize(400, 250);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        countdownLabel = new JLabel("Time left: 60", SwingConstants.CENTER);
        submittedLabel = new JLabel("Submitted: 0/" + totalPlayers, SwingConstants.CENTER);

        inputArea = new JTextArea();
        JButton submitButton = new JButton("Submit");
        submitButton.addActionListener(e -> sendSentenceIfNeeded());

        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.add(countdownLabel, BorderLayout.NORTH);
        infoPanel.add(submittedLabel, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(infoPanel, BorderLayout.NORTH);
        add(new JScrollPane(inputArea), BorderLayout.CENTER);
        add(submitButton, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void startCountdown() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            int secondsLeft = 60;

            public void run() {
                SwingUtilities.invokeLater(() -> countdownLabel.setText("Time left: " + secondsLeft));
                if (--secondsLeft < 0) {
                    timer.cancel();
                    sendSentenceIfNeeded();
                }
            }
        }, 0, 1000);
    }

    private void sendSentenceIfNeeded() {
        if (submitted)
            return;
        submitted = true;
        if (timer != null)
            timer.cancel();

        String sentence = inputArea.getText().trim();
        GameMessage msg = new GameMessage("sentence", sentence, playerId, -1);
        writer.println(gson.toJson(msg));
        writer.flush();

        submittedLabel.setText("Submitted. Waiting for others...");

        SwingUtilities.invokeLater(() -> {
            dispose(); // Tutup window input
            new SentencePromptScreen(writer, sentence, playerName, totalPlayers, maxStep, currentStep);
        });
    }

    public static void resetOpenedFlag() {
        instances.clear();
    }

    public void updateSubmittedCount(int submitted, int total) {
        submittedCount = submitted;
        submittedLabel.setText("Submitted: " + submitted + "/" + total);
    }

    public static void broadcastSubmittedCount(int submitted, int total) {
        SwingUtilities.invokeLater(() -> {
            for (TextInputScreen screen : instances) {
                screen.updateSubmittedCount(submitted, total);
            }
        });
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getTotalPlayers() {
        return totalPlayers;
    }

    public PrintWriter getWriter() {
        return writer;
    }

    public static void closeAll() {
        for (TextInputScreen screen : instances) {
            screen.dispose();
        }
        instances.clear();
    }

}
