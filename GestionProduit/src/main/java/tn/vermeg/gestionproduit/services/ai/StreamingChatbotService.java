package tn.vermeg.gestionproduit.services.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class StreamingChatbotService {

    private static final Logger logger = LoggerFactory.getLogger(StreamingChatbotService.class);

    private final GeminiClientService geminiClient;
    private final PromptTemplateService promptTemplate;

    private final List<String> conversationHistory = new ArrayList<>();

    public StreamingChatbotService(GeminiClientService geminiClient, PromptTemplateService promptTemplate) {
        this.geminiClient = geminiClient;
        this.promptTemplate = promptTemplate;
    }

    public Flux<String> streamChatResponse(String userMessage) {
        logger.info("Streaming chat response for message: {}", userMessage);

        // Add user message to history
        conversationHistory.add("User: " + userMessage);

        // Build context from recent history (last 10 messages)
        String context = buildConversationContext();

        // Build prompt
        String prompt = promptTemplate.buildChatResponsePrompt(userMessage, context);

        // Stream response
        return geminiClient.generateContentStream(prompt, promptTemplate.buildSystemInstruction())
                .doOnNext(chunk -> logger.debug("Chunk received: {}", chunk))
                .doOnComplete(() -> {
                    // In a real implementation, we'd add the full response to history
                    logger.info("Streaming completed");
                })
                .doOnError(error -> logger.error("Streaming error: {}", error.getMessage()));
    }

    public Flux<String> streamRecommendationExplanation(String originalPrompt, java.util.Map<String, Object> criteria, 
                                                         List<Map<String, Object>> topPacks, List<Map<String, Object>> topProduits) {
        logger.info("Streaming recommendation explanation");

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append(promptTemplate.buildRecommendationPrompt(criteria, originalPrompt));
        promptBuilder.append("\n\nTop recommandations calculées :\n");
        
        if (!topPacks.isEmpty()) {
            promptBuilder.append("Packs :\n");
            for (Map<String, Object> pack : topPacks) {
                promptBuilder.append("  - ")
                    .append(pack.get("nomPack"))
                    .append(" (score ")
                    .append(pack.get("score"))
                    .append(") : ")
                    .append(pack.get("reason"))
                    .append("\n");
            }
        }
        
        if (!topProduits.isEmpty()) {
            promptBuilder.append("Produits :\n");
            for (Map<String, Object> produit : topProduits) {
                promptBuilder.append("  - ")
                    .append(produit.get("nomProduit"))
                    .append(" (score ")
                    .append(produit.get("score"))
                    .append(") : ")
                    .append(produit.get("reason"))
                    .append("\n");
            }
        }

        return geminiClient.generateContentStream(promptBuilder.toString(), promptTemplate.buildSystemInstruction())
                .doOnNext(chunk -> logger.debug("Recommendation chunk: {}", chunk))
                .doOnError(error -> logger.error("Recommendation streaming error: {}", error.getMessage()));
    }
    public void clearHistory() {
        conversationHistory.clear();
        logger.info("Conversation history cleared");
    }

    public List<String> getHistory() {
        return new ArrayList<>(conversationHistory);
    }
    private String buildConversationContext() {
        int historySize = Math.min(10, conversationHistory.size());
        if (historySize == 0) {
            return "";
        }
        
        return String.join("\n", 
            conversationHistory.subList(conversationHistory.size() - historySize, conversationHistory.size()));
    }
}
