package protocol;

import java.io.Serializable;
import java.util.List;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Operation {
        QUERY, ADD, REMOVE, ADD_MEANING, UPDATE_MEANING
    }

    private Operation operation;
    private String word;
    private List<String> meanings;
    private String secondaryMeaning; // update old meaning
    private int sleepDuration;
    private String responseMessage;
    private boolean success;

    // Constructors
    public Message() {
    }

    public static Message query(String word, int sleepDuration) {
        Message msg = new Message();
        msg.operation = Operation.QUERY;
        msg.word = word;
        msg.sleepDuration = sleepDuration;
        return msg;
    }

    public static Message add(String word, List<String> meanings, int sleepDuration) {
        Message msg = new Message();
        msg.operation = Operation.ADD;
        msg.word = word;
        msg.meanings = meanings;
        msg.sleepDuration = sleepDuration;
        return msg;
    }

    public static Message remove(String word, int sleepDuration) {
        Message msg = new Message();
        msg.operation = Operation.REMOVE;
        msg.word = word;
        msg.sleepDuration = sleepDuration;
        return msg;
    }

    public static Message addMeaning(String word, String newMeaning, int sleepDuration) {
        Message msg = new Message();
        msg.operation = Operation.ADD_MEANING;
        msg.word = word;
        msg.secondaryMeaning = newMeaning; // use to add meaning but not only two meanings limit
        msg.sleepDuration = sleepDuration;
        return msg;
    }

    public static Message updateMeaning(String word, String oldMeaning, String newMeaning, int sleepDuration) {
        Message msg = new Message();
        msg.operation = Operation.UPDATE_MEANING;
        msg.word = word;
        msg.secondaryMeaning = oldMeaning;
        msg.meanings = List.of(newMeaning); // Use list for the new meaning and put it to the first one
        msg.sleepDuration = sleepDuration;
        return msg;
    }

    // Getters and Setters
    public Operation getOperation() { return operation; }
    public void setOperation(Operation operation) { this.operation = operation; }

    public String getWord() { return word; }
    public void setWord(String word) { this.word = word; }

    public List<String> getMeanings() { return meanings; }
    public void setMeanings(List<String> meanings) { this.meanings = meanings; }

    public String getSecondaryMeaning() { return secondaryMeaning; }
    public void setSecondaryMeaning(String secondaryMeaning) { this.secondaryMeaning = secondaryMeaning; }

    public int getSleepDuration() { return sleepDuration; }
    public void setSleepDuration(int sleepDuration) { this.sleepDuration = sleepDuration; }

    public String getResponseMessage() { return responseMessage; }
    public void setResponseMessage(String responseMessage) { this.responseMessage = responseMessage; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
}
