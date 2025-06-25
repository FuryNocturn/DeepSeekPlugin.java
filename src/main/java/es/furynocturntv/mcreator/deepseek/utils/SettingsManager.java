package es.furynocturntv.mcreator.deepseek.utils;

import net.mcreator.preferences.PreferencesManager;

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

    public SettingsManager(ResponseCache responseCache) {
        this.responseCache = responseCache;
        loadDefaultSettings();
    }

    private void loadDefaultSettings() {
        // Cargar valores por defecto o de preferencias
        PreferencesManager.PREFERENCES.setIfNotSet("deepseek.internet_search", true);
        PreferencesManager.PREFERENCES.setIfNotSet("deepseek.auto_implement", false);
        PreferencesManager.PREFERENCES.setIfNotSet("deepseek.analyze_errors", true);
        PreferencesManager.PREFERENCES.setIfNotSet("deepseek.offline_mode", false);
        PreferencesManager.PREFERENCES.setIfNotSet("deepseek.selected_model", "deepseek-coder");
        PreferencesManager.PREFERENCES.setIfNotSet("deepseek.temperature", 0.7);
        PreferencesManager.PREFERENCES.setIfNotSet("deepseek.top_p", 0.9);
        PreferencesManager.PREFERENCES.setIfNotSet("deepseek.rate_limit", 3000);
    }

    public void saveSettings() {
        PreferencesManager.PREFERENCES.set("deepseek.internet_search", isInternetSearchEnabled());
        PreferencesManager.PREFERENCES.set("deepseek.auto_implement", isAutoImplementEnabled());
        PreferencesManager.PREFERENCES.set("deepseek.analyze_errors", isAnalyzeErrorsEnabled());
        PreferencesManager.PREFERENCES.set("deepseek.offline_mode", isOfflineModeEnabled());
        PreferencesManager.PREFERENCES.set("deepseek.selected_model", getSelectedModel());
        PreferencesManager.PREFERENCES.set("deepseek.temperature", getTemperature());
        PreferencesManager.PREFERENCES.set("deepseek.top_p", getTopP());
        PreferencesManager.PREFERENCES.set("deepseek.rate_limit", getRateLimitDelay());
    }

    // Getters y setters
    public boolean isInternetSearchEnabled() {
        return PreferencesManager.PREFERENCES.get("deepseek.internet_search", true);
    }

    public void setInternetSearchEnabled(boolean enabled) {
        PreferencesManager.PREFERENCES.set("deepseek.internet_search", enabled);
    }

    public boolean isAutoImplementEnabled() {
        return PreferencesManager.PREFERENCES.get("deepseek.auto_implement", false);
    }

    public void setAutoImplementEnabled(boolean enabled) {
        PreferencesManager.PREFERENCES.set("deepseek.auto_implement", enabled);
    }

    public boolean isAnalyzeErrorsEnabled() {
        return PreferencesManager.PREFERENCES.get("deepseek.analyze_errors", true);
    }

    public void setAnalyzeErrorsEnabled(boolean enabled) {
        PreferencesManager.PREFERENCES.set("deepseek.analyze_errors", enabled);
    }

    public boolean isOfflineModeEnabled() {
        return PreferencesManager.PREFERENCES.get("deepseek.offline_mode", false);
    }

    public void setOfflineModeEnabled(boolean enabled) {
        PreferencesManager.PREFERENCES.set("deepseek.offline_mode", enabled);
    }

    public String getSelectedModel() {
        return PreferencesManager.PREFERENCES.get("deepseek.selected_model", "deepseek-coder");
    }

    public void setSelectedModel(String model) {
        PreferencesManager.PREFERENCES.set("deepseek.selected_model", model);
    }

    public double getTemperature() {
        return PreferencesManager.PREFERENCES.get("deepseek.temperature", 0.7);
    }

    public void setTemperature(double temperature) {
        PreferencesManager.PREFERENCES.set("deepseek.temperature", temperature);
    }

    public double getTopP() {
        return PreferencesManager.PREFERENCES.get("deepseek.top_p", 0.9);
    }

    public void setTopP(double topP) {
        PreferencesManager.PREFERENCES.set("deepseek.top_p", topP);
    }

    public long getRateLimitDelay() {
        return PreferencesManager.PREFERENCES.get("deepseek.rate_limit", 3000);
    }

    public void setRateLimitDelay(long delay) {
        PreferencesManager.PREFERENCES.set("deepseek.rate_limit", delay);
    }

    public ResponseCache getResponseCache() {
        return responseCache;
    }

    public List<String> getAvailableLocalModels() {
        return new ArrayList<>(availableLocalModels);
    }

    public List<String> getAvailableOnlineModels() {
        return new ArrayList<>(availableOnlineModels);
    }

    public void addAvailableModel(String modelName) {
        if (!availableLocalModels.contains(modelName)) {
            availableLocalModels.add(modelName);
        }
    }

    public void setDefaultOfflineModel(String modelName) {
        if (availableLocalModels.contains(modelName)) {
            PreferencesManager.PREFERENCES.setIfNotSet("deepseek.selected_model", modelName);
        }
    }
}