package tn.vermeg.gestionproduit.services.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import tn.vermeg.gestionproduit.exceptions.GeminiApiException;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class GeminiClientService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiClientService.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.0-flash}")
    private String model;

    @Value("${gemini.url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String baseUrl;

    @Value("${gemini.timeout-seconds:30}")
    private int timeoutSeconds;

    @Value("${gemini.max-retries:3}")
    private int maxRetries;

    public GeminiClientService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .build();
        this.objectMapper = objectMapper;
    }

    public Mono<Map<String, Object>> generateContent(String prompt, String systemInstruction) {
        return generateContent(prompt, systemInstruction, false);
    }

    public Mono<Map<String, Object>> generateContent(String prompt, String systemInstruction, boolean stream) {
        if (!isAvailable()) {
            logger.warn("Gemini API not available - missing API key");
            return Mono.error(new GeminiApiException("API key not configured", "API_KEY_MISSING"));
        }

        Map<String, Object> requestBody = buildRequestBody(prompt, systemInstruction, stream);

        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/{model}:generateContent")
                        .queryParam("key", apiKey)
                        .build(model))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .map(this::parseResponse)
                .retryWhen(Retry.backoff(maxRetries, Duration.ofSeconds(1))
                        .filter(throwable -> isRetryable(throwable))
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> 
                            new GeminiApiException("Max retries exceeded: " + retrySignal.failure().getMessage(), "MAX_RETRIES_EXCEEDED", retrySignal.failure())))
                .doOnError(e -> logger.error("Gemini API error: {} - Type: {}", e.getMessage(), e.getClass().getSimpleName()))
                .onErrorResume(e -> {
                    if (e instanceof GeminiApiException) {
                        return Mono.error(e);
                    }
                    return Mono.error(new GeminiApiException("Failed to generate content: " + e.getMessage(), "GENERATION_FAILED", e));
                });
    }

    private boolean isRetryable(Throwable throwable) {
        if (throwable instanceof WebClientResponseException) {
            WebClientResponseException ex = (WebClientResponseException) throwable;
            return ex.getStatusCode().is5xxServerError() || ex.getStatusCode().value() == 429;
        }
        return throwable instanceof java.net.SocketTimeoutException || 
               throwable instanceof java.io.IOException;
    }

    public Flux<String> generateContentStream(String prompt, String systemInstruction) {
        if (!isAvailable()) {
            logger.warn("Gemini API not available - missing API key");
            return Flux.error(new GeminiApiException("API key not configured", "API_KEY_MISSING"));
        }

        Map<String, Object> requestBody = buildRequestBody(prompt, systemInstruction, true);

        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/{model}:streamGenerateContent")
                        .queryParam("key", apiKey)
                        .build(model))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .map(this::extractTextFromChunk)
                .filter(text -> !text.isBlank())
                .retryWhen(Retry.backoff(maxRetries, Duration.ofSeconds(1))
                        .filter(this::isRetryable)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> 
                            new GeminiApiException("Max retries exceeded: " + retrySignal.failure().getMessage(), "MAX_RETRIES_EXCEEDED", retrySignal.failure())))
                .doOnError(e -> logger.error("Gemini streaming error: {} - Type: {}", e.getMessage(), e.getClass().getSimpleName()))
                .onErrorResume(e -> {
                    if (e instanceof GeminiApiException) {
                        return Flux.error(e);
                    }
                    return Flux.error(new GeminiApiException("Failed to stream content: " + e.getMessage(), "STREAM_FAILED", e));
                });
    }

    private Map<String, Object> buildRequestBody(String prompt, String systemInstruction, boolean stream) {
        Map<String, Object> body = new HashMap<>();
        
        if (systemInstruction != null && !systemInstruction.isBlank()) {
            Map<String, Object> sysInst = new HashMap<>();
            Map<String, String> parts = new HashMap<>();
            parts.put("text", systemInstruction);
            sysInst.put("parts", new Object[]{parts});
            body.put("systemInstruction", sysInst);
        }

        Map<String, Object> content = new HashMap<>();
        Map<String, String> parts = new HashMap<>();
        parts.put("text", prompt);
        content.put("role", "user");
        content.put("parts", new Object[]{parts});
        body.put("contents", new Object[]{content});

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.3);
        generationConfig.put("maxOutputTokens", 2048);
        generationConfig.put("responseMimeType", "application/json");
        body.put("generationConfig", generationConfig);

        return body;
    }

    private Map<String, Object> parseResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            
            String text = textNode.isMissingNode() ? "" : textNode.asText();
            text = stripCodeFences(text).trim();
            
            if (text.isBlank()) {
                return Map.of("error", "Empty response");
            }

            return objectMapper.readValue(text, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            logger.error("Failed to parse Gemini response: {}", e.getMessage());
            return Map.of("error", "Failed to parse response");
        }
    }

    private String extractTextFromChunk(String chunk) {
        try {
            JsonNode root = objectMapper.readTree(chunk);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode textNode = candidates.get(0).path("content").path("parts").path(0).path("text");
                return textNode.asText();
            }
        } catch (Exception e) {
            logger.debug("Failed to extract text from chunk: {}", e.getMessage());
        }
        return "";
    }

    private String stripCodeFences(String text) {
        if (text == null) return "";
        return text.replaceAll("^```(?:json)?\\s*", "").replaceAll("\\s*```$", "");
    }

    public boolean isAvailable() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }
}
