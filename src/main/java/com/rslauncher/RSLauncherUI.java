package com.rslauncher;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class RSLauncherUI extends JFrame {
    private JTextArea statusArea;
    private JButton startButton;
    private JButton stopButton;

    private Process appProcess;
    private Process mysqlProcess;
    private boolean isWindows;

    private boolean isRunning = ProcessKiller.isAppRunning();

    public RSLauncherUI() {
        setTitle("RSSECURITY Launcher");
        setSize(520, 320);
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // ===== Botões =====
        startButton = new JButton("▶ Iniciar sistema");
        stopButton = new JButton("■ Parar sistema");

        startButton.setPreferredSize(new Dimension(180, 40));
        stopButton.setPreferredSize(new Dimension(180, 40));

        startButton.setEnabled(!isRunning);
        stopButton.setEnabled(isRunning);

        startButton.addActionListener(e -> {
            logStatus("Inicializando sistema...");
            startSystem();
        });

        stopButton.addActionListener(e -> {
            logStatus("Parando sistema...");
            stopSystem();
        });

        // ===== Painel de botões =====
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);

        // ===== Área de status =====
        statusArea = new JTextArea();
        statusArea.setEditable(false);
        statusArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        statusArea.setText("🛡 Bem-vindo ao RSSECURITY Launcher!\n");

        JScrollPane scrollPane = new JScrollPane(statusArea);
        scrollPane.setPreferredSize(new Dimension(480, 180));

        // ===== Layout principal =====
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(buttonPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        setContentPane(mainPanel);

        // ===== Fecha processos ao fechar a janela =====
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
        statusArea.append(message + "\n");
    }

    private void startSystem() {
        if (isRunning) {
            ProcessKiller.killAppIfRunning();
        }
        try {
            startButton.setEnabled(false);
            stopButton.setEnabled(false);

            String appDir = Paths.get("app").toAbsolutePath().toString();
            File properties = new File(appDir + "/application.properties");

            String os = System.getProperty("os.name").toLowerCase();
            logStatus("Sistema detectado: " + os);

            logStatus("Tentando inicializar banco de dados...");
            if (os.contains("win")) {
                mysqlProcess = new ProcessBuilder("C:\\xampp\\mysql\\bin\\mysqld.exe", "--defaults-file=C:\\xampp\\mysql\\bin\\my.ini")
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                        .redirectError(ProcessBuilder.Redirect.DISCARD)
                        .start();
                isWindows = true;
            } else {
                mysqlProcess = new ProcessBuilder("bash", "-c", "/opt/lampp/lampp startmysql").start();
                isWindows = false;
            }

            Thread.sleep(2000);

            // Inicia aplicação
            File jar = new File(appDir + "/rssecurity.jar");
            String appPort = getProperty(properties, "server.port");

            logStatus("Inicializando a aplicação...");
            logStatus("App config: " + properties.getAbsolutePath());

            appProcess = new ProcessBuilder(
                    "javaw", "-jar", jar.getAbsolutePath(),
                    "--spring.config.location=file:" + properties.getAbsolutePath())
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();

            if (waitForAppStartup(Integer.parseInt(appPort), 30)) {
                logStatus("Aplicação iniciada na porta " + appPort);
            }

            // Abre navegador
            String url = "http://localhost:" + appPort;

            if (os.contains("win")) {
                Desktop.getDesktop().browse(new URI(url));
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", url).start();
            } else {
                new ProcessBuilder("xdg-open", url).start();
            }

            logStatus("Sistema iniciado na porta " + appPort);
            JOptionPane.showMessageDialog(this, "Sistema iniciado na porta " + appPort);

            startButton.setEnabled(false);
            stopButton.setEnabled(true);
        } catch (Exception ex) {
            ex.printStackTrace();
            String error =  "Erro ao iniciar: " + ex.getMessage();
            logStatus(error);
            JOptionPane.showMessageDialog(this, error);
            stopSystem();
        }
    }

    private void stopSystem() {
        try {
            if (appProcess != null && appProcess.isAlive()) {
                appProcess.destroy();
                appProcess = null;
            }

            if (isWindows) {
                new ProcessBuilder("cmd", "/c", "taskkill", "/F", "/IM", "mysqld.exe")
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                        .redirectError(ProcessBuilder.Redirect.DISCARD)
                        .start();
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

    private boolean waitForAppStartup(int port, int timeoutSeconds) {
        long start = System.currentTimeMillis();
        while ((System.currentTimeMillis() - start) < timeoutSeconds * 1000) {
            try (Socket socket = new Socket("localhost", port)) {
                return true;
            } catch (IOException e) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return false;
    }

    private String getProperty(File propFile, String property) throws IOException {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(propFile)) {
            props.load(fis);
        }
        return props.getProperty(property);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RSLauncherUI().setVisible(true));
    }
}