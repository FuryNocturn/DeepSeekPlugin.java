package es.furynocturntv.mcreator.deepseek.api;

import es.furynocturntv.mcreator.deepseek.config.DeepSeekPreferences;

/**
 * Gestor de claves API para el servicio DeepSeek.
 * Maneja el almacenamiento seguro y recuperación de claves API.
 */
public class ApiKeyManager {
    private final DeepSeekPreferences preferences;

    public ApiKeyManager() {
        this.preferences = DeepSeekPreferences.getInstance();
    }

    /**
     * Guarda una nueva clave API de forma segura
     * @param apiKey Clave API a almacenar
     * @throws IllegalArgumentException si la clave es nula o vacía
     */
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

    /**
     * Verifica si existe una clave API válida almacenada
     * @return true si hay una clave API no vacía almacenada
     */
    public boolean hasApiKey() {
        String apiKey = getApiKey();
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    public void clearApiKey() {
        preferences.setApiKey(""); // Limpia la API key
    }
}