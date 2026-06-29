package com.wificonverter;

import com.formdev.flatlaf.FlatDarkLaf;
import com.wificonverter.ui.MainWindow;
import com.wificonverter.util.AdminUtil;

import javax.swing.*;
import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;

public class Main {

    private static synchronized void log(String s) {
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of("app.log"), s + "\n", 
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception ignored) {}
    }

    public static void main(String[] args) {
        log("--------------------------------------");
        log("Main started. Admin: " + AdminUtil.isRunningAsAdmin());

        // ── Admin check ──────────────────────────────────────────────────────
        // The EXE manifest now handles elevation. This check is primarily a 
        // fallback for JAR execution or if the user somehow bypassed UAC.
        if (!AdminUtil.isRunningAsAdmin()) {
            log("Still not running as admin. Showing error and exiting.");
            JOptionPane.showMessageDialog(
                null,
                "Administrator privileges are required to run this application.\n" +
                "Please run the WiFiDataConverter.exe with administrator rights.",
                "Admin Required",
                JOptionPane.ERROR_MESSAGE
            );
            System.exit(0);
        }

        log("Running as ADMIN - initializing UI");
        // ── FlatLaf dark theme ───────────────────────────────────────────────
        try {
            FlatDarkLaf.setup();
            UIManager.put("Component.arc",              12);
            UIManager.put("Button.arc",                 10);
            UIManager.put("TextComponent.arc",          8);
            UIManager.put("ScrollBar.thumbArc",         999);
            UIManager.put("ScrollBar.width",            8);
            UIManager.put("TabbedPane.selectedBackground", new Color(0x111535));
            UIManager.put("Panel.background",           new Color(0x07091A));
            UIManager.put("OptionPane.background",      new Color(0x0D1028));
            UIManager.put("OptionPane.messageForeground", new Color(0xE0E4FF));
            log("FlatLaf setup complete.");
        } catch (Exception e) {
            log("FlatLaf setup error: " + e.getMessage());
        }

        // ── Launch ───────────────────────────────────────────────────────────
        SwingUtilities.invokeLater(() -> {
            try {
                log("Building MainWindow...");
                MainWindow window = new MainWindow();
                log("Setting visible...");
                window.setVisible(true);
                log("App window is now visible.");
            } catch (Throwable t) {
                StringWriter sw = new StringWriter();
                t.printStackTrace(new PrintWriter(sw));
                log("CRITICAL LAUNCH ERROR: " + sw.toString());
            }
        });
    }
}
