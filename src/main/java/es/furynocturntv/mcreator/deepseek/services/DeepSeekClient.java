package es.furynocturntv.mcreator.deepseek.services;

import es.furynocturntv.mcreator.deepseek.api.ApiKeyManager;
import es.furynocturntv.mcreator.deepseek.models.LocalModel;
import es.furynocturntv.mcreator.deepseek.models.LocalModelManager;
import es.furynocturntv.mcreator.deepseek.models.ModelDownloader;
import es.furynocturntv.mcreator.deepseek.models.ModelType;
import es.furynocturntv.mcreator.deepseek.utils.CacheEntry;
import es.furynocturntv.mcreator.deepseek.utils.ResponseCache;
import es.furynocturntv.mcreator.deepseek.utils.SettingsManager;
import okhttp3.*;
import org.json.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

public class DeepSeekClient {
    // Configuración de la API
    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";
    private static final int MAX_TOKENS = 4096;
    private static final double COST_PER_INPUT_TOKEN = 0.000002;
    private static final double COST_PER_OUTPUT_TOKEN = 0.000003;

    // Componentes principales
    private final ApiKeyManager apiKeyManager;
    private final SettingsManager settingsManager;
    private final LocalModelManager localModelManager;
    private final ResponseCache responseCache;
    private final ExecutorService executorService;
    private final OkHttpClient httpClient;

    // Estado del cliente
    private volatile boolean requestCancelled = false;
    private double currentSessionCost = 0;
    private long lastRequestTimestamp = 0;

    public DeepSeekClient(ApiKeyManager apiKeyManager, SettingsManager settingsManager) {
        this.apiKeyManager = apiKeyManager;
        this.settingsManager = settingsManager;
        this.localModelManager = new LocalModelManager();
        this.responseCache = new ResponseCache(TimeUnit.HOURS.toMillis(1), 1000);

        // Configurar executor con política de reintentos
        this.executorService = Executors.newCachedThreadPool();

        // Configurar cliente HTTP con timeouts
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(new RateLimitInterceptor())
                .build();

        // Cargar modelo local por defecto
        initializeDefaultLocalModel();

        // Registrar shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    private void initializeDefaultLocalModel() {
        try {
            Path modelsDir = Paths.get(System.getProperty("user.home"), ".deepseek-mcreator", "models");
            Path modelPath = ModelDownloader.downloadDefaultModel(modelsDir);

            LocalModel defaultModel = new LocalModel(
                    "deepseek-coder-33b-instruct",
                    "deepseek-ai/deepseek-coder-33b-instruct",
                    ModelType.CODER_33B,
                    "Modelo especializado en generación de código (33B parámetros, GGUF Q4_K_M)",
                    modelPath
            );

            localModelManager.addModel(defaultModel);
            settingsManager.addAvailableModel(defaultModel.getName());
            settingsManager.setDefaultOfflineModel(defaultModel.getName());
        } catch (Exception e) {
            System.err.println("Error initializing default local model: " + e.getMessage());
        }
    }

    public String sendRequest(String prompt, String modelName) throws Exception {
        // Validar límite de tasa
        checkRateLimit();

        // Generar clave de caché
        String cacheKey = generateCacheKey(prompt, modelName);

        // Verificar caché primero
        Optional<CacheEntry> cachedResponse = responseCache.get(cacheKey);
        if (cachedResponse.isPresent()) {
            currentSessionCost += cachedResponse.get().getCost();
            return cachedResponse.get().getResponse();
        }

        // Ejecutar solicitud
        String response;
        if (settingsManager.isOfflineModeEnabled()) {
            response = sendLocalRequest(prompt, modelName);
        } else {
            response = sendApiRequest(prompt, modelName);
        }

        // Calcular costo y almacenar en caché
        double cost = calculateCost(response);
        currentSessionCost += cost;

        // Configurar TTL según tipo de modelo
        long ttl = modelName.toLowerCase().contains("33b") ?
                TimeUnit.MINUTES.toMillis(30) :
                TimeUnit.HOURS.toMillis(1);

        responseCache.put(cacheKey, response, cost, ttl);
        lastRequestTimestamp = System.currentTimeMillis();

        return response;
    }

