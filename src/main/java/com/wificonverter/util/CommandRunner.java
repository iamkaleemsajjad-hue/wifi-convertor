package com.wificonverter.util;

import java.io.*;
import java.util.*;

public class CommandRunner {

    public static class Result {
        public final int exitCode;
        public final String output;
        public final String error;

        public Result(int exitCode, String output, String error) {
            this.exitCode = exitCode;
            this.output = output;
            this.error = error;
        }

        public boolean isSuccess() { return exitCode == 0; }
    }

    public static Result run(String... command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(false);
            Process process = pb.start();
            String output = readStream(process.getInputStream());
            String error  = readStream(process.getErrorStream());
            int exitCode  = process.waitFor();
            return new Result(exitCode, output, error);
        } catch (Exception e) {
            return new Result(-1, "", e.getMessage() != null ? e.getMessage() : "Unknown error");
        }
    }

    public static Result runPowerShell(String script) {
        return run("powershell", "-NonInteractive", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", script);
    }

    public static Result runNetsh(String... args) {
        List<String> cmd = new ArrayList<>();
        cmd.add("netsh");
        cmd.addAll(Arrays.asList(args));
        return run(cmd.toArray(new String[0]));
    }

    private static String readStream(InputStream stream) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString().trim();
    }
}
