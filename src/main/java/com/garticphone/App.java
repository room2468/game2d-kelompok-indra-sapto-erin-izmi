package com.garticphone;

import javax.swing.SwingUtilities;

import com.garticphone.client.Launcher;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Launcher());
    }
}
