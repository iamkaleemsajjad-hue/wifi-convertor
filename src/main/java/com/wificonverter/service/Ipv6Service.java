package com.wificonverter.service;

import com.wificonverter.util.CommandRunner;

public class Ipv6Service {

    public static boolean disableIpv6(String adapter) {
        // Disable IPv6 on adapter — mobile data often forces IPv4-only or uses NAT64
        CommandRunner.Result r = CommandRunner.runPowerShell(
            "Disable-NetAdapterBinding -Name '" + adapter +
            "' -ComponentID ms_tcpip6 -ErrorAction SilentlyContinue"
        );
        return r.isSuccess();
    }

    public static boolean enableIpv6(String adapter) {
        CommandRunner.Result r = CommandRunner.runPowerShell(
            "Enable-NetAdapterBinding -Name '" + adapter +
            "' -ComponentID ms_tcpip6 -ErrorAction SilentlyContinue"
        );
        return r.isSuccess();
    }

    public static boolean isIpv6Enabled(String adapter) {
        CommandRunner.Result r = CommandRunner.runPowerShell(
            "(Get-NetAdapterBinding -Name '" + adapter +
            "' -ComponentID ms_tcpip6 -ErrorAction SilentlyContinue).Enabled"
        );
        return !r.output.trim().equalsIgnoreCase("False");
    }
}
