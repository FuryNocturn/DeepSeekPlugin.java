package es.furynocturntv.mcreator.deepseek.utils;

import java.util.Objects;

/**
 * Clase que representa una entrada en el caché de respuestas.
 * Almacena la respuesta junto con su costo y tiempo de vida.
 */
public class CacheEntry {
    private final String response;    // La respuesta almacenada
    private final double cost;        // Costo asociado a la generación de la respuesta
    private final long timestamp;     // Momento en que se creó la entrada
    private final long ttl;          // Tiempo de vida en milisegundos

    /**
     * Constructor que crea una nueva entrada de caché
     * @param response La respuesta a almacenar
     * @param cost El costo de la generación
     * @param ttl Tiempo de vida en milisegundos
     */
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

    /**
     * Verifica si la entrada ha expirado
     * @return true si el tiempo actual menos el timestamp es mayor que el TTL
     */
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