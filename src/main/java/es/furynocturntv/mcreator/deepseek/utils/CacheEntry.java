package es.furynocturntv.mcreator.deepseek.utils;

import java.util.Objects;

public class CacheEntry {
    private final String response;
    private final double cost;
    private final long timestamp;
    private final long ttl; // Tiempo de vida en milisegundos

    public CacheEntry(String response, double cost, long ttl) {
        this.response = Objects.requireNonNull(response);
        this.cost = cost;
        this.timestamp = System.currentTimeMillis();
        this.ttl = ttl;
    }

    public String getResponse() {
        return response;
    }

    public double getCost() {
        return cost;
    }

    public boolean isExpired() {
        return (System.currentTimeMillis() - timestamp) > ttl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CacheEntry that = (CacheEntry) o;
        return response.equals(that.response);
    }

    @Override
    public int hashCode() {
        return Objects.hash(response);
    }
}