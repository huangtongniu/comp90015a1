package server;

import javax.swing.*;
import java.awt.*;
import java.time.LocalTime;

/**
 * Graphical User Interface for the Dictionary Server.
 * Implements ServerListener to receive updates from the core logic.
 */
public class DictionaryServerGUI extends JFrame implements ServerListener {
    private DictionaryServer serverCore;
    
    private JTextArea logArea;
    private JLabel connectionsLabel;
    private JButton startStopButton;

    public DictionaryServerGUI(int port, String dictionaryFile) {
        this.serverCore = new DictionaryServer(port, dictionaryFile);
        this.serverCore.setListener(this);
        
        initializeGUI(port);
    }

    private void initializeGUI(int port) {
        setTitle("Dictionary Server - Port: " + port);
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Control Panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        startStopButton = new JButton("Start Server");
        connectionsLabel = new JLabel("Active Connections: 0");
        controlPanel.add(startStopButton);
        controlPanel.add(connectionsLabel);
        add(controlPanel, BorderLayout.NORTH);

        // Log Panel
        logArea = new JTextArea();
        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        startStopButton.addActionListener(e -> {
            if (serverCore.isRunning()) {
                serverCore.stop();
            } else {
                serverCore.start();
            }
        });

        setLocationRelativeTo(null);
        setVisible(true);
    }

    @Override
    public void onLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(LocalTime.now() + " - " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    @Override
    public void onConnectionCountChanged(int count) {
        SwingUtilities.invokeLater(() -> connectionsLabel.setText("Active Connections: " + count));
    }

    @Override
    public void onServerStatusChanged(boolean running) {
        SwingUtilities.invokeLater(() -> {
            startStopButton.setText(running ? "Stop Server" : "Start Server");
            if (running) {
                onLog("UI: Server is now online.");
            } else {
                onLog("UI: Server is now offline.");
            }
        });
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java server.DictionaryServerGUI <port> <dictionary-file>");
            return;
        }
        
        try {
            int port = Integer.parseInt(args[0]);
            String dictionaryFile = args[1];
            
            // Set look and feel
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {}

            SwingUtilities.invokeLater(() -> new DictionaryServerGUI(port, dictionaryFile));
            
        } catch (NumberFormatException e) {
            System.out.println("Error: Port must be a number.");
        }
    }
}
