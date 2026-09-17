package com.aim.reviewer.gemini;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.ContentEmbedding;
import com.google.genai.types.EmbedContentResponse;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The ONLY class that talks to the Google Gen AI SDK.
 *
 * The rest of the application depends only on:
 *
 *   generateJson(...)
 *   embed(...)
 *
 * This class also provides bounded retries for temporary Gemini
 * service errors such as 503, 429 and 5xx responses.
 */
public class GeminiClient {

    private final Client client;

    private static final int MAX_RETRIES = 3;

    public GeminiClient(Client client) {
        this.client = client;
    }

    /**
     * Ask Gemini for a JSON response.
     *
     * Retries temporary service failures using exponential backoff.
     */
    public String generateJson(
            String model,
            String systemInstruction,
            String prompt) {

        GenerateContentConfig config = GenerateContentConfig.builder()
                .systemInstruction(
                        Content.fromParts(
                                Part.fromText(systemInstruction)
                        )
                )
                .responseMimeType("application/json")
                .temperature(0.2f)
                .maxOutputTokens(4096)
                .build();

        Exception lastException = null;

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {

            try {

                GenerateContentResponse response =
                        client.models.generateContent(
                                model,
                                prompt,
                                config
                        );

                String text = response.text();

                if (text == null || text.isBlank()) {
                    throw new RuntimeException(
                            "Gemini returned an empty response"
                    );
                }

                return text;

            } catch (Exception e) {

                lastException = e;

                boolean retryable = isRetryable(e);

                if (!retryable || attempt == MAX_RETRIES) {
                    break;
                }

                sleepBeforeRetry(attempt);
            }
        }

        throw new RuntimeException(
                "Gemini generation failed after "
                        + (MAX_RETRIES + 1)
                        + " attempts: "
                        + getErrorMessage(lastException),
                lastException
        );
    }

    /**
     * Embed a batch of texts.
     *
     * Retries temporary embedding-service failures.
     */
    public List<float[]> embed(
            String model,
            List<String> texts) {

        Exception lastException = null;

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {

            try {

                EmbedContentResponse response =
                        client.models.embedContent(
                                model,
                                texts,
                                null
                        );

                List<float[]> vectors = new ArrayList<>();

                for (ContentEmbedding embedding :
                        response.embeddings().orElse(List.of())) {

                    List<Float> values =
                            embedding.values().orElse(List.of());

                    float[] vector =
                            new float[values.size()];

                    for (int i = 0; i < values.size(); i++) {
                        vector[i] = values.get(i);
                    }

                    vectors.add(vector);
                }

                return vectors;

            } catch (Exception e) {

                lastException = e;

                boolean retryable = isRetryable(e);

                if (!retryable || attempt == MAX_RETRIES) {
                    break;
                }

                sleepBeforeRetry(attempt);
            }
        }

        throw new RuntimeException(
                "Gemini embedding failed after "
                        + (MAX_RETRIES + 1)
                        + " attempts: "
                        + getErrorMessage(lastException),
                lastException
        );
    }

    /**
     * Determines whether an exception represents a temporary
     * service/network problem where retrying makes sense.
     *
     * We intentionally do NOT retry authentication, invalid
     * request, or configuration errors.
     */
    private boolean isRetryable(Exception e) {

        String message = getFullExceptionMessage(e).toLowerCase();

        return message.contains("503")
                || message.contains("service unavailable")
                || message.contains("unavailable")
                || message.contains("high demand")
                || message.contains("429")
                || message.contains("resource_exhausted")
                || message.contains("rate limit")
                || message.contains("408")
                || message.contains("timeout")
                || message.contains("timed out")
                || message.contains("500")
                || message.contains("502")
                || message.contains("504")
                || message.contains("internal server error")
                || message.contains("bad gateway")
                || message.contains("gateway timeout");
    }

    /**
     * Exponential backoff:
     *
     * attempt 0 → ~1 second
     * attempt 1 → ~2 seconds
     * attempt 2 → ~4 seconds
     *
     * Small random jitter prevents repeated requests from
     * hitting the service at exactly the same time.
     */
    private void sleepBeforeRetry(int attempt) {

        long baseDelay =
                1000L * (1L << attempt);

        long jitter =
                ThreadLocalRandom.current()
                        .nextLong(250L, 750L);

        long delay = baseDelay + jitter;

        System.out.println(
                "Gemini temporary failure. "
                        + "Retrying in "
                        + delay
                        + " ms..."
        );

        try {

            Thread.sleep(delay);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new RuntimeException(
                    "Gemini retry interrupted",
                    e
            );
        }
    }

    /**
     * Safely extract the most useful error message.
     */
    private String getErrorMessage(Exception e) {

        if (e == null) {
            return "Unknown error";
        }

        String message = e.getMessage();

        if (message != null && !message.isBlank()) {
            return message;
        }

        return e.getClass().getSimpleName();
    }

    /**
     * Include nested causes because SDK exceptions sometimes
     * keep the actual HTTP error in the cause.
     */
    private String getFullExceptionMessage(Throwable throwable) {

        StringBuilder message = new StringBuilder();

        Throwable current = throwable;

        while (current != null) {

            if (current.getMessage() != null) {
                message.append(" ")
                        .append(current.getMessage());
            }

            current = current.getCause();
        }

        return message.toString();
    }
}