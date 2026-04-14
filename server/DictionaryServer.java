package server;

import protocol.Message;
import storage.DictionaryManager;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class DictionaryServer extends JFrame {
    private int port;
    private String dictionaryFile;
    private DictionaryManager manager;
    private ServerSocket serverSocket;
    private boolean isRunning = false;
    private ExecutorService executorService;
    private AtomicInteger activeConnections = new AtomicInteger(0);

    // GUI Components
    private JTextArea logArea;
    private JLabel connectionsLabel;
    private JButton startStopButton;

    public DictionaryServer(int port, String dictionaryFile) {
        this.port = port;
        this.dictionaryFile = dictionaryFile;
        this.manager = new DictionaryManager(dictionaryFile);
        initializeGUI();
    }

    private void initializeGUI() {
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
            if (isRunning) {
                stopServer();
            } else {
                startServer();
            }
        });

        setVisible(true);
    }

    private void startServer() {
        isRunning = true;
        startStopButton.setText("Stop Server");
        executorService = Executors.newCachedThreadPool();
        
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                log("Server started on port " + port);
                while (isRunning) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        activeConnections.incrementAndGet();
                        updateConnectionsLabel();
                        executorService.execute(() -> handleClient(clientSocket));
                    } catch (IOException e) {
                        if (isRunning) log("Error accepting connection: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                log("Could not listen on port " + port);
            }
        }).start();
    }

    private void stopServer() {
        isRunning = false;
        startStopButton.setText("Start Server");
        try {
            if (serverSocket != null) serverSocket.close();
            if (executorService != null) executorService.shutdownNow();
            log("Server stopped.");
        } catch (IOException e) {
            log("Error stopping server: " + e.getMessage());
        }
        activeConnections.set(0);
        updateConnectionsLabel();
    }

    private void handleClient(Socket socket) {
        String clientInfo = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        log("New connection from " + clientInfo);

        try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            
            Object input;
            while (isRunning && (input = in.readObject()) != null) {
                if (input instanceof Message) {
                    Message request = (Message) input;
                    log("Received " + request.getOperation() + " from " + clientInfo + " (Word: " + request.getWord() + ")");
                    
                    // Simulate delay
                    if (request.getSleepDuration() > 0) {
                        log("Simulating delay: " + request.getSleepDuration() + "ms");
                        Thread.sleep(request.getSleepDuration());
                    }

                    Message response = processRequest(request);
                    out.writeObject(response);
                    out.flush();
                }
            }
        } catch (EOFException e) {
            log("Client " + clientInfo + " disconnected.");
        } catch (Exception e) {
            log("Error handling client " + clientInfo + ": " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                // Ignore
            }
            activeConnections.decrementAndGet();
            updateConnectionsLabel();
        }
    }

    private Message processRequest(Message request) {
        Message response = new Message();
        response.setOperation(request.getOperation());
        response.setWord(request.getWord());
        
        try {
            switch (request.getOperation()) {
                case QUERY:
                    java.util.List<String> meanings = manager.query(request.getWord());
                    if (meanings != null) {
                        response.setMeanings(meanings);
                        response.setSuccess(true);
                        response.setResponseMessage("Found " + meanings.size() + " meaning(s).");
                    } else {
                        response.setSuccess(false);
                        response.setResponseMessage("Word not found.");
                    }
                    break;
                case ADD:
                    String addRes = manager.addWord(request.getWord(), request.getMeanings());
                    response.setSuccess(addRes.startsWith("Success"));
                    response.setResponseMessage(addRes);
                    break;
                case REMOVE:
                    String remRes = manager.removeWord(request.getWord());
                    response.setSuccess(remRes.startsWith("Success"));
                    response.setResponseMessage(remRes);
                    break;
                case ADD_MEANING:
                    String addMRes = manager.addMeaning(request.getWord(), request.getSecondaryMeaning());
                    response.setSuccess(addMRes.startsWith("Success"));
                    response.setResponseMessage(addMRes);
                    break;
                case UPDATE_MEANING:
                    String updRes = manager.updateMeaning(request.getWord(), request.getSecondaryMeaning(), request.getMeanings().get(0));
                    response.setSuccess(updRes.startsWith("Success"));
                    response.setResponseMessage(updRes);
                    break;
            }
        } catch (Exception e) {
            response.setSuccess(false);
            response.setResponseMessage("Internal Error: " + e.getMessage());
        }
        return response;
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(java.time.LocalTime.now() + " - " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void updateConnectionsLabel() {
        SwingUtilities.invokeLater(() -> connectionsLabel.setText("Active Connections: " + activeConnections.get()));
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java DictionaryServer <port> <dictionary-file>");
            return;
        }
        try {
            int port = Integer.parseInt(args[0]);
            String dictionaryFile = args[1];
            new DictionaryServer(port, dictionaryFile);
        } catch (NumberFormatException e) {
            System.out.println("Error: Port must be a number.");
        }
    }
}
