package storage;

import java.io.*;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 1. Concurrent access to the dictionary, multiple clients can access the dictionary at the same time but only one client can write.
 * 2. Query, add, remove, add and update meaning
 * 3. Load and save dictionary to file with pre-defined format
 */
public class DictionaryManager {
    private final Map<String, List<String>> dictionary = new HashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final String filePath;

    public DictionaryManager(String filePath) {
        this.filePath = filePath;
        loadFromFile();
    }

    private void loadFromFile() {
        File file = new File(filePath);
        if (!file.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" : ", 2);
                if (parts.length == 2) {
                    String word = parts[0].trim().toLowerCase();
                    String[] meaningsArr = parts[1].split("\\|");
                    List<String> meanings = new ArrayList<>();
                    for (String m : meaningsArr) {
                        if (!m.trim().isEmpty()) {
                            meanings.add(m.trim());
                        }
                    }
                    dictionary.put(word, meanings);
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading dictionary: " + e.getMessage());
        }
    }

    private void saveToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (Map.Entry<String, List<String>> entry : dictionary.entrySet()) {
                writer.write(entry.getKey() + " : " + String.join("|", entry.getValue()));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error saving dictionary: " + e.getMessage());
        }
    }

    public List<String> query(String word) {
        lock.readLock().lock();
        try {
            return dictionary.get(word.toLowerCase());
        } finally {
            lock.readLock().unlock();
        }
    }

    public String addWord(String word, List<String> meanings) {
        lock.writeLock().lock();
        try {
            word = word.toLowerCase();
            if (dictionary.containsKey(word)) {
                return "Error! Word already exists.";
            }
            // Here handle special case with space or '\t'
            if (meanings == null || meanings.isEmpty() || (meanings.size() == 1 && meanings.get(0).trim().isEmpty())) {
                return "Error! Meaning cannot be empty.";
            }
            dictionary.put(word, new ArrayList<>(meanings));
            saveToFile();
            return "Success! Word added.";
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String removeWord(String word) {
        lock.writeLock().lock();
        try {
            word = word.toLowerCase();
            if (!dictionary.containsKey(word)) {
                return "Error! Word not found.";
            }
            dictionary.remove(word);
            saveToFile();
            return "Success! Word removed.";
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String addMeaning(String word, String newMeaning) {
        lock.writeLock().lock();
        try {
            word = word.toLowerCase();
            if (!dictionary.containsKey(word)) {
                return "Error! Word not found.";
            }
            List<String> meanings = dictionary.get(word);
            if (meanings.contains(newMeaning)) {
                return "Error! Meaning already exists.";
            }
            if (newMeaning.trim().isEmpty()) {
                return "Error! Meaning cannot be empty.";
            }
            meanings.add(newMeaning);
            saveToFile();
            return "Success! Meaning added.";
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String updateMeaning(String word, String oldMeaning, String newMeaning) {
        lock.writeLock().lock();
        try {
            word = word.toLowerCase();
            if (!dictionary.containsKey(word)) {
                return "Error! Word not found.";
            }
            List<String> meanings = dictionary.get(word);
            int index = meanings.indexOf(oldMeaning);
            if (index == -1) {
                return "Error! Original meaning not found.";
            }
            if (newMeaning.trim().isEmpty()) {
                return "Error! New meaning cannot be empty.";
            }
            if (meanings.contains(newMeaning) && !oldMeaning.equals(newMeaning)) {
                return "Error! New meaning already exists.";
            }
            meanings.set(index, newMeaning);
            saveToFile();
            return "Success! Meaning updated.";
        } finally {
            lock.writeLock().unlock();
        }
    }
}
