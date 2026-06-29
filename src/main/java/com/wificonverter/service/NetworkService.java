package com.wificonverter.service;

import com.wificonverter.util.CommandRunner;

public class NetworkService {

    private static String cachedAdapter = null;

    public static String getWifiAdapterName() {
        if (cachedAdapter != null) return cachedAdapter;

        // PowerShell: find first connected Wi-Fi adapter
        CommandRunner.Result r = CommandRunner.runPowerShell(
            "Get-NetAdapter | Where-Object { $_.Status -eq 'Up' -and " +
            "($_.Name -like '*Wi-Fi*' -or $_.Name -like '*WiFi*' -or $_.Name -like '*Wireless*' -or " +
            " $_.InterfaceDescription -like '*Wi-Fi*' -or $_.InterfaceDescription -like '*Wireless*') } " +
            "| Select-Object -First 1 -ExpandProperty Name"
        );
        if (r.isSuccess() && !r.output.isBlank()) {
            cachedAdapter = r.output.trim();
            return cachedAdapter;
        }

        // Fallback: netsh
        r = CommandRunner.run("netsh", "interface", "show", "interface");
        for (String line : r.output.split("\n")) {
            if (line.contains("Connected") && (line.contains("Wi-Fi") || line.contains("Wireless"))) {
                String[] parts = line.trim().split("\\s{2,}");
                if (parts.length >= 1) {
                    cachedAdapter = parts[parts.length - 1].trim();
                    return cachedAdapter;
                }
            }
        }

        cachedAdapter = "Wi-Fi";
        return cachedAdapter;
    }

    public static void resetAdapterCache() {
        cachedAdapter = null;
    }

    public static String getCurrentLocalIp() {
        CommandRunner.Result r = CommandRunner.runPowerShell(
            "(Get-NetIPAddress -AddressFamily IPv4 | " +
            "Where-Object { $_.InterfaceAlias -notlike '*Loopback*' -and $_.IPAddress -notlike '169.254.*' } | " +
            "Select-Object -First 1 -ExpandProperty IPAddress)"
        );
        if (r.isSuccess() && !r.output.isBlank()) return r.output.trim();
        return "Unknown";
    }

    public static String getSimulatedCgnatIp() {
        // CGNAT range 100.64.0.0/10
        int second = (int)(Math.random() * 63) + 64;
        int third  = (int)(Math.random() * 254) + 1;
        int fourth = (int)(Math.random() * 254) + 1;
        return "100." + second + "." + third + "." + fourth;
    }

    public static boolean isWifiConnected() {
        CommandRunner.Result r = CommandRunner.runPowerShell(
            "(Get-NetAdapter | Where-Object { $_.Status -eq 'Up' -and " +
            "($_.Name -like '*Wi-Fi*' -or $_.InterfaceDescription -like '*Wi-Fi*') }).Count -gt 0"
        );
        return r.output.trim().equalsIgnoreCase("True");
    }

    public static String getCurrentDns(String adapter) {
        CommandRunner.Result r = CommandRunner.runPowerShell(
            "(Get-DnsClientServerAddress -InterfaceAlias '" + adapter +
            "' -AddressFamily IPv4 | Select-Object -ExpandProperty ServerAddresses) -join ', '"
        );
        if (r.isSuccess() && !r.output.isBlank()) return r.output.trim();
        return "DHCP";
    }
}
