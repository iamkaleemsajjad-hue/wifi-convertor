package com.wificonverter.util;

public class AdminUtil {

    public static boolean isRunningAsAdmin() {
        try {
            CommandRunner.Result result = CommandRunner.run("net", "session");
            return result.exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static void restartAsAdmin() {
        try {
            String exeFile = System.getProperty("launch4j.exefile");
            String command;

            if (exeFile != null && !exeFile.isEmpty()) {
                command = "Start-Process \"" + exeFile.replace("\"", "`\"") + "\" -Verb RunAs";
            } else {
                String jarPath = AdminUtil.class.getProtectionDomain()
                        .getCodeSource().getLocation().toURI().getPath();
                if (jarPath.startsWith("/")) jarPath = jarPath.substring(1);
                command = "Start-Process javaw -ArgumentList \"-jar `\"" + jarPath + "`\"\" -Verb RunAs";
            }

            CommandRunner.runPowerShell(command);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
