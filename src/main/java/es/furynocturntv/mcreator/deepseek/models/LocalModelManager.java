package es.furynocturntv.mcreator.deepseek.models;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class LocalModelManager {
    private final Map<String, LocalModel> models = new HashMap<>();
    private final Path modelsDirectory;

    public LocalModelManager() {
        this.modelsDirectory = Paths.get(System.getProperty("user.home"), ".deepseek-mcreator", "models");
        createModelsDirectoryIfNotExists();
        loadExistingModels();
    }

    private void createModelsDirectoryIfNotExists() {
        if (!Files.exists(modelsDirectory)) {
            try {
                Files.createDirectories(modelsDirectory);
            } catch (IOException e) {
                throw new RuntimeException("Could not create models directory", e);
            }
        }
    }

    private void loadExistingModels() {
        try (Stream<Path> paths = Files.list(modelsDirectory)) {
            paths.filter(Files::isDirectory)
                    .forEach(this::loadModelFromDirectory);
        } catch (IOException e) {
            System.err.println("Error loading existing models: " + e.getMessage());
        }
    }

    private void loadModelFromDirectory(Path modelDir) {
        try {
            String modelName = modelDir.getFileName().toString();
            Path configPath = modelDir.resolve("config.json");

            if (Files.exists(configPath)) {
                String config = new String(Files.readAllBytes(configPath));
                LocalModel model = LocalModel.fromConfig(modelName, config);
                models.put(modelName, model);
            }
        } catch (Exception e) {
            System.err.println("Error loading model from " + modelDir + ": " + e.getMessage());
        }
    }

    public void addModel(LocalModel model) {
        models.put(model.getName(), model);
    }

    public Optional<LocalModel> getModel(String name) {
        return Optional.ofNullable(models.get(name));
    }

    public List<String> getAvailableModels() {
        return new ArrayList<>(models.keySet());
    }

    public LocalModel importModel(File modelFile, Path targetDirectory) throws Exception {
        // Verificar si es un archivo .gguf o directorio de modelo
        if (modelFile.isDirectory()) {
            return importModelDirectory(modelFile.toPath(), targetDirectory);
        } else if (modelFile.getName().endsWith(".gguf")) {
            return importGGUFModel(modelFile.toPath(), targetDirectory);
        } else {
            throw new IllegalArgumentException("Unsupported model format");
        }
    }

    private LocalModel importModelDirectory(Path sourceDir, Path targetDirectory) throws Exception {
        String modelName = sourceDir.getFileName().toString();
        Path targetDir = targetDirectory.resolve(modelName);

        // Copiar directorio
        if (Files.exists(targetDir)) {
            throw new IOException("Model already exists: " + modelName);
        }

        Files.createDirectories(targetDir);
        Files.walk(sourceDir)
                .forEach(source -> {
                    try {
                        Path dest = targetDir.resolve(sourceDir.relativize(source));
                        Files.copy(source, dest);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

        // Cargar modelo
        LocalModel model = LocalModel.fromDirectory(targetDir);
        models.put(modelName, model);
        return model;
    }

    private LocalModel importGGUFModel(Path modelFile, Path targetDirectory) throws Exception {
        String modelName = modelFile.getFileName().toString().replace(".gguf", "");
        Path modelDir = targetDirectory.resolve(modelName);

        if (Files.exists(modelDir)) {
            throw new IOException("Model already exists: " + modelName);
        }

        Files.createDirectories(modelDir);
        Files.copy(modelFile, modelDir.resolve(modelFile.getFileName()));

        // Crear configuración básica
        LocalModel model = new LocalModel(
                modelName,
                "imported/" + modelName,
                ModelType.UNKNOWN,
                "Modelo importado por el usuario"
        );

        model.saveConfig(modelDir);
        models.put(modelName, model);
        return model;
    }
}