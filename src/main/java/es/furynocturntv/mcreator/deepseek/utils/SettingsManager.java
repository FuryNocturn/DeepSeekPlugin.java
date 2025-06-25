package es.furynocturntv.mcreator.deepseek.utils;

import es.furynocturntv.mcreator.deepseek.config.DeepSeekPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SettingsManager {
    private final ResponseCache responseCache;
    private final List<String> availableLocalModels = new ArrayList<>();
    private final List<String> availableOnlineModels = Arrays.asList(
            "deepseek-chat",
            "deepseek-coder",
            "deepseek-math"
    );

    private final DeepSeekPreferences preferences;

    public SettingsManager(ResponseCache responseCache) {
        this.responseCache = responseCache;
        this.preferences = DeepSeekPreferences.getInstance();
        loadDefaultSettings();
    }

    private void loadDefaultSettings() {
        // Los valores por defecto ya están manejados en DeepSeekPreferences
        // No necesitamos setIfNotSet porque DeepSeekPreferences ya maneja los valores por defecto
    }

    public void saveSettings() {
        // No necesitamos hacer nada aquí porque DeepSeekPreferences guarda automáticamente
    }

    // Getters y setters actualizados
    public boolean isInternetSearchEnabled() {
        return preferences.isInternetSearchEnabled();
    }

    public void setInternetSearchEnabled(boolean enabled) {
        preferences.setInternetSearchEnabled(enabled);
    }

    public boolean isAutoImplementEnabled() {
        return preferences.isAutoImplementEnabled();
    }

    public void setAutoImplementEnabled(boolean enabled) {
        preferences.setAutoImplementEnabled(enabled);
    }

    public boolean isAnalyzeErrorsEnabled() {
        return preferences.isAnalyzeErrorsEnabled();
    }

    public void setAnalyzeErrorsEnabled(boolean enabled) {
        preferences.setAnalyzeErrorsEnabled(enabled);
    }

    public boolean isOfflineModeEnabled() {
        return preferences.isOfflineModeEnabled();
    }

    public void setOfflineModeEnabled(boolean enabled) {
        preferences.setOfflineModeEnabled(enabled);
    }

    public String getSelectedModel() {
        return preferences.getSelectedModel();
    }

    public void setSelectedModel(String model) {
        preferences.setSelectedModel(model);
    }

    public double getTemperature() {
        return preferences.getTemperature();
    }

    public void setTemperature(double temperature) {
        preferences.setTemperature(temperature);
    }

    public double getTopP() {
        return preferences.getTopP();
    }

    public void setTopP(double topP) {
        preferences.setTopP(topP);
    }

    public long getRateLimitDelay() {
        return preferences.getRateLimitDelay();
    }

    public void setRateLimitDelay(long delay) {
        preferences.setRateLimitDelay((int) delay);
    }

    public ResponseCache getResponseCache() {
        return responseCache;
    }

    public List<String> getAvailableLocalModels() {
        return preferences.getAvailableLocalModels();
    }

    public List<String> getAvailableOnlineModels() {
        return new ArrayList<>(availableOnlineModels);
    }

    public void addAvailableModel(String modelName) {
        preferences.addLocalModel(modelName);
    }

    public void setDefaultOfflineModel(String modelName) {
        if (preferences.getAvailableLocalModels().contains(modelName)) {
            preferences.setSelectedModel(modelName);
        }
    }
}