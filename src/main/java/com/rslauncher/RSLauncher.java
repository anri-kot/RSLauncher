package com.rslauncher;

import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import javax.swing.SwingUtilities;

public class RSLauncher {
    static FileChannel lockChannel;
    static FileLock appLock;
    static Path PID_FILE = Paths.get("app/rssecurity.pid");

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ensureSingleInstance(Paths.get("app/rssecurity.lock"));
            new RSLauncherUI().setVisible(true);
        });
    }

    public static void ensureSingleInstance(Path lockPath) {
        try {
            Files.createDirectories(lockPath.getParent());
            lockChannel = FileChannel.open(lockPath,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            appLock = lockChannel.tryLock(); // null => falhou
            if (appLock == null) {
                System.err.println("Já existe uma instância em execução.");
                System.exit(1);
            }
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try { if (appLock != null && appLock.isValid()) appLock.release(); } catch (Exception ignored) {}
                try { if (lockChannel != null && lockChannel.isOpen()) lockChannel.close(); } catch (Exception ignored) {}
                try { Files.deleteIfExists(lockPath); } catch (Exception ignored) {}
            }));
        } catch (OverlappingFileLockException e) {
            System.err.println("Já existe uma instância em execução.");
            System.exit(1);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao criar lock file", e);
        }
    }

    
    public static void writePidFile() {
        try {
            long pid = ProcessHandle.current().pid();
            Files.createDirectories(PID_FILE.getParent());
            Files.writeString(PID_FILE, Long.toString(pid),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try { Files.deleteIfExists(PID_FILE); } catch (Exception ignored) {}
            }));
        } catch (Exception e) {
            throw new RuntimeException("Não foi possível escrever PID file", e);
        }
    }
}