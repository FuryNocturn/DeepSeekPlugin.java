package es.furynocturntv.mcreator.deepseek.models;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ModelDownloader {
    private static final String DEFAULT_MODEL = "deepseek-coder-33b-instruct";
    private static final String MODEL_URL = "https://huggingface.co/TheBloke/deepseek-coder-33B-instruct-GGUF/resolve/main/deepseek-coder-33b-instruct.Q4_K_M.gguf";

    public static Path downloadDefaultModel(Path modelsDir) throws IOException {
        if (!Files.exists(modelsDir)) {
            Files.createDirectories(modelsDir);
        }

        Path modelPath = modelsDir.resolve(DEFAULT_MODEL + ".gguf");

        if (!Files.exists(modelPath)) {
            System.out.println("Downloading DeepSeek Coder 33B Instruct model...");

            HttpURLConnection connection = (HttpURLConnection) new URL(MODEL_URL).openConnection();
            connection.setRequestMethod("GET");

            try (InputStream in = connection.getInputStream();
                 OutputStream out = Files.newOutputStream(modelPath)) {
                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            System.out.println("Model downloaded successfully to: " + modelPath);
        }

        return modelPath;
    }

    private static void downloadModelWithProgress(String url, Path target) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");

        int fileSize = connection.getContentLength();
        try (InputStream in = connection.getInputStream();
             OutputStream out = Files.newOutputStream(target)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalRead = 0;

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalRead += bytesRead;

                // Actualizar progreso (podría enviarse a una UI)
                int progress = (int) ((totalRead * 100) / fileSize);
                System.out.printf("Downloading model: %d%%\r", progress);
            }
        }
    }

    public static Path getDefaultModelPath() {
        return Paths.get(System.getProperty("user.home"), ".deepseek-mcreator", "models", DEFAULT_MODEL + ".gguf");
    }
}