package com.garticphone.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.garticphone.shared.GameMessage;
import com.google.gson.Gson;

public class GuessFromDrawingScreen extends JFrame {
    private static final List<GuessFromDrawingScreen> instances = new ArrayList<>();
    private final PrintWriter writer;
    private final int playerId;
    private final int totalPlayers;
    private final String playerName;
    private final BufferedImage image;
    private final Gson gson = new Gson();
    private int maxStep = 0;
    private int currentStep = 0;
    private boolean submitted = false;
    private JLabel submittedLabel;
    private JTextArea inputArea;
    private JButton submitButton;
    private JLabel timerLabel;
    private Timer timer;
    private int secondsLeft = 60;

    public GuessFromDrawingScreen(PrintWriter writer, int playerId, int totalPlayers, String playerName,
            String base64Image, int maxStep, int currentStep) {
        this.maxStep = maxStep;
        this.currentStep = currentStep + 1;
        this.writer = writer;
        this.playerId = playerId;
        this.totalPlayers = totalPlayers;
        this.playerName = playerName;
        this.image = decodeBase64ToImage(base64Image);

        instances.add(this);
        initUI();
        startTimer();
    }

    private void initUI() {
        setTitle(playerName + " - Guess the Drawing" + "(" + currentStep + "/" + maxStep + ")");
        setSize(550, 450);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        setLayout(new BorderLayout(8, 8));

        // --- Atas: Judul dan Timer ---
        JPanel northPanel = new JPanel(new BorderLayout());
        JLabel instr = new JLabel("Describe the drawing below:", SwingConstants.CENTER);
        instr.setFont(new Font("Arial", Font.BOLD, 15));
        northPanel.add(instr, BorderLayout.CENTER);

        timerLabel = new JLabel("Time left: 10s", SwingConstants.RIGHT);
        timerLabel.setFont(new Font("Arial", Font.BOLD, 15));
        timerLabel.setForeground(new Color(180, 50, 50));
        timerLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        northPanel.add(timerLabel, BorderLayout.EAST);

        add(northPanel, BorderLayout.NORTH);

        // --- Tengah: gambar ---
        JPanel imagePanel = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (image != null) {
                    int imgW = image.getWidth(), imgH = image.getHeight();
                    int panelW = getWidth(), panelH = getHeight();
                    double scale = Math.min(panelW / (double) imgW, panelH / (double) imgH);
                    int w = (int) (imgW * scale), h = (int) (imgH * scale);
                    g.drawImage(image, (panelW - w) / 2, (panelH - h) / 2, w, h, null);
                }
            }
        };
        imagePanel.setPreferredSize(new Dimension(420, 210));
        imagePanel.setBackground(Color.WHITE);
        add(imagePanel, BorderLayout.CENTER);

        // --- Bawah: Input dan Submit ---
        JPanel inputPanel = new JPanel(new BorderLayout(6, 0));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));

        inputArea = new JTextArea();
        inputArea.setFont(new Font("Arial", Font.PLAIN, 15));
        inputArea.setRows(2);
        inputArea.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setPreferredSize(new Dimension(240, 45));

        submitButton = new JButton("Submit");
        submitButton.setFont(new Font("Arial", Font.BOLD, 15));
        submitButton.addActionListener(e -> sendGuessIfNeeded());

        inputPanel.add(inputArea, BorderLayout.CENTER);
        inputPanel.add(submitButton, BorderLayout.EAST);

        add(inputPanel, BorderLayout.SOUTH);

        // --- Kanan: Progress ---
        JPanel eastPanel = new JPanel(new BorderLayout());
        eastPanel.setPreferredSize(new Dimension(130, 60));
        submittedLabel = new JLabel("Submitted: 0/" + totalPlayers, SwingConstants.CENTER);
        submittedLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        eastPanel.add(submittedLabel, BorderLayout.SOUTH);
        eastPanel.setOpaque(false);

        add(eastPanel, BorderLayout.EAST);

        // --- Focus on input
        addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
                inputArea.requestFocusInWindow();
            }
        });

        setVisible(true);
    }

    private void sendGuessIfNeeded() {
        if (submitted)
            return;
        submitted = true;
        String text = inputArea.getText().trim();
        GameMessage msg = new GameMessage("sentence", text, playerId, -1);
        writer.println(gson.toJson(msg));
        writer.flush();

        if (timer != null)
            timer.stop();
        submitButton.setEnabled(false);
        inputArea.setEnabled(false);
        timerLabel.setText("Submitted!");
        submittedLabel.setText("Submitted. Waiting for others...");

    }

    private void startTimer() {
        timer = new Timer(1000, e -> {
            secondsLeft--;
            timerLabel.setText("Time left: " + secondsLeft + "s");
            if (secondsLeft <= 0) {
                timer.stop();
                sendGuessIfNeeded();
            }
        });
        timer.setRepeats(true);
        timer.start();
    }

    public static void closeAll() {
        for (GuessFromDrawingScreen s : instances)
            s.dispose();
        instances.clear();
    }

    public void updateSubmittedCount(int submitted, int total) {
        submittedLabel.setText("Submitted: " + submitted + "/" + total);
    }

    public static void broadcastSubmittedCount(int submitted, int total) {
        SwingUtilities.invokeLater(() -> {
            for (GuessFromDrawingScreen s : instances) {
                s.updateSubmittedCount(submitted, total);
            }
        });
    }

    private BufferedImage decodeBase64ToImage(String base64) {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            return javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bytes));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
