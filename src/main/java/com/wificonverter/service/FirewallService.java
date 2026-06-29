package com.wificonverter.service;

import com.wificonverter.util.CommandRunner;

public class FirewallService {

    private static final String RULE_CGNAT    = "WC_CGNAT_BlockInbound";
    private static final String RULE_PORTS    = "WC_BlockServerPorts";
    private static final String RULE_PORTS_UDP = "WC_BlockP2PUDP";

    // ── CGNAT Simulation ─────────────────────────────────────────────────────
    public static boolean enableCgnatBlock() {
        // We removed the unconditional 'block any TCP in' rule because it overrides
        // Windows stateful firewall, dropping SYN-ACK responses and breaking standard internet.
        // Server Port blockade and Strict NAT UDP are sufficient for simulating mobile data.
        return true;
    }

    public static boolean disableCgnatBlock() {
        CommandRunner.runNetsh("advfirewall", "firewall", "delete", "rule", "name=" + RULE_CGNAT);
        return true;
    }

    public static boolean isCgnatBlockActive() {
        return false;
    }

    // ── Port Blockade ─────────────────────────────────────────────────────────
    // Block common hosting / server ports (HTTP, HTTPS, SSH, RDP, FTP, Minecraft, etc.)
    public static boolean enablePortBlockade() {
        boolean a = CommandRunner.runNetsh(
            "advfirewall", "firewall", "add", "rule",
            "name=" + RULE_PORTS,
            "dir=in", "action=block", "protocol=TCP",
            "localport=21,22,25,80,443,3000,3306,3389,5432,8080,8443,8888,19132,25565",
            "enable=yes",
            "description=WifiConverter: Block server ports like mobile data"
        ).isSuccess();

        boolean b = CommandRunner.runNetsh(
            "advfirewall", "firewall", "add", "rule",
            "name=" + RULE_PORTS_UDP,
            "dir=in", "action=block", "protocol=UDP",
            "localport=6881-6889,4500,51820",
            "enable=yes",
            "description=WifiConverter: Block P2P UDP ports"
        ).isSuccess();

        return a && b;
    }

    public static boolean disablePortBlockade() {
        CommandRunner.runNetsh("advfirewall", "firewall", "delete", "rule", "name=" + RULE_PORTS);
        CommandRunner.runNetsh("advfirewall", "firewall", "delete", "rule", "name=" + RULE_PORTS_UDP);
        return true;
    }

    public static boolean isPortBlockadeActive() {
        CommandRunner.Result r = CommandRunner.runNetsh(
            "advfirewall", "firewall", "show", "rule", "name=" + RULE_PORTS
        );
        return r.isSuccess() && r.output.contains(RULE_PORTS);
    }

    // ── Clean up ALL rules created by this app ────────────────────────────────
    public static void removeAllRules() {
        disableCgnatBlock();
        disablePortBlockade();
    }
}
