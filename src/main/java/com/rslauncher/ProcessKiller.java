package com.rslauncher;

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

    public static boolean isAppRunning() {
        return ProcessHandle.allProcesses()
                .filter(ProcessHandle::isAlive)
                .anyMatch(process -> {
                    Optional<String[]> args = process.info().arguments();
                    Optional<String> cmd = process.info().commandLine();
                    return args.isPresent() && String.join(" ", args.get()).contains("rssecurity.jar")
                           || cmd.isPresent() && cmd.get().contains("rssecurity.jar");
                });
    }

}