package server;

/**
 * Interface for server events to be observed by UI or other listeners.
 */
public interface ServerListener {

    void onLog(String message);    //when a new log message is generated
    void onConnectionCountChanged(int count);  //when the count of active client connections changes
    void onServerStatusChanged(boolean running);  //when the server status changes (started/stopped)
}
