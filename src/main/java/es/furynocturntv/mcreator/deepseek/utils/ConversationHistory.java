package es.furynocturntv.mcreator.deepseek.utils;

import com.google.gson.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ConversationHistory {
    private final Path historyFile;
    private final List<Conversation> conversations;
    private final Gson gson;

    public ConversationHistory() {
        this.historyFile = Paths.get(
                System.getProperty("user.home"),
                ".deepseek-mcreator",
                "conversation_history.json"
        );

        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();

        this.conversations = loadHistory();
    }

    public void addEntry(String prompt, String response, double cost) {
        Conversation conversation = new Conversation();
        conversation.timestamp = LocalDateTime.now();
        conversation.prompt = prompt;
        conversation.response = response;
        conversation.cost = cost;

        conversations.add(conversation);
        saveHistory();
    }

    public List<Conversation> getRecentConversations(int limit) {
        return conversations.stream()
                .sorted(Comparator.comparing((Conversation c) -> c.timestamp).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<Conversation> searchConversations(String query) {
        String lowerQuery = query.toLowerCase();
        return conversations.stream()
                .filter(c -> c.prompt.toLowerCase().contains(lowerQuery) ||
                        c.response.toLowerCase().contains(lowerQuery))
                .sorted(Comparator.comparing((Conversation c) -> c.timestamp).reversed())
                .collect(Collectors.toList());
    }

    public void clearHistory() {
        conversations.clear();
        saveHistory();
    }

    public double getTotalCost() {
        return conversations.stream()
                .mapToDouble(c -> c.cost)
                .sum();
    }

    private List<Conversation> loadHistory() {
        try {
            if (Files.exists(historyFile)) {
                String json = Files.readString(historyFile);
                Conversation[] loaded = gson.fromJson(json, Conversation[].class);
                return new ArrayList<>(Arrays.asList(loaded));
            }
        } catch (Exception e) {
            System.err.println("Error loading conversation history: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    private void saveHistory() {
        try {
            Files.createDirectories(historyFile.getParent());
            String json = gson.toJson(conversations);
            Files.writeString(historyFile, json);
        } catch (Exception e) {
            System.err.println("Error saving conversation history: " + e.getMessage());
        }
    }

    public static class Conversation {
        public LocalDateTime timestamp;
        public String prompt;
        public String response;
        public double cost;
    }

    private static class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
        @Override
        public JsonElement serialize(LocalDateTime src, java.lang.reflect.Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }

        @Override
        public LocalDateTime deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) {
            return LocalDateTime.parse(json.getAsString());
        }
    }
}
