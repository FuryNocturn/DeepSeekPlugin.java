package es.furynocturntv.mcreator.deepseek.utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PluginLogger {
    private static final Path LOG_FILE = Paths.get(
            System.getProperty("user.home"),
            ".deepseek-mcreator",
            "logs",
            "deepseek_plugin.log"
    );

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public static void log(String message) {
        log(message, Level.INFO);
    }

    public static void log(String message, Level level) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String logEntry = String.format("[%s] [%s] %s\n",
                timestamp, level, message);

        // Escribir en archivo
        try {
            Files.createDirectories(LOG_FILE.getParent());
            Files.write(LOG_FILE, logEntry.getBytes(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Error writing to log file: " + e.getMessage());
        }

        // También escribir en consola en desarrollo
        if (isDevelopmentMode()) {
            System.out.print(logEntry);
        }
    }

    public static void logError(String message, Throwable throwable) {
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        log(message + "\n" + sw.toString(), Level.ERROR);
    }

    public static String getLogContent() {
        try {
            return Files.readString(LOG_FILE);
        } catch (IOException e) {
            return "No se pudo leer el archivo de log: " + e.getMessage();
        }
    }

    public static void clearLogs() {
        try {
            Files.deleteIfExists(LOG_FILE);
        } catch (IOException e) {
            System.err.println("Error clearing logs: " + e.getMessage());
        }
    }

    private static boolean isDevelopmentMode() {
        return Boolean.getBoolean("deepseek.devmode");
    }

    public enum Level {
        INFO, WARN, ERROR, DEBUG
    }
}
