package server;

/**
 * Interface for server events to be observed by UI or other listeners.
 */
public interface ServerListener {
    /**
     * Called when a new log message is generated.
     * @param message The log message.
     */
    void onLog(String message);

    /**
     * Called when the count of active client connections changes.
     * @param count The current number of active connections.
     */
    void onConnectionCountChanged(int count);

    /**
     * Called when the server status changes (started/stopped).
     * @param running True if the server is now running, false otherwise.
     */
    void onServerStatusChanged(boolean running);
}
