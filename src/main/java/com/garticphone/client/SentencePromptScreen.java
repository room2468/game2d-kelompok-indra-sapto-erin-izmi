package com.garticphone.client;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.garticphone.shared.GameMessage;
import com.google.gson.Gson;

public class SentencePromptScreen extends JFrame {
    private BufferedImage canvas;
    private Graphics2D g2;
    private int prevX, prevY;
    private String currentTool = "FreeDraw";
    private Color currentColor = Color.BLACK;
    private int eraserSize = 30;
    private Point eraserCursor = null;
    private Point shapeStart = null;
    private Point shapeEnd = null;
    private Stack<BufferedImage> history = new Stack<>();
    private static final java.util.List<SentencePromptScreen> instances = new java.util.ArrayList<>();
    private JLabel submittedLabel;
    private int submittedCount = 0;
    private int totalPlayers = 0;
    private int maxStep = 0;
    private int currentStep = 0;
    private boolean submitted = false;
    private final PrintWriter writer;

    private JLabel timerLabel;
    private Timer timer;
    private int secondsLeft = 180;

    private JButton submitButton;
    private JPanel toolPanel;
    private JComboBox<String> toolSelector;
    private JButton colorButton, undoButton, clearButton;

    public SentencePromptScreen(PrintWriter writer, String sentencePrompt, String playerName, int totalPlayers,
            int maxStep,
            int currentStep) {
        this.totalPlayers = totalPlayers;
        this.writer = writer;
        this.maxStep = maxStep;
        this.currentStep = currentStep + 1;
        setTitle(playerName + " Draw This Sentence" + "(" + currentStep + "/" + maxStep + ")");
        instances.add(this);
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // ---- Atas: Judul prompt + Timer ----
        JPanel topPanel = new JPanel(new BorderLayout());
        JLabel promptLabel = new JLabel("Prompt: " + sentencePrompt, SwingConstants.CENTER);
        promptLabel.setFont(new Font("Arial", Font.BOLD, 18));
        topPanel.add(promptLabel, BorderLayout.CENTER);

        timerLabel = new JLabel("Time left: " + secondsLeft + "s", SwingConstants.RIGHT);
        timerLabel.setFont(new Font("Arial", Font.BOLD, 22));
        timerLabel.setForeground(new Color(200, 40, 40));
        timerLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 20));
        topPanel.add(timerLabel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // ---- Tool Panel ----
        toolPanel = new JPanel();
        String[] tools = { "FreeDraw", "Line", "Rectangle", "Oval", "Eraser", "Fill" };
        toolSelector = new JComboBox<>(tools);
        JButton sizeLabel = new JButton("Size:");
        JTextField sizeField = new JTextField("4", 3);
        colorButton = new JButton("Color");
        undoButton = new JButton("Undo");
        clearButton = new JButton("Clear");

        toolPanel.add(toolSelector);
        toolPanel.add(sizeLabel);
        toolPanel.add(sizeField);
        toolPanel.add(colorButton);
        toolPanel.add(undoButton);
        toolPanel.add(clearButton);

        // ---- Drawing Panel ----
        JPanel drawPanel = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (canvas == null) {
                    canvas = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
                    g2 = canvas.createGraphics();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(Color.WHITE);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.setColor(currentColor);
                    g2.setStroke(new BasicStroke(4));
                }
                g.drawImage(canvas, 0, 0, null);

                if (currentTool.equals("Eraser") && eraserCursor != null) {
                    g.setColor(Color.GRAY);
                    ((Graphics2D) g).setStroke(new BasicStroke(1));
                    g.drawOval(eraserCursor.x - eraserSize / 2, eraserCursor.y - eraserSize / 2, eraserSize,
                            eraserSize);
                } else if ((currentTool.equals("Line") || currentTool.equals("Rectangle") || currentTool.equals("Oval"))
                        && shapeStart != null && shapeEnd != null) {
                    g.setColor(currentColor);
                    ((Graphics2D) g).setStroke(new BasicStroke(2));
                    int x = Math.min(shapeStart.x, shapeEnd.x);
                    int y = Math.min(shapeStart.y, shapeEnd.y);
                    int w = Math.abs(shapeStart.x - shapeEnd.x);
                    int h = Math.abs(shapeStart.y - shapeEnd.y);
                    switch (currentTool) {
                        case "Line":
                            g.drawLine(shapeStart.x, shapeStart.y, shapeEnd.x, shapeEnd.y);
                            break;
                        case "Rectangle":
                            g.drawRect(x, y, w, h);
                            break;
                        case "Oval":
                            g.drawOval(x, y, w, h);
                            break;
                    }
                }
            }
        };
        drawPanel.setPreferredSize(new Dimension(780, 450));
        drawPanel.setBackground(Color.WHITE);

        drawPanel.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                prevX = e.getX();
                prevY = e.getY();
                if (!currentTool.equals("FreeDraw") && !currentTool.equals("Eraser") && !currentTool.equals("Fill")) {
                    shapeStart = e.getPoint();
                } else if (currentTool.equals("Fill")) {
                    saveSnapshot();
                    floodFill(prevX, prevY, canvas.getRGB(prevX, prevY), currentColor.getRGB());
                    drawPanel.repaint();
                }
                saveSnapshot();
            }

            public void mouseReleased(MouseEvent e) {
                if (g2 == null || shapeStart == null || shapeEnd == null)
                    return;

                int strokeSize = parseStrokeSize(sizeField.getText());
                g2.setStroke(new BasicStroke(strokeSize));
                g2.setColor(currentColor);

                int x = Math.min(shapeStart.x, shapeEnd.x);
                int y = Math.min(shapeStart.y, shapeEnd.y);
                int w = Math.abs(shapeStart.x - shapeEnd.x);
                int h = Math.abs(shapeStart.y - shapeEnd.y);

                switch (currentTool) {
                    case "Line":
                        g2.drawLine(shapeStart.x, shapeStart.y, shapeEnd.x, shapeEnd.y);
                        break;
                    case "Rectangle":
                        g2.drawRect(x, y, w, h);
                        break;
                    case "Oval":
                        g2.drawOval(x, y, w, h);
                        break;
                }

                shapeStart = null;
                shapeEnd = null;
                eraserCursor = null;
                drawPanel.repaint();
            }
        });

        drawPanel.addMouseMotionListener(new MouseAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (g2 == null)
                    return;
                int strokeSize = parseStrokeSize(sizeField.getText());
                if (currentTool.equals("FreeDraw")) {
                    g2.setStroke(new BasicStroke(strokeSize));
                    g2.setColor(currentColor);
                    g2.drawLine(prevX, prevY, e.getX(), e.getY());
                } else if (currentTool.equals("Eraser")) {
                    g2.setStroke(new BasicStroke(eraserSize));
                    g2.setColor(Color.WHITE);
                    g2.drawLine(prevX, prevY, e.getX(), e.getY());
                    eraserCursor = e.getPoint();
                } else {
                    shapeEnd = e.getPoint();
                }
                prevX = e.getX();
                prevY = e.getY();
                drawPanel.repaint();
            }

            public void mouseMoved(MouseEvent e) {
                if (currentTool.equals("Eraser")) {
                    eraserCursor = e.getPoint();
                    drawPanel.repaint();
                }
            }
        });

        toolSelector.addActionListener(e -> currentTool = (String) toolSelector.getSelectedItem());

        colorButton.addActionListener(e -> {
            Color chosen = JColorChooser.showDialog(this, "Choose Color", currentColor);
            if (chosen != null) {
                currentColor = chosen;
                if (g2 != null)
                    g2.setColor(currentColor);
            }
        });

        undoButton.addActionListener(e -> {
            if (!history.isEmpty()) {
                canvas = history.pop();
                g2 = canvas.createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                drawPanel.repaint();
            }
        });

        clearButton.addActionListener(e -> {
            saveSnapshot();
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
            g2.setColor(currentColor);
            drawPanel.repaint();
        });

        // ---- South Panel: Tools + Submit + Progress ----
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(toolPanel, BorderLayout.CENTER);

        submitButton = new JButton("Submit");
        submitButton.addActionListener(e -> sendDrawingIfNeeded());
        southPanel.add(submitButton, BorderLayout.EAST);

        submittedLabel = new JLabel("Submitted: 0/" + totalPlayers, SwingConstants.CENTER);
        southPanel.add(submittedLabel, BorderLayout.SOUTH);

        add(drawPanel, BorderLayout.CENTER);
        add(southPanel, BorderLayout.SOUTH);

        setVisible(true);

        // Start timer
        startTimer();
    }

    private int parseStrokeSize(String text) {
        try {
            return Math.max(1, Integer.parseInt(text));
        } catch (NumberFormatException e) {
            return 4;
        }
    }

    private void saveSnapshot() {
        if (canvas != null) {
            BufferedImage snapshot = new BufferedImage(canvas.getWidth(), canvas.getHeight(), canvas.getType());
            Graphics g = snapshot.getGraphics();
            g.drawImage(canvas, 0, 0, null);
            g.dispose();
            history.push(snapshot);
        }
    }

    private void floodFill(int x, int y, int targetColor, int replacementColor) {
        if (targetColor == replacementColor)
            return;
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        boolean[][] visited = new boolean[width][height];
        Queue<Point> queue = new LinkedList<>();
        queue.add(new Point(x, y));

        while (!queue.isEmpty()) {
            Point p = queue.poll();
            int cx = p.x;
            int cy = p.y;
            if (cx < 0 || cy < 0 || cx >= width || cy >= height || visited[cx][cy])
                continue;
            if (canvas.getRGB(cx, cy) != targetColor)
                continue;

            canvas.setRGB(cx, cy, replacementColor);
            visited[cx][cy] = true;

            queue.add(new Point(cx + 1, cy));
            queue.add(new Point(cx - 1, cy));
            queue.add(new Point(cx, cy + 1));
            queue.add(new Point(cx, cy - 1));
        }
    }

    private void sendDrawingIfNeeded() {
        if (submitted)
            return;
        submitted = true;
        if (timer != null)
            timer.stop();
        setToolsEnabled(false);

        // Konversi gambar ke base64 string
        String base64Image = encodeImageToBase64(canvas);

        // Kirim ke server
        GameMessage msg = new GameMessage("drawing", base64Image, -1, -1);
        writer.println(new Gson().toJson(msg));
        writer.flush();

        submittedLabel.setText("Submitted. Waiting for others...");
    }

    private void setToolsEnabled(boolean enabled) {
        toolSelector.setEnabled(enabled);
        colorButton.setEnabled(enabled);
        undoButton.setEnabled(enabled);
        clearButton.setEnabled(enabled);
        submitButton.setEnabled(enabled);
    }

    private void startTimer() {
        timer = new Timer(1000, e -> {
            secondsLeft--;
            timerLabel.setText("Time left: " + secondsLeft + "s");
            if (secondsLeft <= 0) {
                timer.stop();
                sendDrawingIfNeeded();
            }
        });
        timer.setRepeats(true);
        timer.start();
    }

    public void updateSubmittedCount(int submitted, int total) {
        submittedCount = submitted;
        submittedLabel.setText("Submitted: " + submitted + "/" + total);
    }

    public static void broadcastSubmittedCount(int submitted, int total) {
        SwingUtilities.invokeLater(() -> {
            for (SentencePromptScreen screen : instances) {
                screen.updateSubmittedCount(submitted, total);
            }
        });
    }

    public static void resetOpenedFlag() {
        instances.clear();
    }

    // Utility method to encode BufferedImage to Base64 string
    private String encodeImageToBase64(BufferedImage image) {
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "png", baos);
            byte[] bytes = baos.toByteArray();
            return java.util.Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static void closeAll() {
        for (SentencePromptScreen screen : instances) {
            screen.dispose();
        }
        instances.clear();
    }

    public void updateStepLabel(int currentStep, int maxStep) {
        this.currentStep = currentStep;
        this.maxStep = maxStep;

    }

    public static void broadcastStepUpdate(int currentStep, int maxStep) {
        SwingUtilities.invokeLater(() -> {
            for (SentencePromptScreen screen : instances) {
                screen.updateStepLabel(currentStep, maxStep);
            }
        });
    }

}
