package es.furynocturntv.mcreator.deepseek.utils;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class PluginLoader {
    private final Path pluginsDir;
    private final List<DeepSeekExtension> extensions;

    public PluginLoader() {
        this.pluginsDir = Paths.get(
                System.getProperty("user.home"),
                ".deepseek-mcreator",
                "plugins"
        );
        this.extensions = new ArrayList<>();
    }

    public void loadPlugins() {
        try {
            Files.createDirectories(pluginsDir);

            // Cargar plugins del directorio
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(pluginsDir, "*.jar")) {
                for (Path pluginPath : stream) {
                    try {
                        loadPlugin(pluginPath);
                    } catch (Exception e) {
                        PluginLogger.logError("Error loading plugin: " + pluginPath, e);
                    }
                }
            }
        } catch (IOException e) {
            PluginLogger.logError("Error accessing plugins directory", e);
        }
    }

    private void loadPlugin(Path pluginPath) throws Exception {
        URLClassLoader loader = URLClassLoader.newInstance(
                new URL[] { pluginPath.toUri().toURL() },
                getClass().getClassLoader()
        );

        // Buscar clases que implementen DeepSeekExtension
        try (JarFile jar = new JarFile(pluginPath.toFile())) {
            Enumeration<JarEntry> entries = jar.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    String className = entry.getName()
                            .replace("/", ".")
                            .replace(".class", "");

                    try {
                        Class<?> clazz = loader.loadClass(className);
                        if (DeepSeekExtension.class.isAssignableFrom(clazz) &&
                                !clazz.equals(DeepSeekExtension.class)) {

                            DeepSeekExtension extension = (DeepSeekExtension) clazz.getDeclaredConstructor().newInstance();
                            extensions.add(extension);
                            PluginLogger.log("Loaded extension: " + extension.getName());
                        }
                    } catch (NoClassDefFoundError | ClassNotFoundException e) {
                        // Ignorar clases que no se pueden cargar
                    }
                }
            }
        }
    }

    public List<DeepSeekExtension> getExtensions() {
        return Collections.unmodifiableList(extensions);
    }

    public interface DeepSeekExtension {
        String getName();
        void initialize();
        void onPrompt(String prompt);
        void onResponse(String response);
    }
}