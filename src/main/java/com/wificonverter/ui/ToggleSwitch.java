package com.wificonverter.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

/**
 * Animated toggle switch component.
 */
public class ToggleSwitch extends JComponent {

    private boolean selected = false;
    private float   animProgress = 0f;
    private Timer   animTimer;

    private static final Color COLOR_OFF  = new Color(0x2E3256);
    private static final Color COLOR_ON   = new Color(0x00E87A);
    private static final Color COLOR_KNOB = Color.WHITE;
    private static final int W = 54, H = 28;

    private Runnable onChange;

    public ToggleSwitch() {
        setPreferredSize(new Dimension(W, H));
        setMinimumSize(new Dimension(W, H));
        setMaximumSize(new Dimension(W, H));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setOpaque(false);

        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { toggle(); }
        });
    }

    public void toggle() {
        selected = !selected;
        animateTo(selected ? 1f : 0f);
        if (onChange != null) SwingUtilities.invokeLater(onChange);
    }

    public void setSelected(boolean value) {
        selected = value;
        animProgress = value ? 1f : 0f;
        repaint();
    }

    public boolean isSelected() { return selected; }
    public void setOnChange(Runnable r) { this.onChange = r; }

    private void animateTo(float target) {
        if (animTimer != null && animTimer.isRunning()) animTimer.stop();
        animTimer = new Timer(14, e -> {
            float diff = target - animProgress;
            if (Math.abs(diff) < 0.02f) {
                animProgress = target;
                ((Timer) e.getSource()).stop();
            } else {
                animProgress += diff * 0.22f;
            }
            repaint();
        });
        animTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Track
        Color track = lerp(COLOR_OFF, COLOR_ON, animProgress);
        g2.setColor(track);
        g2.fill(new RoundRectangle2D.Float(0, 0, W, H, H, H));

        // Glow when ON
        if (animProgress > 0.5f) {
            float alpha = (animProgress - 0.5f) * 2f;
            g2.setColor(new Color(0, 232, 122, (int)(60 * alpha)));
            g2.setStroke(new BasicStroke(3f));
            g2.draw(new RoundRectangle2D.Float(1, 1, W - 2, H - 2, H, H));
        }

        // Knob
        int knobD = H - 6;
        float knobX = 3 + (W - knobD - 6) * animProgress;
        g2.setColor(COLOR_KNOB);
        g2.fill(new Ellipse2D.Float(knobX, 3, knobD, knobD));

        g2.dispose();
    }

    private static Color lerp(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color(
            (int)(a.getRed()   + (b.getRed()   - a.getRed())   * t),
            (int)(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
            (int)(a.getBlue()  + (b.getBlue()  - a.getBlue())  * t)
        );
    }
}
