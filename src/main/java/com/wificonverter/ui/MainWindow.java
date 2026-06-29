package com.wificonverter.ui;

import com.wificonverter.service.*;
import com.wificonverter.util.CommandRunner;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainWindow extends JFrame {

    // ── Palette ──────────────────────────────────────────────────────────────
    private static final Color BG_BASE    = new Color(0x07091A);
    private static final Color BG_LEFT    = new Color(0x0D1030);
    private static final Color ACCENT_MOB = new Color(0xFF6B35);  // orange = mobile
    private static final Color ACCENT_WIFI= new Color(0x3EA6FF);  // blue   = wifi
    private static final Color GREEN      = new Color(0x00E87A);
    private static final Color TEXT_WHITE = new Color(0xE8ECff);
    private static final Color TEXT_MUTED = new Color(0x5A6090);
    private static final Color BORDER_CLR = new Color(0x1A2050);

    // ── State ─────────────────────────────────────────────────────────────────
    private boolean mobileMode = false;
    private float   glowAnim   = 0f;
    private Timer   glowTimer;
    private float   particlePhase = 0f;
    private Timer   particleTimer;
    private String  wifiAdapter = "Wi-Fi";
    private String  localIp     = "Detecting…";
    private String  simulatedIp = "";

    // ── UI Components ─────────────────────────────────────────────────────────
    private JPanel  statusVisual;
    private JLabel  modeLabel;
    private JLabel  ipLabel;
    private JLabel  adapterLabel;
    private JButton masterBtn;
    private LogPanel logPanel;
    private FeatureCard cgnatCard, portCard, dnsCard, bwCard, ipv6Card, natCard;
    private JLabel statusBarLabel;

    private final ExecutorService exec = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    public MainWindow() {
        super("WiFi → Mobile Data Converter");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(980, 700);
        setMinimumSize(new Dimension(900, 640));
        setLocationRelativeTo(null);
        setResizable(true);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { onExit(); }
        });

        buildUI();
        startAnimations();
        detectNetwork();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  UI Construction
    // ═══════════════════════════════════════════════════════════════════════════

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0, 0, BG_BASE, 0, getHeight(), new Color(0x0C1030)));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        root.setOpaque(false);
        setContentPane(root);

        root.add(buildHeader(),   BorderLayout.NORTH);
        root.add(buildCenter(),   BorderLayout.CENTER);
        root.add(buildStatusBar(),BorderLayout.SOUTH);
    }

    // ── Header ────────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, new Color(0x0F1435), getWidth(), 0, new Color(0x0A0E28)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(BORDER_CLR);
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g2.dispose();
            }
        };
        header.setOpaque(false);
        header.setPreferredSize(new Dimension(0, 72));
        header.setBorder(new EmptyBorder(0, 24, 0, 24));

        // Left: icon + title
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);

        JLabel iconLbl = new JLabel("📱");
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));
        left.add(iconLbl);

        JPanel titles = new JPanel();
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.setOpaque(false);
        JLabel title = new JLabel("WiFi → Mobile Data Converter");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(TEXT_WHITE);
        JLabel sub = new JLabel("Simulate carrier-grade mobile data behavior on your WiFi");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sub.setForeground(TEXT_MUTED);
        titles.add(title);
        titles.add(sub);
        left.add(titles);
        header.add(left, BorderLayout.WEST);

        // Right: admin badge
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        JLabel badge = new JLabel(" ⚡ ADMIN ");
        badge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        badge.setForeground(GREEN);
        badge.setOpaque(true);
        badge.setBackground(new Color(0x00E87A, false).darker().darker().darker());
        badge.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x00E87A55), 1, true),
            BorderFactory.createEmptyBorder(3, 8, 3, 8)
        ));
        right.add(badge);
        header.add(right, BorderLayout.EAST);

        return header;
    }

    // ── Center ────────────────────────────────────────────────────────────────
    private JPanel buildCenter() {
        JPanel center = new JPanel(new BorderLayout(16, 0));
        center.setOpaque(false);
        center.setBorder(new EmptyBorder(16, 16, 8, 16));

        center.add(buildLeftPanel(),  BorderLayout.WEST);
        center.add(buildRightPanel(), BorderLayout.CENTER);
        return center;
    }

    // ── Left: Status + master control ─────────────────────────────────────────
    private JPanel buildLeftPanel() {
        JPanel panel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_LEFT);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                g2.setColor(BORDER_CLR);
                g2.setStroke(new BasicStroke(1));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 16, 16));
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setPreferredSize(new Dimension(270, 0));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Mode label
        modeLabel = new JLabel("WIFI MODE");
        modeLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        modeLabel.setForeground(ACCENT_WIFI);
        modeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(modeLabel);
        panel.add(Box.createVerticalStrut(12));

        // Animated visualization canvas
        statusVisual = buildVisualizationPanel();
        statusVisual.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(statusVisual);
        panel.add(Box.createVerticalStrut(16));

        // Separator
        panel.add(buildSeparator());
        panel.add(Box.createVerticalStrut(14));

        // IP info
        JLabel ipTitle = new JLabel("IP ADDRESS");
        ipTitle.setFont(new Font("Segoe UI", Font.BOLD, 9));
        ipTitle.setForeground(TEXT_MUTED);
        ipTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(ipTitle);
        panel.add(Box.createVerticalStrut(4));

        ipLabel = new JLabel("Detecting…");
        ipLabel.setFont(new Font("Consolas", Font.BOLD, 14));
        ipLabel.setForeground(ACCENT_WIFI);
        ipLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(ipLabel);
        panel.add(Box.createVerticalStrut(12));

        JLabel adpTitle = new JLabel("ADAPTER");
        adpTitle.setFont(new Font("Segoe UI", Font.BOLD, 9));
        adpTitle.setForeground(TEXT_MUTED);
        adpTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(adpTitle);
        panel.add(Box.createVerticalStrut(4));

        adapterLabel = new JLabel("Detecting…");
        adapterLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        adapterLabel.setForeground(TEXT_MUTED);
        adapterLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(adapterLabel);

        panel.add(Box.createVerticalGlue());
        panel.add(buildSeparator());
        panel.add(Box.createVerticalStrut(16));

        // Master activate button
        masterBtn = new JButton("⚡  ACTIVATE MOBILE MODE") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c1 = mobileMode ? new Color(0xFF6B35) : new Color(0x1A2455);
                Color c2 = mobileMode ? new Color(0xFF3E00) : new Color(0x2A3870);
                g2.setPaint(new GradientPaint(0, 0, c1, 0, getHeight(), c2));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                // Glow overlay
                if (mobileMode) {
                    g2.setColor(new Color(255, 107, 53, (int)(40 + 30 * Math.sin(glowAnim))));
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        masterBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        masterBtn.setForeground(TEXT_WHITE);
        masterBtn.setContentAreaFilled(false);
        masterBtn.setBorderPainted(false);
        masterBtn.setFocusPainted(false);
        masterBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        masterBtn.setPreferredSize(new Dimension(220, 44));
        masterBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        masterBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        masterBtn.addActionListener(e -> toggleMobileMode());
        panel.add(masterBtn);
        panel.add(Box.createVerticalStrut(10));

        // Reset button
        JButton resetBtn = new JButton("↺  Reset All Settings");
        resetBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        resetBtn.setForeground(TEXT_MUTED);
        resetBtn.setContentAreaFilled(false);
        resetBtn.setBorderPainted(false);
        resetBtn.setFocusPainted(false);
        resetBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        resetBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        resetBtn.addActionListener(e -> resetAll());
        panel.add(resetBtn);

        return panel;
    }

    // ── Animated network visualization ─────────────────────────────────────
    private JPanel buildVisualizationPanel() {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawVisualization((Graphics2D) g);
            }
        };
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(230, 160));
        p.setMaximumSize(new Dimension(230, 160));
        return p;
    }

    private void drawVisualization(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = statusVisual.getWidth(), h = statusVisual.getHeight();
        if (w == 0 || h == 0) return;

        int cx = w / 2, cy = h / 2;
        int leftX = 40, rightX = w - 40;

        // Connection line
        g2.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{6, 4}, 0));
        g2.setColor(new Color(0x2A3060));
        g2.drawLine(leftX + 28, cy, rightX - 28, cy);

        // Flowing particles along the line
        int lineLen = rightX - leftX - 56;
        Color partColor = mobileMode ? ACCENT_MOB : ACCENT_WIFI;
        for (int i = 0; i < 4; i++) {
            float offset = (particlePhase + i * 0.25f) % 1.0f;
            int px = leftX + 28 + (int)(offset * lineLen);
            int py = cy;
            float alpha = (float)(0.3 + 0.7 * Math.sin(offset * Math.PI));
            g2.setColor(new Color(partColor.getRed(), partColor.getGreen(), partColor.getBlue(), (int)(alpha * 255)));
            g2.setStroke(new BasicStroke(1));
            g2.fill(new Ellipse2D.Float(px - 4, py - 4, 8, 8));
        }

        // Left icon (WIFI)
        drawCircleIcon(g2, leftX, cy, mobileMode ? new Color(0x2A3060) : ACCENT_WIFI, "📶");

        // Right icon (Mobile)
        drawCircleIcon(g2, rightX, cy, mobileMode ? ACCENT_MOB : new Color(0x2A3060), "📱");

        // Center arrow
        g2.setColor(mobileMode ? ACCENT_MOB : ACCENT_WIFI);
        g2.setStroke(new BasicStroke(2));
        int ax = cx, ay = cy;
        int[] xp = {ax - 8, ax + 8, ax - 8};
        int[] yp = {ay - 7, ay, ay + 7};
        g2.fillPolygon(xp, yp, 3);

        // Labels
        g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
        g2.setColor(mobileMode ? new Color(0x2A3060) : ACCENT_WIFI);
        g2.drawString("WiFi", leftX - 14, cy + 34);
        g2.setColor(mobileMode ? ACCENT_MOB : new Color(0x2A3060));
        g2.drawString("Data", rightX - 14, cy + 34);
    }

    private void drawCircleIcon(Graphics2D g2, int cx, int cy, Color color, String emoji) {
        int r = 26;
        // Glow
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 40));
        g2.fill(new Ellipse2D.Float(cx - r - 6, cy - r - 6, (r + 6) * 2, (r + 6) * 2));
        // Circle
        g2.setColor(new Color(0x0D1028));
        g2.fill(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
        g2.setColor(color);
        g2.setStroke(new BasicStroke(2));
        g2.draw(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
        // Emoji
        g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(emoji, cx - fm.stringWidth(emoji) / 2, cy + fm.getAscent() / 2 - 2);
    }

    // ── Right panel: feature cards + log ─────────────────────────────────────
    private JPanel buildRightPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);

        // Cards grid
        JPanel grid = new JPanel(new GridLayout(2, 3, 12, 12));
        grid.setOpaque(false);

        cgnatCard = new FeatureCard("🌐", "CGNAT Simulation",
            "Block all new inbound TCP — matches Carrier-Grade NAT on mobile networks",
            "BLOCKING", "INACTIVE",
            () -> { exec.submit(() -> { boolean ok = FirewallService.enableCgnatBlock(); logCard("CGNAT Block", ok); }); },
            () -> { exec.submit(() -> { FirewallService.disableCgnatBlock(); log("CGNAT block removed", LogPanel.Level.SUCCESS); }); }
        );

        dnsCard = new FeatureCard("🔮", "Carrier DNS Override",
            "Force DNS to T-Mobile & AT&T carrier servers — simulates mobile DNS hijacking",
            "OVERRIDDEN", "DEFAULT",
            () -> { exec.submit(() -> { boolean ok = DnsService.setCarrierDns(wifiAdapter); logCard("DNS Override", ok); }); },
            () -> { exec.submit(() -> { DnsService.restoreDhcpDns(wifiAdapter); log("DNS restored to DHCP", LogPanel.Level.SUCCESS); }); }
        );

        portCard = new FeatureCard("🔌", "Port Blockade",
            "Block server ports (HTTP, SSH, RDP, Minecraft…) — no inbound services like mobile",
            "BLOCKED", "OPEN",
            () -> { exec.submit(() -> { boolean ok = FirewallService.enablePortBlockade(); logCard("Port Blockade", ok); }); },
            () -> { exec.submit(() -> { FirewallService.disablePortBlockade(); log("Port rules removed", LogPanel.Level.SUCCESS); }); }
        );

        bwCard = new FeatureCard("⚡", "Bandwidth Throttle",
            "Cap throughput to ~15 Mbps via Windows QoS — matching average 4G LTE speed",
            "15 Mbps CAP", "UNLIMITED",
            () -> { exec.submit(() -> { boolean ok = BandwidthService.applyThrottle(); logCard("Bandwidth Throttle", ok); }); },
            () -> { exec.submit(() -> { BandwidthService.removeThrottle(); log("Throttle removed", LogPanel.Level.SUCCESS); }); }
        );

        ipv6Card = new FeatureCard("🔷", "IPv6 Disabled",
            "Disable IPv6 on adapter — many mobile networks use IPv4-only or NAT64 bridging",
            "IPv4 ONLY", "v4+v6",
            () -> { exec.submit(() -> { boolean ok = Ipv6Service.disableIpv6(wifiAdapter); logCard("IPv6 Disable", ok); }); },
            () -> { exec.submit(() -> { Ipv6Service.enableIpv6(wifiAdapter); log("IPv6 re-enabled", LogPanel.Level.SUCCESS); }); }
        );

        natCard = new FeatureCard("🔒", "Strict NAT Mode",
            "Block inbound UDP on gaming / P2P ports — simulates Symmetric NAT on mobile",
            "STRICT", "OPEN",
            () -> { exec.submit(() -> {
                // Strict NAT = block inbound UDP for P2P gaming
                CommandRunner.Result r = CommandRunner.runNetsh(
                    "advfirewall","firewall","add","rule","name=WC_StrictNAT_UDP",
                    "dir=in","action=block","protocol=UDP","localport=3074,3478-3479,4500,6112,27015-27030","enable=yes"
                );
                logCard("Strict NAT", r.isSuccess());
            }); },
            () -> { exec.submit(() -> {
                CommandRunner.runNetsh("advfirewall","firewall","delete","rule","name=WC_StrictNAT_UDP");
                log("NAT rules removed", LogPanel.Level.SUCCESS);
            }); }
        );

        grid.add(cgnatCard);
        grid.add(dnsCard);
        grid.add(portCard);
        grid.add(bwCard);
        grid.add(ipv6Card);
        grid.add(natCard);
        panel.add(grid, BorderLayout.CENTER);

        // Log panel
        logPanel = new LogPanel();
        logPanel.setPreferredSize(new Dimension(0, 170));
        JPanel logWrap = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x08091E));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.dispose();
            }
        };
        logWrap.setOpaque(false);
        logWrap.add(logPanel);
        panel.add(logWrap, BorderLayout.SOUTH);

        return panel;
    }

    // ── Status bar ─────────────────────────────────────────────────────────
    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(new Color(0x09091F));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(BORDER_CLR);
                g.drawLine(0, 0, getWidth(), 0);
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(0, 28));
        bar.setBorder(new EmptyBorder(0, 16, 0, 16));

        statusBarLabel = new JLabel(" ● Idle  |  Adapter: detecting…");
        statusBarLabel.setFont(new Font("Consolas", Font.PLAIN, 11));
        statusBarLabel.setForeground(TEXT_MUTED);
        bar.add(statusBarLabel, BorderLayout.WEST);

        JLabel version = new JLabel("v1.0  |  WiFi Data Converter ");
        version.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        version.setForeground(new Color(0x3A4060));
        bar.add(version, BorderLayout.EAST);
        return bar;
    }

    private JSeparator buildSeparator() {
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER_CLR);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return sep;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Animations
    // ═══════════════════════════════════════════════════════════════════════════

    private void startAnimations() {
        particleTimer = new Timer(40, e -> {
            particlePhase += 0.04f;
            if (particlePhase >= 1f) particlePhase -= 1f;
            if (statusVisual != null) statusVisual.repaint();
        });
        particleTimer.start();

        glowTimer = new Timer(30, e -> {
            glowAnim += 0.08f;
            if (masterBtn != null) masterBtn.repaint();
        });
        glowTimer.start();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Network Detection
    // ═══════════════════════════════════════════════════════════════════════════

    private void detectNetwork() {
        exec.submit(() -> {
            log("Detecting network adapter…", LogPanel.Level.INFO);
            wifiAdapter = NetworkService.getWifiAdapterName();
            localIp     = NetworkService.getCurrentLocalIp();
            simulatedIp = NetworkService.getSimulatedCgnatIp();

            SwingUtilities.invokeLater(() -> {
                adapterLabel.setText(wifiAdapter);
                ipLabel.setText(localIp);
                statusBarLabel.setText(" ● Ready  |  Adapter: " + wifiAdapter + "  |  IP: " + localIp);
            });
            log("Adapter: " + wifiAdapter, LogPanel.Level.SUCCESS);
            log("Local IP: " + localIp, LogPanel.Level.SUCCESS);
            log("Ready — click ACTIVATE to enable mobile mode", LogPanel.Level.INFO);
        });
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Master Toggle
    // ═══════════════════════════════════════════════════════════════════════════

    private void toggleMobileMode() {
        mobileMode = !mobileMode;

        if (mobileMode) {
            masterBtn.setText("⛔  DEACTIVATE MOBILE MODE");
            modeLabel.setText("MOBILE DATA MODE");
            modeLabel.setForeground(ACCENT_MOB);
            ipLabel.setText(simulatedIp);
            ipLabel.setForeground(ACCENT_MOB);
            statusBarLabel.setText(" ● Mobile Mode Active  |  Adapter: " + wifiAdapter);

            // Enable all cards
            cgnatCard.setFeatureActive(true);
            dnsCard.setFeatureActive(true);
            portCard.setFeatureActive(true);
            bwCard.setFeatureActive(true);
            ipv6Card.setFeatureActive(true);
            natCard.setFeatureActive(true);

            log("══ ACTIVATING MOBILE DATA MODE ══", LogPanel.Level.WARNING);
            exec.submit(() -> {
                log("Applying CGNAT firewall block…", LogPanel.Level.INFO);
                logCard("CGNAT", FirewallService.enableCgnatBlock());

                log("Applying port blockade…", LogPanel.Level.INFO);
                logCard("Port Blockade", FirewallService.enablePortBlockade());

                log("Overriding DNS to carrier servers…", LogPanel.Level.INFO);
                logCard("DNS Override", DnsService.setCarrierDns(wifiAdapter));

                log("Applying QoS bandwidth throttle (15 Mbps)…", LogPanel.Level.INFO);
                logCard("Bandwidth Throttle", BandwidthService.applyThrottle());

                log("Disabling IPv6…", LogPanel.Level.INFO);
                logCard("IPv6 Disable", Ipv6Service.disableIpv6(wifiAdapter));

                log("Adding strict NAT UDP rules…", LogPanel.Level.INFO);
                CommandRunner.Result natR = CommandRunner.runNetsh(
                    "advfirewall","firewall","add","rule","name=WC_StrictNAT_UDP",
                    "dir=in","action=block","protocol=UDP",
                    "localport=3074,3478-3479,4500,6112,27015-27030","enable=yes");
                logCard("Strict NAT", natR.isSuccess());

                log("══ MOBILE DATA MODE ACTIVE ══", LogPanel.Level.SUCCESS);
            });

        } else {
            masterBtn.setText("⚡  ACTIVATE MOBILE MODE");
            modeLabel.setText("WIFI MODE");
            modeLabel.setForeground(ACCENT_WIFI);
            ipLabel.setText(localIp);
            ipLabel.setForeground(ACCENT_WIFI);
            statusBarLabel.setText(" ● WiFi Mode  |  Adapter: " + wifiAdapter);

            // Disable all cards
            cgnatCard.setFeatureActive(false);
            dnsCard.setFeatureActive(false);
            portCard.setFeatureActive(false);
            bwCard.setFeatureActive(false);
            ipv6Card.setFeatureActive(false);
            natCard.setFeatureActive(false);

            log("══ REVERTING TO NORMAL WIFI MODE ══", LogPanel.Level.WARNING);
            exec.submit(() -> resetNetworkSettings());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Reset / Cleanup
    // ═══════════════════════════════════════════════════════════════════════════

    private void resetAll() {
        if (mobileMode) toggleMobileMode();
        else {
            exec.submit(() -> {
                log("Manual reset requested…", LogPanel.Level.WARNING);
                resetNetworkSettings();
            });
        }
    }

    private void resetNetworkSettings() {
        log("Removing firewall rules…", LogPanel.Level.INFO);
        FirewallService.removeAllRules();
        CommandRunner.runNetsh("advfirewall","firewall","delete","rule","name=WC_StrictNAT_UDP");

        log("Restoring DHCP DNS…", LogPanel.Level.INFO);
        DnsService.restoreDhcpDns(wifiAdapter);

        log("Removing bandwidth throttle…", LogPanel.Level.INFO);
        BandwidthService.removeThrottle();

        log("Re-enabling IPv6…", LogPanel.Level.INFO);
        Ipv6Service.enableIpv6(wifiAdapter);

        log("══ ALL SETTINGS RESTORED ══", LogPanel.Level.SUCCESS);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Helpers
    // ═══════════════════════════════════════════════════════════════════════════

    private void log(String msg, LogPanel.Level level) {
        if (logPanel != null) logPanel.log(msg, level);
    }

    private void logCard(String name, boolean ok) {
        log(name + (ok ? " applied successfully" : " failed (check admin rights)"),
            ok ? LogPanel.Level.SUCCESS : LogPanel.Level.ERROR);
    }

    private void onExit() {
        if (mobileMode) {
            int r = JOptionPane.showConfirmDialog(this,
                "Mobile Data Mode is still ACTIVE.\n\nRestore all settings before closing?",
                "Restore Settings?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (r == JOptionPane.CANCEL_OPTION) return;
            if (r == JOptionPane.YES_OPTION) {
                log("Restoring on exit…", LogPanel.Level.WARNING);
                resetNetworkSettings();
            }
        }
        if (glowTimer     != null) glowTimer.stop();
        if (particleTimer != null) particleTimer.stop();
        exec.shutdownNow();
        dispose();
        System.exit(0);
    }
}
