package es.furynocturntv.mcreator.deepseek.api;


import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class ApiKeyManager {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;

    private final byte[] encryptionKey;
    private String apiKey;

    public ApiKeyManager() {
        this.encryptionKey = loadOrGenerateEncryptionKey();
        this.apiKey = loadEncryptedApiKey();
    }

    public void saveApiKey(String apiKey) {
        try {
            this.apiKey = apiKey;
            String encrypted = encrypt(apiKey);
            PreferencesManager.PREFERENCES.set("deepseek.api_key", encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save API key", e);
        }
    }

    public String getApiKey() {
        return apiKey;
    }

    private String loadEncryptedApiKey() {
        String encrypted = PreferencesManager.PREFERENCES.get("deepseek.api_key", "");
        if (encrypted.isEmpty()) return "";

        try {
            return decrypt(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load API key", e);
        }
    }

    private byte[] loadOrGenerateEncryptionKey() {
        String key = PreferencesManager.PREFERENCES.get("deepseek.enc_key", "");
        if (key.isEmpty()) {
            byte[] newKey = generateEncryptionKey();
            String encoded = Base64.getEncoder().encodeToString(newKey);
            PreferencesManager.PREFERENCES.set("deepseek.enc_key", encoded);
            return newKey;
        }
        return Base64.getDecoder().decode(key);
    }

    private byte[] generateEncryptionKey() {
        KeyGenerator keyGenerator;
        try {
            keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(256);
            return keyGenerator.generateKey().getEncoded();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate encryption key", e);
        }
    }

    private String encrypt(String data) throws Exception {
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(encryptionKey, "AES");
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec);
        byte[] encryptedText = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

        byte[] combined = new byte[iv.length + encryptedText.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encryptedText, 0, combined, iv.length, encryptedText.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    private String decrypt(String encryptedData) throws Exception {
        byte[] combined = Base64.getDecoder().decode(encryptedData);

        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, iv.length);

        byte[] encryptedText = new byte[combined.length - GCM_IV_LENGTH];
        System.arraycopy(combined, iv.length, encryptedText, 0, encryptedText.length);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(encryptionKey, "AES");
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);

        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec);
        byte[] decryptedText = cipher.doFinal(encryptedText);

        return new String(decryptedText, StandardCharsets.UTF_8);
    }
}