package com.rslauncher;

import javax.swing.SwingUtilities;

public class RSLauncher {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new RSLauncherUI().setVisible(true);
        });
    }
}