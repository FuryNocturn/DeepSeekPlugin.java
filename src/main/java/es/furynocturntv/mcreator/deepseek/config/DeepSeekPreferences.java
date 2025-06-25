package es.furynocturntv.mcreator.deepseek.config;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.prefs.Preferences;

public class DeepSeekPreferences {

    private static final String PREF_NODE = "es/furynocturntv/mcreator/deepseek";
    private static DeepSeekPreferences instance;
    private final Preferences prefs;
    private final Path configFilePath;

    // Claves de preferencias
    private static final String INTERNET_SEARCH = "internet_search";
    private static final String AUTO_IMPLEMENT = "auto_implement";
    private static final String ANALYZE_ERRORS = "analyze_errors";
    private static final String OFFLINE_MODE = "offline_mode";
    private static final String SELECTED_MODEL = "selected_model";
    private static final String API_KEY = "api_key";
    private static final String TEMPERATURE = "temperature";
    private static final String TOP_P = "top_p";
    private static final String RATE_LIMIT = "rate_limit";
    private static final String LOCAL_MODELS = "local_models";
    private static final String FIRST_RUN = "first_run";

    private DeepSeekPreferences() {
        this.prefs = Preferences.userRoot().node(PREF_NODE);
        this.configFilePath = Paths.get(System.getProperty("user.home"),
                ".deepseek-mcreator", "preferences.json");
        createDefaultConfigIfNeeded();
    }

    public static synchronized DeepSeekPreferences getInstance() {
        if (instance == null) {
            instance = new DeepSeekPreferences();
        }
        return instance;
    }

    // Métodos principales
    public void savePreference(String key, String value) {
        prefs.put(key, value);
        backupToFile();
    }

    public String getPreference(String key, String defaultValue) {
        return prefs.get(key, defaultValue);
    }

    public void saveBooleanPreference(String key, boolean value) {
        prefs.putBoolean(key, value);
        backupToFile();
    }

    public boolean getBooleanPreference(String key, boolean defaultValue) {
        return prefs.getBoolean(key, defaultValue);
    }

    public void saveDoublePreference(String key, double value) {
        prefs.putDouble(key, value);
        backupToFile();
    }

    public double getDoublePreference(String key, double defaultValue) {
        return prefs.getDouble(key, defaultValue);
    }

    public void saveIntPreference(String key, int value) {
        prefs.putInt(key, value);
        backupToFile();
    }

    public int getIntPreference(String key, int defaultValue) {
        return prefs.getInt(key, defaultValue);
    }

    // Métodos específicos para el proyecto
    public boolean isInternetSearchEnabled() {
        return getBooleanPreference(INTERNET_SEARCH, true);
    }

    public void setInternetSearchEnabled(boolean enabled) {
        saveBooleanPreference(INTERNET_SEARCH, enabled);
    }

    public boolean isAutoImplementEnabled() {
        return getBooleanPreference(AUTO_IMPLEMENT, false);
    }

    public void setAutoImplementEnabled(boolean enabled) {
        saveBooleanPreference(AUTO_IMPLEMENT, enabled);
    }

    public boolean isAnalyzeErrorsEnabled() {
        return getBooleanPreference(ANALYZE_ERRORS, true);
    }

    public void setAnalyzeErrorsEnabled(boolean enabled) {
        saveBooleanPreference(ANALYZE_ERRORS, enabled);
    }

    public boolean isOfflineModeEnabled() {
        return getBooleanPreference(OFFLINE_MODE, false);
    }

    public void setOfflineModeEnabled(boolean enabled) {
        saveBooleanPreference(OFFLINE_MODE, enabled);
    }

    public String getSelectedModel() {
        return getPreference(SELECTED_MODEL, "deepseek-coder");
    }

    public void setSelectedModel(String model) {
        savePreference(SELECTED_MODEL, model);
    }

    public String getApiKey() {
        return getPreference(API_KEY, "");
    }

    public void setApiKey(String apiKey) {
        savePreference(API_KEY, apiKey);
    }

    public double getTemperature() {
        return getDoublePreference(TEMPERATURE, 0.7);
    }

    public void setTemperature(double temperature) {
        saveDoublePreference(TEMPERATURE, temperature);
    }

    public double getTopP() {
        return getDoublePreference(TOP_P, 0.9);
    }

    public void setTopP(double topP) {
        saveDoublePreference(TOP_P, topP);
    }

