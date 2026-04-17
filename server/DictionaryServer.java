package server;

import protocol.Message;
import storage.DictionaryManager;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Core logic for the Dictionary Server.
 * Handles networking and provides hooks for UI listeners.
 */
public class DictionaryServer {
    private int port;
    private String dictionaryFile;
    private DictionaryManager manager;
    private ServerSocket serverSocket;
    private boolean isRunning = false;
    private ExecutorService executorService;
    private AtomicInteger activeConnections = new AtomicInteger(0);
    
    private ServerListener listener;

    public DictionaryServer(int port, String dictionaryFile) {
        this.port = port;
        this.dictionaryFile = dictionaryFile;
        this.manager = new DictionaryManager(dictionaryFile);
    }

    public void setListener(ServerListener listener) {
        this.listener = listener;
    }

    public void start() {
        if (isRunning) return;
        
        executorService = Executors.newCachedThreadPool();
        isRunning = true;
        notifyServerStatus(true);
        
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                log("Server started on port " + port);
                while (isRunning) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        activeConnections.incrementAndGet();
                        notifyConnectionCount();
                        executorService.execute(() -> handleClient(clientSocket));
                    } catch (IOException e) {
                        if (isRunning) log("Error accepting connection: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                log("Could not listen on port " + port);
                isRunning = false;
                notifyServerStatus(false);
            }
        }).start();
    }

    public void stop() {
        if (!isRunning) return;
        
        isRunning = false;
        try {
            if (serverSocket != null) serverSocket.close();
            if (executorService != null) executorService.shutdownNow();
            log("Server stopped.");
        } catch (IOException e) {
            log("Error stopping server: " + e.getMessage());
        }
        activeConnections.set(0);
        notifyConnectionCount();
        notifyServerStatus(false);
    }

    public boolean isRunning() {
        return isRunning;
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
            notifyConnectionCount();
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
        if (listener != null) {
            listener.onLog(message);
        } else {
            System.out.println(java.time.LocalTime.now() + " - " + message);
        }
    }

    private void notifyConnectionCount() {
        if (listener != null) {
            listener.onConnectionCountChanged(activeConnections.get());
        }
    }

    private void notifyServerStatus(boolean running) {
        if (listener != null) {
            listener.onServerStatusChanged(running);
        }
    }

    /**
     * Entry point to run the server in headless mode (Console only).
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java server.DictionaryServer <port> <dictionary-file>");
            return;
        }
        try {
            int port = Integer.parseInt(args[0]);
            String dictionaryFile = args[1];
            DictionaryServer server = new DictionaryServer(port, dictionaryFile);
            server.start();
            System.out.println("Server running in console mode. Press Ctrl+C to stop.");
        } catch (NumberFormatException e) {
            System.out.println("Error: Port must be a number.");
        }
    }
}
