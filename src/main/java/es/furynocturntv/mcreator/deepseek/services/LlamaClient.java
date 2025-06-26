package es.furynocturntv.mcreator.deepseek.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.furynocturntv.mcreator.deepseek.models.LocalModel;
import es.furynocturntv.mcreator.deepseek.models.ModelResponse;
import okhttp3.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Cliente para interactuar con el servidor local de LLaMA.cpp
 * Gestiona el inicio/parada del servidor y las peticiones de generación.
 */
public class LlamaClient {
    private static final String LOCAL_HOST = "http://127.0.0.1:8080";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final Path modelsDir;
    private final Path binariesDir;
    private Process llamaProcess;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LlamaClient(Path modelsDir, Path binariesDir) {
        this.modelsDir = modelsDir;
        this.binariesDir = binariesDir;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.MINUTES)
                .build();
    }

    /**
     * Inicia el servidor de LLaMA con el modelo especificado
     * @param modelName Nombre del modelo a cargar
     * @param timeoutMs Tiempo máximo de espera para el inicio del servidor
     * @throws IOException Si hay problemas al iniciar el proceso
     * @throws TimeoutException Si el servidor no responde en el tiempo especificado
     */
    public void startServer(String modelName, int timeoutMs) throws IOException, TimeoutException {
        Path modelPath = modelsDir.resolve(modelName + ".gguf");
        if (!Files.exists(modelPath)) {
            throw new FileNotFoundException("Model file not found: " + modelPath);
        }

        String binaryName = getBinaryName();
        Path binaryPath = binariesDir.resolve(binaryName);

        if (!Files.exists(binaryPath)) {
            throw new IllegalStateException("Llama.cpp binary not found: " + binaryPath);
        }

        List<String> command = new ArrayList<>();
        command.add(binaryPath.toString());
        command.add("-m");
        command.add(modelPath.toString());
        command.add("--port");
        command.add("8080");
        command.add("--n-gpu-layers");
        command.add("35"); // Ajustar según GPU
        command.add("--ctx-size");
        command.add("2048");

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        llamaProcess = builder.start();

        // Esperar a que el servidor esté listo
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                if (isServerReady()) {
                    return;
                }
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for server");
            }
        }

        throw new TimeoutException("Server did not start within timeout period");
    }

    private boolean isServerReady() {
        Request request = new Request.Builder()
                .url(LOCAL_HOST + "/health")
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            return response.isSuccessful();
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Genera una respuesta usando el modelo cargado
     * @param params Parámetros de generación (temperatura, top_p, etc)
     * @param timeoutMs Tiempo máximo de espera para la respuesta
     * @return La respuesta generada por el modelo
     */
    public ModelResponse generateResponse(LocalModel.GenerationParams params, int timeoutMs)
            throws IOException, TimeoutException {
        String json = objectMapper.writeValueAsString(params);
        RequestBody body = RequestBody.create(json, JSON);

        Request request = new Request.Builder()
                .url(LOCAL_HOST + "/completion")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            return objectMapper.readValue(response.body().string(), ModelResponse.class);
        }
    }

    public void stopServer() throws IOException {
        if (llamaProcess != null && llamaProcess.isAlive()) {
            llamaProcess.destroy();
            try {
                if (!llamaProcess.waitFor(5, TimeUnit.SECONDS)) {
                    llamaProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                llamaProcess.destroyForcibly();
            }
        }
    }

    private String getBinaryName() {
        String osName = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();

        if (osName.contains("win")) {
            return "main.exe";
        } else if (osName.contains("mac")) {
            return arch.contains("aarch64") ? "main-arm64" : "main";
        } else if (osName.contains("nix") || osName.contains("nux")) {
            return arch.contains("aarch64") ? "main-arm64" : "main";
        }

        throw new UnsupportedOperationException("Unsupported OS: " + osName);
    }
}