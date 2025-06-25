package es.furynocturntv.mcreator.deepseek.utils;

import okhttp3.*;

import java.io.IOException;

public class RateLimitInterceptor implements Interceptor {
    private final SettingsManager settingsManager;
    private long lastRequestTime = 0;

    public RateLimitInterceptor(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - lastRequestTime;
        long rateLimitDelay = settingsManager.getRateLimitDelay();

        if (elapsed < rateLimitDelay) {
            try {
                long waitTime = rateLimitDelay - elapsed;
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Request interrupted during rate limit delay");
            }
        }

        Request request = chain.request();
        Response response = chain.proceed(request);
        lastRequestTime = System.currentTimeMillis();

        return response;
    }
}