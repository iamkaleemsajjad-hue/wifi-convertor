package com.wificonverter.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/**
 * A dark rounded card displaying a network feature with an icon, title,
 * description, status label and animated toggle switch.
 */
public class FeatureCard extends JPanel {

    // Design tokens
    private static final Color BG_CARD    = new Color(0x0D1028);
    private static final Color BG_HOVER   = new Color(0x111535);
    private static final Color BORDER     = new Color(0x1C2245);
    private static final Color BORDER_ON  = new Color(0x2A4A8A); // blue glow when active
    private static final Color TEXT_TITLE = new Color(0xE0E4FF);
    private static final Color TEXT_DESC  = new Color(0x5A6090);
    private static final Color CLR_ON     = new Color(0x00E87A);
    private static final Color CLR_OFF    = new Color(0x4A5080);

    private boolean hovered = false;
    private final ToggleSwitch toggle;
    private final JLabel statusLabel;
    private final String enabledText;
    private final String disabledText;

    public FeatureCard(String emoji, String title, String description,
                       String enabledText, String disabledText,
                       Runnable onEnable, Runnable onDisable) {
        this.enabledText  = enabledText;
        this.disabledText = disabledText;

        setOpaque(false);
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(16, 16, 14, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0; gbc.weightx = 1;

        // Emoji icon
        JLabel iconLbl = new JLabel(emoji);
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 26));
        gbc.gridy = 0; gbc.insets = new Insets(0, 0, 6, 0);
        add(iconLbl, gbc);

        // Title
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titleLbl.setForeground(TEXT_TITLE);
        gbc.gridy = 1; gbc.insets = new Insets(0, 0, 4, 0);
        add(titleLbl, gbc);

        // Description
        JLabel descLbl = new JLabel("<html><div style='width:130px'>" + description + "</div></html>");
        descLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        descLbl.setForeground(TEXT_DESC);
        gbc.gridy = 2; gbc.insets = new Insets(0, 0, 12, 0);
        add(descLbl, gbc);

        // Spacer
        gbc.gridy = 3; gbc.weighty = 1;
        add(Box.createVerticalGlue(), gbc);
        gbc.weighty = 0;

        // Bottom row: status + toggle
        JPanel bottom = new JPanel(new BorderLayout(0, 0));
        bottom.setOpaque(false);

        statusLabel = new JLabel(disabledText);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        statusLabel.setForeground(CLR_OFF);
        bottom.add(statusLabel, BorderLayout.WEST);

        toggle = new ToggleSwitch();
        toggle.setOnChange(() -> {
            boolean on = toggle.isSelected();
            statusLabel.setText(on ? enabledText : disabledText);
            statusLabel.setForeground(on ? CLR_ON : CLR_OFF);
            repaint();
            if (on) { if (onEnable  != null) new Thread(onEnable).start();  }
            else    { if (onDisable != null) new Thread(onDisable).start(); }
        });
        bottom.add(toggle, BorderLayout.EAST);

        gbc.gridy = 4; gbc.insets = new Insets(0, 0, 0, 0);
        add(bottom, gbc);

        // Hover glint
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { hovered = true;  repaint(); }
            @Override public void mouseExited (java.awt.event.MouseEvent e) { hovered = false; repaint(); }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        RoundRectangle2D rect = new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);

        // Background
        g2.setColor(hovered ? BG_HOVER : BG_CARD);
        g2.fill(rect);

        // Border
        g2.setStroke(new BasicStroke(1.2f));
        g2.setColor(toggle.isSelected() ? BORDER_ON : BORDER);
        g2.draw(rect);

        // Top-left accent glow when active
        if (toggle.isSelected()) {
            GradientPaint gp = new GradientPaint(0, 0, new Color(0x2244CC, true),
                    getWidth() / 2f, getHeight() / 2f, new Color(0x2244CC, false));
            g2.setPaint(gp);
            g2.fill(rect);
        }

        g2.dispose();
        super.paintComponent(g);
    }

    /** Force the toggle to a given state without triggering callbacks. */
    public void setFeatureActive(boolean active) {
        toggle.setSelected(active);
        statusLabel.setText(active ? enabledText : disabledText);
        statusLabel.setForeground(active ? CLR_ON : CLR_OFF);
        repaint();
    }

    public boolean isFeatureActive() { return toggle.isSelected(); }
    public ToggleSwitch getToggle()  { return toggle; }
}
