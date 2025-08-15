package com.rslauncher;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class ProcessKiller {

    public static void killAppIfRunning() {
        ProcessHandle.allProcesses()
                .filter(ProcessHandle::isAlive)
                .filter(process -> {
                    Optional<String[]> args = process.info().arguments();
                    Optional<String> cmd = process.info().commandLine();

                    return args.isPresent() && String.join(" ", args.get()).contains("rssecurity.jar")
                           || cmd.isPresent() && cmd.get().contains("rssecurity.jar");
                })
                .forEach(process -> {
                    System.out.println("Encerrando processo PID: " + process.pid());
                    process.destroy();
                });
    }

    public static Optional<ProcessHandle> findRunningByPidFile(Path pidFile) {
        try {
            if (!Files.exists(pidFile)) return Optional.empty();
            long pid = Long.parseLong(Files.readString(pidFile).trim());
            Optional<ProcessHandle> ph = ProcessHandle.of(pid)
                    .filter(ProcessHandle::isAlive);
            if (ph.isPresent()) return ph;
            Files.deleteIfExists(pidFile);
        } catch (Exception ignored) {}
        return Optional.empty();
    }

    public static boolean isAppRunning() {
        return findRunningByPidFile(RSLauncher.PID_FILE).isPresent();
    }

}