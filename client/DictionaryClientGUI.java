package client;

import protocol.Message;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

/**
 * ~ Dictionary Client GUI ~
 * Integrated with socket communication and protocol implementation.
 * 
 * @author [Student Name]
 * Student ID: [Student ID]
 */
public class DictionaryClientGUI extends JFrame {

    // Connection configuration
    private String serverAddress;
    private int port;
    private int sleepDuration;

    // Socket communication
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    // GUI Components
    private JTextField wordField;
    private JTextArea meaningArea;
    private JTextField existingMeaningField;
    private JTextField newMeaningField;
    private JTextArea resultArea;
    private JButton searchButton;
    private JButton addWordButton;
    private JButton removeWordButton;
    private JButton addMeaningButton;
    private JButton updateMeaningButton;
    private JLabel statusLabel;

    // Connection status
    private boolean isConnected = false;

    public DictionaryClientGUI(String serverAddress, int port, int sleepDuration) {
        this.serverAddress = serverAddress;
        this.port = port;
        this.sleepDuration = sleepDuration;
        initializeGUI();
        connectToServer();
    }

    private void initializeGUI() {
        setTitle("Dictionary Client - " + serverAddress + ":" + port);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Create main panels
        add(createConnectionPanel(), BorderLayout.NORTH);
        add(createOperationsPanel(), BorderLayout.CENTER);
        add(createResultPanel(), BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setResizable(true);
    }

    private JPanel createConnectionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(new TitledBorder("Connection Status"));

        statusLabel = new JLabel("Not Connected");
        statusLabel.setForeground(Color.RED);
        panel.add(statusLabel);

        return panel;
    }

    private JPanel createOperationsPanel() {
        JPanel mainPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Search Word Panel
        mainPanel.add(createSearchPanel());

        // Add Word Panel
        mainPanel.add(createAddWordPanel());

        // Remove Word Panel
        mainPanel.add(createRemoveWordPanel());

        // Update Operations Panel
        mainPanel.add(createUpdatePanel());

        return mainPanel;
    }

    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Search Word"));

        wordField = new JTextField();
        searchButton = new JButton("Search");