    public int getRateLimitDelay() {
        return getIntPreference(RATE_LIMIT, 3000);
    }

    public void setRateLimitDelay(int delayMs) {
        saveIntPreference(RATE_LIMIT, delayMs);
    }

    public List<String> getAvailableLocalModels() {
        String models = getPreference(LOCAL_MODELS, "deepseek-coder-33b-instruct");
        return new ArrayList<>(Arrays.asList(models.split(";")));
    }

    public void addLocalModel(String modelName) {
        List<String> models = getAvailableLocalModels();
        if (!models.contains(modelName)) {
            models.add(modelName);
            savePreference(LOCAL_MODELS, String.join(";", models));
        }
    }

    public boolean isFirstRun() {
        return getBooleanPreference(FIRST_RUN, true);
    }

    public void setFirstRun(boolean firstRun) {
        saveBooleanPreference(FIRST_RUN, firstRun);
    }

    // Métodos de respaldo y recuperación
    private void backupToFile() {
        try {
            Files.createDirectories(configFilePath.getParent());

            Map<String, Object> configMap = new HashMap<>();
            for (String key : getAllKeys()) {
                if (key.equals(API_KEY)) continue; // No guardamos la API key en el archivo

                String type = prefs.get(key + "_type", "");
                switch (type) {
                    case "boolean":
                        configMap.put(key, prefs.getBoolean(key, false));
                        break;
                    case "double":
                        configMap.put(key, prefs.getDouble(key, 0.0));
                        break;
                    case "int":
                        configMap.put(key, prefs.getInt(key, 0));
                        break;
                    default:
                        configMap.put(key, prefs.get(key, ""));
                }
            }

            String json = new com.google.gson.Gson().toJson(configMap);
            Files.writeString(configFilePath, json);
        } catch (IOException e) {
            System.err.println("Error al guardar preferencias en archivo: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void restoreFromBackup() {
        try {
            if (Files.exists(configFilePath)) {
                String json = Files.readString(configFilePath);
                Map<String, Object> configMap = new com.google.gson.Gson().fromJson(json, Map.class);

                for (Map.Entry<String, Object> entry : configMap.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();

                    if (value instanceof Boolean) {
                        prefs.putBoolean(key, (Boolean) value);
                    } else if (value instanceof Double) {
                        prefs.putDouble(key, (Double) value);
                    } else if (value instanceof Integer) {
                        prefs.putInt(key, (Integer) value);
                    } else {
                        prefs.put(key, value.toString());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error al cargar preferencias desde archivo: " + e.getMessage());
        }
    }

    private void createDefaultConfigIfNeeded() {
        if (isFirstRun()) {
            setDefaultValues();
            setFirstRun(false);
            backupToFile();
        } else {
            restoreFromBackup();
        }
    }

    private void setDefaultValues() {
        setInternetSearchEnabled(true);
        setAutoImplementEnabled(false);
        setAnalyzeErrorsEnabled(true);
        setOfflineModeEnabled(false);
        setSelectedModel("deepseek-coder");
        setTemperature(0.7);
        setTopP(0.9);
        setRateLimitDelay(3000);
        savePreference(LOCAL_MODELS, "deepseek-coder-33b-instruct");
    }

    private List<String> getAllKeys() {
        return Arrays.asList(
                INTERNET_SEARCH,
                AUTO_IMPLEMENT,
                ANALYZE_ERRORS,
                OFFLINE_MODE,
                SELECTED_MODEL,
                API_KEY,
                TEMPERATURE,
                TOP_P,
                RATE_LIMIT,
                LOCAL_MODELS,
                FIRST_RUN
        );
    }

    // Método para encriptación sensible (opcional)
    public void setSecureApiKey(String apiKey) {
        try {
            String encrypted = encrypt(apiKey);
            savePreference(API_KEY, encrypted);
        } catch (Exception e) {
            System.err.println("Error al encriptar API key: " + e.getMessage());
        }
    }

    public String getSecureApiKey() {
        try {
            String encrypted = getPreference(API_KEY, "");
            return decrypt(encrypted);
        } catch (Exception e) {
            System.err.println("Error al desencriptar API key: " + e.getMessage());
            return "";
        }
    }

    private String encrypt(String data) throws Exception {
        // Implementar encriptación AES (puedes usar javax.crypto)
        return data; // En producción, usar encriptación real
    }

    private String decrypt(String data) throws Exception {
        // Implementar desencriptación
        return data;
    }
}