package es.furynocturntv.mcreator.deepseek.api;

import es.furynocturntv.mcreator.deepseek.config.DeepSeekPreferences;

public class ApiKeyManager {
    private final DeepSeekPreferences preferences;

    public ApiKeyManager() {
        this.preferences = DeepSeekPreferences.getInstance();
    }

    public void saveApiKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("La API key no puede estar vacía");
        }

        try {
            // Usamos el método seguro de DeepSeekPreferences
            preferences.setSecureApiKey(apiKey);
        } catch (Exception e) {
            throw new RuntimeException("Error al guardar la API key: " + e.getMessage());
        }
    }

    public String getApiKey() {
        try {
            // Usamos el método seguro de DeepSeekPreferences
            return preferences.getSecureApiKey();
        } catch (Exception e) {
            System.err.println("Error al recuperar la API key: " + e.getMessage());
            return "";
        }
    }

    public boolean hasApiKey() {
        String apiKey = getApiKey();
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    public void clearApiKey() {
        preferences.setApiKey(""); // Limpia la API key
    }
}