package es.furynocturntv.mcreator.deepseek.models;

public class ModelResponse {
    public String text;
    public int tokensGenerated;
    public long inferenceTimeMs;
    public boolean truncated;
}