    private String sendApiRequest(String prompt, String modelName) throws Exception {
        if (requestCancelled) {
            requestCancelled = false;
            throw new CancellationException("Request cancelled by user");
        }

        // Validar tamaño del prompt
        validatePromptSize(prompt);

        // Crear cuerpo de la solicitud
        JSONObject requestBody = createRequestBody(prompt, modelName);

        // Crear y ejecutar solicitud HTTP
        Request request = new Request.Builder()
                .url(API_URL)
                .post(RequestBody.create(requestBody.toString(), MediaType.get("application/json")))
                .addHeader("Authorization", "Bearer " + apiKeyManager.getApiKey())
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("API request failed: " + response.code() + " - " + response.body().string());
            }

            return processApiResponse(response.body().string());
        }
    }

    private String sendLocalRequest(String prompt, String modelName) throws Exception {
        LocalModel model = localModelManager.getModel(modelName)
                .orElseThrow(() -> new IllegalArgumentException("Model not found: " + modelName));

        CompletableFuture<String> future = new CompletableFuture<>();

        executorService.submit(() -> {
            try {
                String response = model.generateResponse(prompt);
                future.complete(response);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        return future.get(settingsManager.getLocalModelTimeout(), TimeUnit.SECONDS);
    }

    private JSONObject createRequestBody(String prompt, String modelName) {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", modelName);

        JSONArray messages = new JSONArray();
        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", prompt);
        messages.put(message);

        requestBody.put("messages", messages);
        requestBody.put("max_tokens", MAX_TOKENS);
        requestBody.put("temperature", settingsManager.getTemperature());
        requestBody.put("top_p", settingsManager.getTopP());

        return requestBody;
    }

    private String processApiResponse(String responseBody) throws JSONException {
        JSONObject jsonResponse = new JSONObject(responseBody);

        // Calcular costo
        int inputTokens = jsonResponse.getJSONObject("usage").getInt("prompt_tokens");
        int outputTokens = jsonResponse.getJSONObject("usage").getInt("completion_tokens");
        double cost = (inputTokens * COST_PER_INPUT_TOKEN) + (outputTokens * COST_PER_OUTPUT_TOKEN);
        currentSessionCost += cost;

        return jsonResponse.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");
    }

    private void validatePromptSize(String prompt) {
        if (prompt.length() > MAX_TOKENS * 4) {
            throw new IllegalArgumentException(
                    "Prompt too long. Max length: " + (MAX_TOKENS * 4) + " characters");
        }
    }

    private void checkRateLimit() {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastRequest = currentTime - lastRequestTimestamp;

        if (timeSinceLastRequest < settingsManager.getRateLimitDelay()) {
            throw new IllegalStateException(
                    "Rate limit exceeded. Please wait " +
                            (settingsManager.getRateLimitDelay() - timeSinceLastRequest) / 1000 +
                            " seconds before making another request.");
        }
    }

    private String generateCacheKey(String prompt, String modelName) {
        // Usar hash para manejar prompts largos eficientemente
        return modelName + "-" + Integer.toHexString(prompt.hashCode());
    }

    private double calculateCost(String response) {
        // Estimación simplificada del costo basada en longitud
        return response.length() / 1000.0 * 0.002;
    }

    public void cancelCurrentRequest() {
        requestCancelled = true;
        httpClient.dispatcher().cancelAll();
    }

    public double getCurrentSessionCost() {
        return currentSessionCost;
    }

    public void resetSessionCost() {
        currentSessionCost = 0;
    }

    public List<String> getAvailableModels() {
        if (settingsManager.isOfflineModeEnabled()) {
            return localModelManager.getAvailableModels();
        } else {
            return Arrays.asList(
                    "deepseek-chat",
                    "deepseek-coder",
                    "deepseek-math"
            );
        }
    }

    public void importLocalModel(File modelFile) throws Exception {
        LocalModel model = localModelManager.importModel(
                modelFile,
                settingsManager.getLocalModelsDirectory()
        );

        settingsManager.addAvailableModel(model.getName());
    }

    public ResponseCache.CacheStats getCacheStats() {
        return responseCache.getStats();
    }

    public void clearCache() {
        responseCache.clear();
    }

    public void shutdown() {
        executorService.shutdown();
        responseCache.shutdown();
        localModelManager.shutdown();

        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // Interceptor para manejar límites de tasa
    private class RateLimitInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            long currentTime = System.currentTimeMillis();
            long timeSinceLastRequest = currentTime - lastRequestTimestamp;

            if (timeSinceLastRequest < settingsManager.getRateLimitDelay()) {
                throw new IOException("Rate limit exceeded");
            }

            Request request = chain.request();
            Response response = chain.proceed(request);

            if (response.code() == 429) { // Too Many Requests
                response.close();
                throw new IOException("API rate limit exceeded");
            }

            return response;
        }
    }
}