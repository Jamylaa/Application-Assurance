package tn.vermeg.gestionproduit.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.vermeg.gestionproduit.services.chatbot.rag.RAGService;
import tn.vermeg.gestionproduit.services.chatbot.rag.VectorEmbeddingService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rag")
public class RAGController {

    private static final Logger logger = LoggerFactory.getLogger(RAGController.class);
    private final RAGService ragService;
    private final VectorEmbeddingService vectorEmbeddingService;

    public RAGController(RAGService ragService, VectorEmbeddingService vectorEmbeddingService) {
        this.ragService = ragService;
        this.vectorEmbeddingService = vectorEmbeddingService;
    }
    @PostMapping("/search")
    public ResponseEntity<Map<String, Object>> search(@RequestBody Map<String, String> request) {
        try {
            String query = request.get("query");
            String documentType = request.get("documentType");

            List<RAGService.RAGSearchResult> results;
            if (documentType != null && !documentType.isBlank()) {
                results = ragService.search(query, documentType);
            } else {
                results = ragService.search(query);
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "query", query,
                "results", results,
                "count", results.size()
            ));
        } catch (Exception e) {
            logger.error("Erreur lors de la recherche RAG: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Rechercher avec fallback sur recherche textuelle
     */
    @PostMapping("/search/fallback")
    public ResponseEntity<Map<String, Object>> searchWithFallback(@RequestBody Map<String, String> request) {
        try {
            String query = request.get("query");
            List<RAGService.RAGSearchResult> results = ragService.searchWithFallback(query);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "query", query,
                "results", results,
                "count", results.size()
            ));
        } catch (Exception e) {
            logger.error("Erreur lors de la recherche avec fallback: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Indexer tous les produits, packs et garanties
     */
    @PostMapping("/index")
    public ResponseEntity<Map<String, Object>> indexAll() {
        try {
            vectorEmbeddingService.indexAllKnowledge();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Indexation des connaissances lancée avec succès"
            ));
        } catch (Exception e) {
            logger.error("Erreur lors de l'indexation: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Réindexer une entité spécifique
     */
    @PostMapping("/index/{documentType}/{entityId}")
    public ResponseEntity<Map<String, Object>> reindexEntity(
            @PathVariable String documentType,
            @PathVariable String entityId) {
        try {
            vectorEmbeddingService.reindexEntity(documentType, entityId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Entité réindexée avec succès"
            ));
        } catch (Exception e) {
            logger.error("Erreur lors de la réindexation: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Générer un embedding pour un texte
     */
    @PostMapping("/embedding")
    public ResponseEntity<Map<String, Object>> generateEmbedding(@RequestBody Map<String, String> request) {
        try {
            String text = request.get("text");
            float[] embedding = vectorEmbeddingService.generateEmbedding(text);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "text", text,
                "embedding", embedding,
                "dimension", embedding.length
            ));
        } catch (Exception e) {
            logger.error("Erreur lors de la génération de l'embedding: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
}
