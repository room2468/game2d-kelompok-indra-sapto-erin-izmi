package com.garticphone.client;

import javax.swing.SwingUtilities;

public class GameClient {
    public static void launch() {
        SwingUtilities.invokeLater(LoginScreen::new);
    }
}
