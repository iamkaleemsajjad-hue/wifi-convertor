package com.wificonverter.ui;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;

/**
 * Styled scrollable activity log panel.
 */
public class LogPanel extends JPanel {

    public enum Level { INFO, SUCCESS, WARNING, ERROR }

    private final JTextPane textPane;
    private final StyledDocument doc;

    private static final Color BG    = new Color(0x080B18);
    private static final Color GREEN = new Color(0x00E87A);
    private static final Color AMBER = new Color(0xFFB347);
    private static final Color RED   = new Color(0xFF4D6A);
    private static final Color CYAN  = new Color(0x00D4FF);
    private static final Color GRAY  = new Color(0x6070A0);
    private static final Font  MONO  = new Font("Consolas", Font.PLAIN, 12);

    public LogPanel() {
        setLayout(new BorderLayout());
        setOpaque(false);

        // Header
        JLabel header = new JLabel("  ◈ Activity Log");
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setForeground(GRAY);
        header.setBorder(BorderFactory.createEmptyBorder(6, 4, 4, 0));
        header.setOpaque(false);
        add(header, BorderLayout.NORTH);

        textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setBackground(BG);
        textPane.setForeground(new Color(0xC0CAE8));
        textPane.setFont(MONO);
        textPane.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 4));
        doc = textPane.getStyledDocument();

        JScrollPane scroll = new JScrollPane(textPane);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(0x1C2040), 1));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    public void log(String message) { log(message, Level.INFO); }

    public void log(String message, Level level) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Timestamp
                SimpleAttributeSet ts = new SimpleAttributeSet();
                StyleConstants.setForeground(ts, GRAY);
                StyleConstants.setFontFamily(ts, "Consolas");
                StyleConstants.setFontSize(ts, 12);
                String time = java.time.LocalTime.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
                doc.insertString(doc.getLength(), "[" + time + "] ", ts);

                // Prefix symbol + colored message
                SimpleAttributeSet style = new SimpleAttributeSet();
                StyleConstants.setFontFamily(style, "Consolas");
                StyleConstants.setFontSize(style, 12);
                String prefix;
                switch (level) {
                    case SUCCESS -> { StyleConstants.setForeground(style, GREEN);  prefix = "✔ "; }
                    case WARNING -> { StyleConstants.setForeground(style, AMBER);  prefix = "⚠ "; }
                    case ERROR   -> { StyleConstants.setForeground(style, RED);    prefix = "✘ "; }
                    default      -> { StyleConstants.setForeground(style, CYAN);   prefix = "› "; }
                }
                doc.insertString(doc.getLength(), prefix + message + "\n", style);

                // Auto-scroll
                textPane.setCaretPosition(doc.getLength());
            } catch (BadLocationException ignored) {}
        });
    }

    public void clear() {
        SwingUtilities.invokeLater(() -> {
            try { doc.remove(0, doc.getLength()); } catch (BadLocationException ignored) {}
        });
    }
}
