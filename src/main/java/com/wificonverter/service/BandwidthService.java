package com.wificonverter.service;

import com.wificonverter.util.CommandRunner;

public class BandwidthService {

    private static final String POLICY_NAME = "WifiConverter_MobileThrottle";

    // Typical mobile data speeds (bits per second):
    // 4G LTE average: ~20 Mbps down / 8 Mbps up
    // We throttle to 15 Mbps to simulate mobile feel
    private static final long THROTTLE_BPS = 15_000_000L;

    public static boolean applyThrottle() {
        // Remove existing policy first (idempotent)
        removeThrottle();

        // Windows QoS — throttle all traffic application-wide
        String script =
            "New-NetQosPolicy " +
            "-Name '" + POLICY_NAME + "' " +
            "-AppPathNameMatchCondition '*' " +
            "-ThrottleRateActionBitsPerSecond " + THROTTLE_BPS + " " +
            "-PolicyStore ActiveStore " +
            "-Confirm:$false -ErrorAction SilentlyContinue";

        CommandRunner.Result r = CommandRunner.runPowerShell(script);
        return r.isSuccess();
    }

    public static boolean removeThrottle() {
        String script =
            "Remove-NetQosPolicy " +
            "-Name '" + POLICY_NAME + "' " +
            "-Confirm:$false -ErrorAction SilentlyContinue";

        CommandRunner.runPowerShell(script);
        return true; // Always treat removal as success
    }

    public static boolean isThrottleActive() {
        String script =
            "(Get-NetQosPolicy -Name '" + POLICY_NAME + "' " +
            "-ErrorAction SilentlyContinue) -ne $null";

        CommandRunner.Result r = CommandRunner.runPowerShell(script);
        return r.output.trim().equalsIgnoreCase("True");
    }

    public static long getThrottleBps() { return THROTTLE_BPS; }
    public static double getThrottleMbps() { return THROTTLE_BPS / 1_000_000.0; }
}
