package es.furynocturntv.mcreator.deepseek.utils;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Implementa un sistema de caché para respuestas con tiempo de expiración.
 * Utiliza un LinkedHashMap con política de eliminación LRU (Least Recently Used).
 */
public class ResponseCache {
    private final Map<String, CacheEntry> cacheMap;
    private final ScheduledExecutorService cleanupExecutor;
    private final long defaultTTL;
    private final int maxSize;

    public ResponseCache(long defaultTTL, int maxSize) {
        this.defaultTTL = defaultTTL;
        this.maxSize = maxSize;
        this.cacheMap = new LinkedHashMap<String, CacheEntry>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                return size() > maxSize;
            }
        };

        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
        scheduleCleanup();
    }

    /**
     * Almacena una respuesta en el caché con el TTL por defecto
     * @param key Clave única para identificar la respuesta
     * @param response La respuesta a almacenar
     * @param cost Costo asociado a la generación de la respuesta
     */
    public void put(String key, String response, double cost) {
        put(key, response, cost, defaultTTL);
    }

    /**
     * Almacena una respuesta en el caché con un TTL específico
     */
    public synchronized void put(String key, String response, double cost, long ttl) {
        if (key == null || response == null) {
            return;
        }

        cacheMap.put(key, new CacheEntry(response, cost, ttl));
    }

    /**
     * Obtiene una respuesta del caché si existe y no ha expirado
     */
    public synchronized Optional<CacheEntry> get(String key) {
        CacheEntry entry = cacheMap.get(key);
        if (entry != null && !entry.isExpired()) {
            return Optional.of(entry);
        }
        return Optional.empty();
    }

    /**
     * Elimina todas las entradas expiradas del caché
     */
    public synchronized void cleanup() {
        Iterator<Map.Entry<String, CacheEntry>> it = cacheMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, CacheEntry> entry = it.next();
            if (entry.getValue().isExpired()) {
                it.remove();
            }
        }
    }

    /**
     * Programa la limpieza periódica del caché
     * Ejecuta la tarea de limpieza cada TTL/2 milisegundos
     */
    private void scheduleCleanup() {
        cleanupExecutor.scheduleAtFixedRate(() -> {
            try {
                cleanup();
            } catch (Exception e) {
                System.err.println("Error cleaning cache: " + e.getMessage());
            }
        }, defaultTTL / 2, defaultTTL / 2, TimeUnit.MILLISECONDS);
    }

    /**
     * Obtiene estadísticas del caché
     */
    public synchronized CacheStats getStats() {
        int total = cacheMap.size();
        int expired = 0;

        for (CacheEntry entry : cacheMap.values()) {
            if (entry.isExpired()) {
                expired++;
            }
        }

        return new CacheStats(total, expired, maxSize);
    }

    /**
     * Limpia completamente el caché
     */
    public synchronized void clear() {
        cacheMap.clear();
    }

    /**
     * Apaga el ejecutor de limpieza
     */
    public void shutdown() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Clase para estadísticas del caché
     */
    public static class CacheStats {
        public final int totalEntries;
        public final int expiredEntries;
        public final int maxSize;

        public CacheStats(int totalEntries, int expiredEntries, int maxSize) {
            this.totalEntries = totalEntries;
            this.expiredEntries = expiredEntries;
            this.maxSize = maxSize;
        }

        public double getUsagePercentage() {
            return (double) totalEntries / maxSize * 100;
        }
    }
}