        panel.add(new JLabel("Word:"), BorderLayout.WEST);
        panel.add(wordField, BorderLayout.CENTER);
        panel.add(searchButton, BorderLayout.EAST);

        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchWord();
            }
        });

        return panel;
    }

    private JPanel createAddWordPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Add New Word"));

        JPanel inputPanel = new JPanel(new GridLayout(3, 2, 5, 5));

        final JTextField addWordField = new JTextField();
        meaningArea = new JTextArea(3, 20);
        meaningArea.setLineWrap(true);
        meaningArea.setWrapStyleWord(true);
        JScrollPane meaningScroll = new JScrollPane(meaningArea);

        addWordButton = new JButton("Add Word");

        inputPanel.add(new JLabel("Word:"));
        inputPanel.add(addWordField);
        inputPanel.add(new JLabel("Meaning(s):"));
        inputPanel.add(meaningScroll);
        inputPanel.add(new JLabel(""));
        inputPanel.add(addWordButton);

        panel.add(inputPanel, BorderLayout.CENTER);

        addWordButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addWord(addWordField.getText(), meaningArea.getText());
            }
        });

        return panel;
    }

    private JPanel createRemoveWordPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Remove Word"));

        final JTextField removeWordField = new JTextField();
        removeWordButton = new JButton("Remove");

        panel.add(new JLabel("Word:"), BorderLayout.WEST);
        panel.add(removeWordField, BorderLayout.CENTER);
        panel.add(removeWordButton, BorderLayout.EAST);

        removeWordButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                removeWord(removeWordField.getText());
            }
        });

        return panel;
    }

    private JPanel createUpdatePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Update Operations"));

        JPanel operationsPanel = new JPanel(new GridLayout(3, 2, 5, 5));

        final JTextField updateWordField = new JTextField();
        existingMeaningField = new JTextField();
        newMeaningField = new JTextField();

        addMeaningButton = new JButton("Add Meaning");
        updateMeaningButton = new JButton("Update Meaning");

        operationsPanel.add(new JLabel("Word:"));
        operationsPanel.add(updateWordField);
        operationsPanel.add(new JLabel("Existing Meaning:"));
        operationsPanel.add(existingMeaningField);
        operationsPanel.add(new JLabel("New Meaning:"));
        operationsPanel.add(newMeaningField);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(addMeaningButton);
        buttonPanel.add(updateMeaningButton);

        panel.add(operationsPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        addMeaningButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addMeaning(updateWordField.getText(), newMeaningField.getText());
            }
        });

        updateMeaningButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateMeaning(updateWordField.getText(),
                        existingMeaningField.getText(),
                        newMeaningField.getText());
            }
        });

        return panel;
    }

    private JPanel createResultPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Results"));

        resultArea = new JTextArea(8, 50);
        resultArea.setEditable(false);
        resultArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(resultArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Connection logic
     */
    private void connectToServer() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket(serverAddress, port);
                    out = new ObjectOutputStream(socket.getOutputStream());
                    in = new ObjectInputStream(socket.getInputStream());
                    setConnectionStatus(true);
                    displayResult("Connected to " + serverAddress + ":" + port);
                } catch (IOException e) {
                    setConnectionStatus(false);
                    displayResult("Error: Could not connect to server - " + e.getMessage());
                }
            }
        }).start();
    }

    /**
     * Shared method to send messages to the server
     */
    private void sendMessage(final Message msg) {
        if (socket == null || socket.isClosed()) {
            displayResult("Error: Not connected to server.");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    out.writeObject(msg);
                    out.flush();
                    final Message response = (Message) in.readObject();
                    
                    // Update UI on Event Dispatch Thread
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            handleResponse(response);
                        }
                    });
                } catch (IOException | ClassNotFoundException e) {
                    displayResult("Error: Communication failure - " + e.getMessage());
                    setConnectionStatus(false);
                }
            }
        }).start();
    }

    private void handleResponse(Message response) {
        if (response.isSuccess()) {
            if (response.getOperation() == Message.Operation.QUERY) {
                displayResult("Found meanings for [" + response.getWord() + "]:");
                for (String m : response.getMeanings()) {
                    displayResult(" - " + m);
                }
            } else {
                displayResult(response.getResponseMessage());
            }
        } else {
            displayResult("Error: " + response.getResponseMessage());
        }
    }

    /**
     * Search for a word in the dictionary
     * You need to implement this
     */
    private void searchWord() {
        String word = wordField.getText().trim();
        if (word.isEmpty()) {
            displayResult("Error: Please enter a word to search.");
            return;
        }
        sendMessage(Message.query(word, sleepDuration));
    }

    /**
     * Add a new word with meanings to the dictionary
     * You need to implement this
     */
    private void addWord(String word, String meanings) {
        if (word.trim().isEmpty() || meanings.trim().isEmpty()) {
            displayResult("Error: Both word and meaning(s) are required.");
            return;
        }
        List<String> meaningList = Arrays.asList(meanings.split("\n"));
        sendMessage(Message.add(word, meaningList, sleepDuration));
    }

    /**
     * Remove a word from the dictionary
     * You need to implement this
     */
    private void removeWord(String word) {
        if (word.trim().isEmpty()) {
            displayResult("Error: Please enter a word to remove.");
            return;
        }
        sendMessage(Message.remove(word, sleepDuration));
    }

    /**
     * Add a new meaning to an existing word
     * You need to implement this
     */
    private void addMeaning(String word, String newMeaning) {
        if (word.trim().isEmpty() || newMeaning.trim().isEmpty()) {
            displayResult("Error: Both word and new meaning are required.");
            return;
        }
        sendMessage(Message.addMeaning(word, newMeaning, sleepDuration));
    }

    /**
     * Update an existing meaning of a word
     * You need to implement this
     */
    private void updateMeaning(String word, String existingMeaning, String newMeaning) {
        if (word.trim().isEmpty() || existingMeaning.trim().isEmpty() || newMeaning.trim().isEmpty()) {
            displayResult("Error: Word, existing meaning, and new meaning are all required.");
            return;
        }
        sendMessage(Message.updateMeaning(word, existingMeaning, newMeaning, sleepDuration));
    }

    /**
     * Display result in the result area
     */
    private void displayResult(final String result) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                resultArea.append(java.time.LocalTime.now() + ": " + result + "\n");
                resultArea.setCaretPosition(resultArea.getDocument().getLength());
            }
        });
    }

    /**
     * Update connection status
     * You should call this method when connection status changes
     */
    public void setConnectionStatus(final boolean connected) {
        this.isConnected = connected;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (connected) {
                    statusLabel.setText("Connected");
                    statusLabel.setForeground(Color.GREEN);
                } else {
                    statusLabel.setText("Not Connected");
                    statusLabel.setForeground(Color.RED);
                }
                searchButton.setEnabled(connected);
                addWordButton.setEnabled(connected);
                removeWordButton.setEnabled(connected);
                addMeaningButton.setEnabled(connected);
                updateMeaningButton.setEnabled(connected);
            }
        });
    }

    /**
     * Main method for testing GUI
     * You should modify this to include command line argument parsing
     */
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java DictionaryClient <server-address> <server-port> <sleep-duration>");
            return;
        }

        final String address = args[0];
        final int portNum = Integer.parseInt(args[1]);
        final int sleep = Integer.parseInt(args[2]);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e) {
                    // Will use default look and feel
                }

                DictionaryClientGUI gui = new DictionaryClientGUI(address, portNum, sleep);
                gui.setVisible(true);
            }
        });
    }
}
