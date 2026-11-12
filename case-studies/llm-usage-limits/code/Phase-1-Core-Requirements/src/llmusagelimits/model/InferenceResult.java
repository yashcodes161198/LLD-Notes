package llmusagelimits.model;

// What the backend returns: the answer plus the REAL token counts (Req 2, 3).
public final class InferenceResult {
    private final String text;
    private final long inputTokens;
    private final long outputTokens;

    public InferenceResult(String text, long inputTokens, long outputTokens) {
        if (text == null || inputTokens < 0 || outputTokens < 0) {
            throw new IllegalArgumentException("invalid inference result");
        }
        this.text = text;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
    }

    public String getText() { return text; }
    public long getInputTokens() { return inputTokens; }
    public long getOutputTokens() { return outputTokens; }
    public long totalTokens() { return inputTokens + outputTokens; }
}
