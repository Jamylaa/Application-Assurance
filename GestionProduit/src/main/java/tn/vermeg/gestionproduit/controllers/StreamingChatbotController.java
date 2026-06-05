package tn.vermeg.gestionproduit.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import tn.vermeg.gestionproduit.services.ai.StreamingChatbotService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chatbot/stream")
@CrossOrigin(origins = "http://localhost:4200")
public class StreamingChatbotController {

    private static final Logger logger = LoggerFactory.getLogger(StreamingChatbotController.class);

    private final StreamingChatbotService streamingChatbotService;

    public StreamingChatbotController(StreamingChatbotService streamingChatbotService) {
        this.streamingChatbotService = streamingChatbotService;
    }
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.trim().isEmpty()) {
            return Flux.just("Error: Message is required");
        }

        logger.info("Streaming chat request: {}", message);
        return streamingChatbotService.streamChatResponse(message);
    }
    @PostMapping(value = "/recommendation", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamRecommendation(@RequestBody Map<String, Object> request) {
        String originalPrompt = (String) request.get("originalPrompt");
        @SuppressWarnings("unchecked")
        Map<String, Object> criteria = (Map<String, Object>) request.get("criteria");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> topPacks = (List<Map<String, Object>>) request.get("topPacks");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> topProduits = (List<Map<String, Object>>) request.get("topProduits");

        logger.info("Streaming recommendation explanation");
        return streamingChatbotService.streamRecommendationExplanation(originalPrompt, criteria, topPacks, topProduits);
    }

    @DeleteMapping("/history")
    public ResponseEntity<Map<String, String>> clearHistory() {
        streamingChatbotService.clearHistory();
        return ResponseEntity.ok(Map.of("status", "History cleared"));
    }
    @GetMapping("/history")
    public ResponseEntity<List<String>> getHistory() {
        return ResponseEntity.ok(streamingChatbotService.getHistory());
    }
@GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "Streaming Chatbot Service",
            "features", Map.of(
                "streaming", true,
                "conversationHistory", true,
                "recommendationExplanation", true
            )
        ));
    }
}