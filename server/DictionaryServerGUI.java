package server;

import javax.swing.*;
import java.awt.*;
import java.time.LocalTime;

/**
 * Graphical User Interface for the Dictionary Server.
 */
public class DictionaryServerGUI extends JFrame implements ServerListener {
    private DictionaryServer serverCore;
    
    private JTextArea logArea;
    private JLabel connectionsLabel;
    private JButton startStopButton;

    public DictionaryServerGUI(int port, String dictionaryFile) {
        // Initialize core logic
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

        // Setup button listener
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


    // --- Implementation of ServerListener ---
    public void onLog(String message) {
        // Renamed from printLog for clarity, but does the same thing
        SwingUtilities.invokeLater(() -> {
            logArea.append(LocalTime.now() + " - " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public void onConnectionCountChanged(int count) {
        SwingUtilities.invokeLater(() -> connectionsLabel.setText("Active Connections: " + count));
    }
    public void onServerStatusChanged(boolean running) {
        SwingUtilities.invokeLater(() -> {
            startStopButton.setText(running ? "Stop Server" : "Start Server");
            onLog("UI Notice: Server is now " + (running ? "ONLINE" : "OFFLINE"));
        });
    }

    /**
     * Main entry point for the Server with GUI.
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java server.DictionaryServerGUI <port> <dictionary-file>");
            return;
        }
        
        try {
            int port = Integer.parseInt(args[0]);
            String dictionaryFile = args[1];
            
            // Set system look and feel
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {}

            SwingUtilities.invokeLater(() -> new DictionaryServerGUI(port, dictionaryFile));
            
        } catch (NumberFormatException e) {
            System.out.println("Error: Port must be a number.");
        }
    }
}
