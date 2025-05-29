package com.garticphone.client;

import java.awt.GridLayout;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import com.garticphone.server.*;

/**
 *
 * @author fajar
 */
public class Launcher extends JFrame {
    public Launcher() {
        setTitle("Gartic Phone LAN - Start");
        setSize(300, 150);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JButton hostButton = new JButton("Host Game");
        JButton joinButton = new JButton("Join Game");

        hostButton.addActionListener(e -> {
            String portStr = JOptionPane.showInputDialog(this, "Enter port to host:", "12345");
            try {
                int port = Integer.parseInt(portStr);
                String name = JOptionPane.showInputDialog(this, "Enter your name:", "Host");
                if (name == null || name.isBlank())
                    return;

                GameServer.start(port);

                String hostAddress = getLocalIPv4Address();
                if (hostAddress == null) {
                    JOptionPane.showMessageDialog(this, "Unable to detect local IP address.");
                    return;
                }

                dispose();
                new WaitingRoom(name, hostAddress, port, true);

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid port number.");
            }
        });

        joinButton.addActionListener(e -> {
            dispose();
            GameClient.launch();
        });

        setLayout(new GridLayout(2, 1));
        add(hostButton);
        add(joinButton);
        setVisible(true);
    }

    private String getLocalIPv4Address() {
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (InetAddress address : Collections.list(ni.getInetAddresses())) {
                    if (!address.isLoopbackAddress() && address instanceof java.net.Inet4Address) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }
}
