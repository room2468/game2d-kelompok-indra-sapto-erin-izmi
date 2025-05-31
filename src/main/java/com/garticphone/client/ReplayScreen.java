package com.garticphone.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Base64;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import com.garticphone.shared.PlayerData;

public class ReplayScreen extends JFrame {
    private static final java.util.List<ReplayScreen> instances = new java.util.ArrayList<>();

    public ReplayScreen(List<List<String>> chains, List<PlayerData> players) {
        setTitle("Game Replay");
        setSize(900, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        instances.add(this);

        JTabbedPane tabbedPane = new JTabbedPane();

        // Untuk tiap player (tab)
        for (int p = 0; p < chains.size(); p++) {
            final int playerIndex = p;
            List<String> chain = chains.get(p);

            JPanel rootPanel = new JPanel();
            rootPanel.setLayout(new BorderLayout());

            // ------ TOMBOL EXPORT ------
            JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton exportPng = new JButton("Export PNG");

            topPanel.add(exportPng);

            rootPanel.add(topPanel, BorderLayout.NORTH);

            JPanel chainPanel = new JPanel();
            chainPanel.setLayout(new BoxLayout(chainPanel, BoxLayout.Y_AXIS));
            chainPanel.setBackground(Color.WHITE);

            String lastType = "text"; // urutan: text, gambar, text, dst

            // Kumpulkan semua gambar chain, nanti buat GIF export (optional)
            java.util.List<BufferedImage> chainImages = new java.util.ArrayList<>();

            for (int idx = 0; idx < chain.size(); idx++) {
                String content = chain.get(idx);
                int who = (p + idx) % players.size();
                String playerName = players.get(who).name;

                JPanel bubble = new JPanel(new BorderLayout());
                bubble.setOpaque(false);

                if (lastType.equals("text")) {
                    // Text: kiri, label atas
                    JLabel nameLabel = new JLabel(playerName + " menulis", JLabel.LEFT);
                    nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
                    bubble.add(nameLabel, BorderLayout.NORTH);

                    JTextArea area = new JTextArea(content);
                    area.setEditable(false);
                    area.setWrapStyleWord(true);
                    area.setLineWrap(true);
                    area.setBackground(new Color(230, 240, 255));
                    area.setFont(new Font("Arial", Font.PLAIN, 16));
                    area.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                    area.setMaximumSize(new Dimension(400, 80));
                    bubble.add(area, BorderLayout.WEST);

                    lastType = "image";
                } else {
                    // Panel vertikal kanan: label + gambar
                    JPanel rightPanel = new JPanel();
                    rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
                    rightPanel.setOpaque(false);

                    JLabel nameLabel = new JLabel(playerName + " menggambar", JLabel.RIGHT);
                    nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
                    nameLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
                    rightPanel.add(nameLabel);

                    BufferedImage img = decodeBase64ToImage(content);
                    chainImages.add(img);
                    JLabel imgLabel = new JLabel(new ImageIcon(img.getScaledInstance(240, 160, Image.SCALE_SMOOTH)));
                    imgLabel.setBorder(BorderFactory.createEmptyBorder(5, 30, 5, 10));
                    imgLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
                    rightPanel.add(imgLabel);

                    bubble.add(rightPanel, BorderLayout.EAST);

                    lastType = "text";
                }
                bubble.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
                chainPanel.add(bubble);

            }
            exportPng.addActionListener(e -> exportChainPanelToImage(chainPanel, players.get(playerIndex).name, this));
            JScrollPane scrollPane = new JScrollPane(chainPanel);
            rootPanel.add(scrollPane, BorderLayout.CENTER);
            // Export Action (simple, bisa diimprove)

            tabbedPane.add(players.get(playerIndex).name, rootPanel);
            tabbedPane.add(players.get(p).name, rootPanel);
        }

        add(tabbedPane);
        setVisible(true);
    }

    // ---- Base64 image decoder ----
    private BufferedImage decodeBase64ToImage(String base64) {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            return ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (Exception e) {
            BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(0, 0, 100, 100);
            g.setColor(Color.RED);
            g.drawString("INVALID", 10, 50);
            g.dispose();
            return img;
        }
    }

    public void exportChainPanelToImage(JPanel panel, String name, JFrame parent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(name + "-replay.png"));
        int res = chooser.showSaveDialog(parent);
        if (res != JFileChooser.APPROVE_OPTION)
            return;
        File file = chooser.getSelectedFile();

        // Paksa panel layout & ukurannya pas
        panel.invalidate();
        panel.validate();
        panel.revalidate();
        panel.repaint();
        Dimension size = panel.getPreferredSize();
        panel.setSize(size);

        BufferedImage img = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, size.width, size.height);
        panel.paintAll(g2);
        g2.dispose();

        try {
            String ext = "png";
            if (file.getName().toLowerCase().endsWith(".jpg"))
                ext = "jpg";
            ImageIO.write(img, ext, file);
            JOptionPane.showMessageDialog(parent, "Berhasil export ke " + file.getAbsolutePath());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent, "Gagal export: " + ex.getMessage());
        }
    }

    public static void closeAll() {
        for (ReplayScreen s : instances)
            s.dispose();
        instances.clear();
    }
}
