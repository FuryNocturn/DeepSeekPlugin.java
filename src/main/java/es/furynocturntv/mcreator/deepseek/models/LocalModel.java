package es.furynocturntv.mcreator.deepseek.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.furynocturntv.mcreator.deepseek.services.LlamaClient;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeoutException;

public class LocalModel {
    private final String name;
    private final String modelId;
    private final ModelType type;
    private final String description;
    private final Path modelPath;
    private final LlamaClient llamaClient;
    private boolean modelLoaded = false;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Tiempos de espera (en milisegundos)
    private static final int LOAD_TIMEOUT = 120000; // 2 minutos para cargar el modelo
    private static final int GENERATION_TIMEOUT = 300000; // 5 minutos para generación

    public LocalModel(String name, String modelId, ModelType type, String description, Path modelPath) {
        this.name = name;
        this.modelId = modelId;
        this.type = type;
        this.description = description;
        this.modelPath = modelPath;

        // Configurar cliente llama.cpp
        Path binariesDir = Path.of(System.getProperty("user.home"), ".deepseek-mcreator", "binaries");
        this.llamaClient = new LlamaClient(modelPath.getParent(), binariesDir);
    }

    /**
     * Genera una respuesta usando el modelo local
     * @param prompt Texto de entrada para el modelo
     * @return Respuesta generada por el modelo
     * @throws Exception Si ocurre algún error durante la generación
     */
    public String generateResponse(String prompt) throws Exception {
        if (!modelLoaded) {
            loadModel();
        }

        // Configurar parámetros de generación según el tipo de modelo
        GenerationParams params = createGenerationParams(prompt);

        try {
            ModelResponse response = llamaClient.generateResponse(params, GENERATION_TIMEOUT);
            return processResponse(response);
        } catch (TimeoutException e) {
            throw new Exception("Tiempo de espera agotado generando respuesta", e);
        } catch (IOException e) {
            throw new Exception("Error comunicándose con el servidor de modelo local", e);
        }
    }

    /**
     * Carga el modelo en memoria
     * @throws Exception Si no se puede cargar el modelo
     */
    private synchronized void loadModel() throws Exception {
        if (!modelLoaded) {
            try {
                llamaClient.startServer(this.name, LOAD_TIMEOUT);
                modelLoaded = true;

                // Registrar shutdown hook para limpiar
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        llamaClient.stopServer();
                    } catch (Exception e) {
                        System.err.println("Error deteniendo servidor de modelo: " + e.getMessage());
                    }
                }));
            } catch (TimeoutException e) {
                throw new Exception("Tiempo de espera agotado cargando el modelo", e);
            } catch (IOException e) {
                throw new Exception("Error iniciando servidor de modelo local", e);
            }
        }
    }

    /**
     * Crea parámetros de generación según el tipo de modelo
     */
    private GenerationParams createGenerationParams(String prompt) {
        GenerationParams params = new GenerationParams();
        params.prompt = prompt;

        switch (type) {
            case CODER_33B:
                params.temperature = 0.2;
                params.maxTokens = 2048;
                params.topP = 0.95;
                params.repeatPenalty = 1.1;
                params.stopSequences = new String[]{"\n\n", "```"};
                break;

            case CHAT_7B:
                params.temperature = 0.7;
                params.maxTokens = 1024;
                params.topP = 0.9;
                break;

            default:
                params.temperature = 0.5;
                params.maxTokens = 512;
        }

        return params;
    }

    /**
     * Procesa la respuesta del modelo según su tipo
     */
    private String processResponse(ModelResponse response) {
        if (type == ModelType.CODER_33B) {
            return formatCodeResponse(response.text);
        }
        return response.text;
    }

    /**
     * Formatea respuestas de código para mejor legibilidad
     */
    private String formatCodeResponse(String code) {
        // Eliminar posibles repeticiones del prompt
        if (code.contains("```")) {
            String[] parts = code.split("```");
            if (parts.length > 1) {
                code = parts[1];
            }
        }

        return "// Respuesta generada por " + name + " (local)\n\n" +
                code.trim() + "\n\n" +
                "// Fin de la respuesta";
    }

    /**
     * Guarda la configuración del modelo en un directorio
     */
    public void saveConfig(Path directory) throws IOException {
        JSONObject config = new JSONObject();
        config.put("name", name);
        config.put("modelId", modelId);
        config.put("type", type.name());
        config.put("description", description);
        config.put("path", modelPath.toString());

        Files.writeString(
                directory.resolve("config.json"),
                config.toString(2),
                StandardOpenOption.CREATE
        );
    }

    /**
     * Crea una instancia LocalModel desde un archivo de configuración
     */
    public static LocalModel fromConfig(String name, String configJson) {
        JSONObject config = new JSONObject(configJson);
        return new LocalModel(
                config.optString("name", name),
                config.getString("modelId"),
                ModelType.valueOf(config.getString("type")),
                config.getString("description"),
                Path.of(config.optString("path", ""))
        );
    }

    /**
     * Crea una instancia LocalModel desde un directorio que contiene el modelo
     */
    public static LocalModel fromDirectory(Path directory) throws IOException {
        String config = Files.readString(directory.resolve("config.json"));
        return fromConfig(directory.getFileName().toString(), config);
    }

    // Getters
    public String getName() { return name; }
    public String getModelId() { return modelId; }
    public ModelType getType() { return type; }
    public String getDescription() { return description; }
    public Path getModelPath() { return modelPath; }
    public boolean isModelLoaded() { return modelLoaded; }

    /**
     * Clase para parámetros de generación
     */
    public static class GenerationParams {
        public String prompt;
        public double temperature = 0.7;
        public int maxTokens = 512;
        public double topP = 0.9;
        public double repeatPenalty = 1.0;
        public String[] stopSequences = new String[0];
    }
}