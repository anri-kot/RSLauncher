package com.rslauncher;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class RSLauncherUI extends JFrame {
    private JTextArea statusLabel;
    private JButton startButton;
    private JButton stopButton;

    private Process appProcess;
    private Process mysqlProcess;
    private boolean isWindows;

    public RSLauncherUI() {
        setTitle("RSSECURITY Launcher");
        setSize(400, 200);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        startButton = new JButton("Iniciar sistema");
        stopButton = new JButton("Parar sistema");
        stopButton.setEnabled(false);
        statusLabel = new JTextArea(" ");

        startButton.addActionListener(e -> {
            logStatus("Inicializando sistema...");
            startSystem();
        });

        stopButton.addActionListener(e -> {
            logStatus("Parando sistema...");
            stopSystem();
        });

        setLayout(new FlowLayout());
        add(startButton);
        add(stopButton);
        add(statusLabel);

        // Fecha processos ao fechar a janela
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (appProcess != null && appProcess.isAlive()) {
                    stopSystem();
                }
            }
        });
    }

    private void logStatus(String message) {
        System.out.println(message);
        statusLabel.append(message + "\n");
    }

    private void startSystem() {
        try {
            startButton.setEnabled(false);
            stopButton.setEnabled(false);

            String os = System.getProperty("os.name").toLowerCase();
            logStatus("Sistema detectado: " + os);

            logStatus("Tentando inicializar banco de dados...");
            if (os.contains("win")) {
                mysqlProcess = new ProcessBuilder("cmd", "/c", "start", "C:\\xampp\\mysql_start.bat").start();
                isWindows = true;
            } else {
                mysqlProcess = new ProcessBuilder("bash", "-c", "/opt/lampp/lampp startmysql").start();
                int exitCode = mysqlProcess.waitFor();
                if (exitCode != 0) {
                    String error = "Erro ao iniciar MySQL. Tente executar manualmente:\nsudo /opt/lampp/lampp startmysql";
                    logStatus(error);
                    JOptionPane.showMessageDialog(null, error, "Erro", JOptionPane.ERROR_MESSAGE);
                }
                isWindows = false;
            }

            Thread.sleep(3000);

            // Inicia aplicação
            File jar = new File("app/rssecurity.jar");
            File properties = new File("app/application.properties");

            logStatus("Inicializando a aplicação...");

            appProcess = new ProcessBuilder(
                    "java", "-jar", jar.getAbsolutePath(),
                    "--spring.config.location=file:" + properties.getAbsolutePath()).start();

            Thread.sleep(7000);

            // Abre navegador
            String port = loadPort(properties);
            String url = "http://localhost:" + port;
            if (os.contains("win")) {
                new ProcessBuilder("cmd", "/c", "start", url).start();
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", url).start();
            } else {
                new ProcessBuilder("xdg-open", url).start();
            }

            logStatus("Sistema iniciado");
            JOptionPane.showMessageDialog(this, "Sistema iniciado.");

            startButton.setEnabled(false);
            stopButton.setEnabled(true);
        } catch (Exception ex) {
            ex.printStackTrace();
            String error =  "Erro ao iniciar: " + ex.getMessage();
            logStatus(error);
            JOptionPane.showMessageDialog(this, error);
        }
    }

    private void stopSystem() {
        try {
            if (appProcess != null && appProcess.isAlive()) {
                appProcess.destroy();
                appProcess = null;
            }

            if (isWindows) {
                new ProcessBuilder("cmd", "/c", "C:\\xampp\\mysql_stop.bat").start();
            } else {
                new ProcessBuilder("bash", "-c", "/opt/lampp/lampp stopmysql").start();
            }

            logStatus("Sistema encerrado.");
            JOptionPane.showMessageDialog(this, "Sistema encerrado.");

            startButton.setEnabled(true);
            stopButton.setEnabled(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String loadPort(File propFile) throws IOException {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(propFile)) {
            props.load(fis);
        }
        return props.getProperty("server.port", "8080");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RSLauncherUI().setVisible(true));
    }
}