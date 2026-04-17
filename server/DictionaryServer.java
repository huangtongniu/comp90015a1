package server;

import protocol.Message;
import storage.DictionaryManager;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Dictionary Server Core Logic.
 * This class is independent of any GUI implementation.
 */
public class DictionaryServer {
    private int port;
    private DictionaryManager manager;
    private ServerSocket serverSocket;
    private boolean isRunning = false;
    private ExecutorService executorService;
    private AtomicInteger activeConnections = new AtomicInteger(0);
    private final List<Socket> clientSockets = Collections.synchronizedList(new ArrayList<>()); // listen to client sockets
    private ServerListener listener;

    public DictionaryServer(int port, String dictionaryFile) {
        this.port = port;
        this.manager = new DictionaryManager(dictionaryFile);
    }

    public void setListener(ServerListener listener) {
        this.listener = listener;
    }

    public synchronized void start() {
        if (isRunning) return;
        
        executorService = Executors.newCachedThreadPool();
        isRunning = true;
        notifyServerStatus(true);
        
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                printLog("Server started on port " + port);
                while (isRunning) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        clientSockets.add(clientSocket); // Track the socket
                        activeConnections.incrementAndGet();
                        notifyConnectionCount();
                        executorService.execute(() -> handleClient(clientSocket));
                    } catch (IOException e) {
                        if (isRunning) printLog("Error accepting connection: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                if (isRunning) {
                    printLog("Could not listen on port " + port + ": " + e.getMessage());
                    isRunning = false;
                    notifyServerStatus(false);
                }
            }
        }).start();
    }

    public synchronized void stop() {
        if (!isRunning) return;
        
        isRunning = false;
        try {
            if (serverSocket != null) serverSocket.close();

            if (executorService != null) executorService.shutdownNow();
            synchronized (clientSockets) {
                for (Socket s : clientSockets) {
                    try {
                        if (!s.isClosed()) s.close();
                    } catch (IOException e) {
                        // Ignore
                    }
                }
                clientSockets.clear();
            }

            printLog("Server stopped and all clients disconnected.");
        } catch (IOException e) {
            printLog("Error stopping server: " + e.getMessage());
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
        printLog("New connection from " + clientInfo);

        try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            
            Object input;
            while (isRunning && (input = in.readObject()) != null) {
                if (input instanceof Message) {
                    Message request = (Message) input;
                    printLog("Received " + request.getOperation() + " from " + clientInfo + " (Word: " + request.getWord() + ")");
                    
                    if (request.getSleepDuration() > 0) {
                        printLog("Simulating delay: " + request.getSleepDuration() + "ms");
                        Thread.sleep(request.getSleepDuration());
                    }

                    Message response = processRequest(request);
                    out.writeObject(response);
                    out.flush();
                    out.reset(); // Clear handle table to prevent memory leaks and caching issues
                }
            }
        } catch (EOFException e) {
            printLog("Client " + clientInfo + " disconnected.");
        } catch (Exception e) {
            printLog("Error handling client " + clientInfo + ": " + e.getMessage());
        } finally {
            try {
                if (!socket.isClosed()) socket.close();
            } catch (IOException e) {
                // Ignore
            }
            clientSockets.remove(socket); // Remove from tracking list
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

    private void printLog(String message) {
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
}
