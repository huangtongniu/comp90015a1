# Multi-Threaded Distributed Dictionary System

This is a multi-threaded distributed dictionary system built with Java, utilizing a classic Client-Server architecture. It features concurrent multi-user access, real-time data persistence, and a highly decoupled Graphical User Interface.

## 🌟 Key Features

*   **High Performance Concurrency**: The server implements an `ExecutorService` thread pool model, capable of handling a large number of simultaneous client connections efficiently.
*   **Robust Consistency**: Integrated `ReentrantReadWriteLock` ensures "concurrent read, exclusive write" access, maximizing search throughput while maintaining data integrity.
*   **Architectural Decoupling**: The server core logic is independent of the GUI via the **Observer Pattern**. It can run in headless mode or with the provided Swing interface.
*   **Real-time Connection Sensing**: The client features a dedicated background **Receiver Thread**, providing instantaneous feedback if the server closes or the connection drops.
*   **Visual Monitoring**: Server GUI provides a live log stream and an active connection counter for real-time monitoring.
*   **Simulation Delay Support**: Custom `sleep-duration` argument allows simulating network latency to verify asynchronous non-blocking performance.

## 📁 Project Structure

```text
Dictionary/
├── client/           # Client-side GUI and response handling logic
├── server/           # Server-side core logic, Listener interface, and GUI wrapper
├── protocol/         # Communication protocol (Message object definition)
├── storage/          # Storage layer (DictionaryManager, file I/O, and locking)
├── out/              # Compiled bytecode directory (.class files)
├── dic.txt           # Default dictionary data file
└── project_structure.puml  # UML class diagram for the project
```

## 🚀 Getting Started

### 1. Compilation
Run the following command from the project root:
```bash
javac -d out protocol/*.java storage/*.java server/*.java client/*.java
```

### 2. Packaging (JAR)
Package the compiled classes into executable JAR files:
```bash
# Package Server
jar cfe DictionaryServer.jar server.DictionaryServerGUI -C out .

# Package Client
jar cfe DictionaryClient.jar client.DictionaryClientGUI -C out .
```

### 3. Execution

#### Run Server:
```bash
java -jar DictionaryServer.jar <port> <dictionary-file>
# Example: java -jar DictionaryServer.jar 3005 dic.txt
```

#### Run Client:
```bash
java -jar DictionaryClient.jar <server-address> <port> <sleep-duration>
# Example: java -jar DictionaryClient.jar localhost 3005 500
```
*   `<sleep-duration>`: Simulated network latency in milliseconds. Set to `0` for no delay.

## 🛠 Technical Details

### Networking & Protocol
*   **TCP/IP**: Built on top of the Transmission Control Protocol to guarantee reliable data delivery without loss or corruption.
*   **Serialization**: Uses Java's `ObjectInputStream/ObjectOutputStream` for direct object-level communication, ensuring type safety and efficient data exchange.

### Concurrency Model
*   **Server**: Uses a `Thread-per-connection` strategy within a managed thread pool. The main thread blocks on `accept()`, while worker threads handle individual business logic.
*   **Client**: Decouples the UI event thread from the network communication thread. This prevents "GUI Freezing" even during high-latency (Simulated Delay) operations.

### Data Persistence
*   All mutations (`ADD`, `REMOVE`, `UPDATE`) are synchronized to the local text file in real-time.
*   The system supports fault-tolerant parsing for the `word : meaning1|meaning2` data format during initialization.

---
*Created as a project for Distributed Computing, adhering to Single Responsibility and SOLID design principles.*