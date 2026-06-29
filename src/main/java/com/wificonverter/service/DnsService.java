package com.wificonverter.service;

import com.wificonverter.util.CommandRunner;

public class DnsService {

    // We use Cloudflare (1.1.1.1) and Google (8.8.8.8) to simulate 
    // an external DNS swap without breaking actual internet resolution.
    private static final String PRIMARY_DNS   = "1.1.1.1";
    private static final String SECONDARY_DNS = "8.8.8.8";

    public static boolean setCarrierDns(String adapter) {
        // Set primary DNS
        CommandRunner.Result r1 = CommandRunner.runNetsh(
            "interface", "ip", "set", "dns",
            "name=" + adapter,
            "static", PRIMARY_DNS, "primary"
        );

        // Add secondary DNS
        CommandRunner.runNetsh(
            "interface", "ip", "add", "dns",
            "name=" + adapter,
            SECONDARY_DNS, "index=2"
        );

        return r1.isSuccess();
    }

    public static boolean restoreDhcpDns(String adapter) {
        CommandRunner.Result r = CommandRunner.runNetsh(
            "interface", "ip", "set", "dns",
            "name=" + adapter,
            "source=dhcp"
        );
        return r.isSuccess();
    }

    public static String getPrimaryDns() { return PRIMARY_DNS; }
    public static String getSecondaryDns() { return SECONDARY_DNS; }
}
