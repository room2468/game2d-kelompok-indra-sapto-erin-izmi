package com.garticphone.client;

import java.awt.GridLayout;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

public class LoginScreen extends JFrame {
    public LoginScreen() {
        setTitle("Gartic Phone LAN - Login");
        setSize(400, 200);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        String ipAddress = getLocalIPv4Address();
        JTextField nameField = new JTextField();
        JTextField ipField = new JTextField(ipAddress);
        JTextField portField = new JTextField("12345");
        JButton connectButton = new JButton("Connect");

        connectButton.addActionListener(e -> {
            String name = nameField.getText();
            String ip = ipField.getText();
            String portStr = portField.getText();
            try {
                int port = Integer.parseInt(portStr);
                dispose();
                new WaitingRoom(name, ip, port, false);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid port number.");
            }
        });

        setLayout(new GridLayout(4, 2));
        add(new JLabel("Your Name:"));
        add(nameField);
        add(new JLabel("Server IP:"));
        add(ipField);
        add(new JLabel("Port:"));
        add(portField);
        add(connectButton);

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
