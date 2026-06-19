package tn.vermeg.gestionproduit.services.chatbot.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// Temporairement désactivé car Spring AI n'est pas disponible
// import org.springframework.ai.embedding.EmbeddingModel;
// import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tn.vermeg.gestionproduit.entities.KnowledgeDocument;
import tn.vermeg.gestionproduit.repositories.KnowledgeDocumentRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Service
public class RAGService {

    private static final Logger logger = LoggerFactory.getLogger(RAGService.class);
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final VectorEmbeddingService vectorEmbeddingService;

    @Value("${rag.search.enabled:true}")
    private boolean ragEnabled;

    @Value("${rag.search.top-k:5}")
    private int topK;

    @Value("${rag.search.similarity-threshold:0.7}")
    private double similarityThreshold;

    public RAGService(
            // EmbeddingModel embeddingModel,
            KnowledgeDocumentRepository knowledgeDocumentRepository,
            VectorEmbeddingService vectorEmbeddingService) {
        // this.embeddingModel = embeddingModel;
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
        this.vectorEmbeddingService = vectorEmbeddingService;
    }
    public List<RAGSearchResult> search(String query) {
        return search(query, null);
    }
    public List<RAGSearchResult> search(String query, String documentType) {
        if (!ragEnabled) {
            logger.debug("RAG désactivé, recherche ignorée");
            return new ArrayList<>();
        }

        if (query == null || query.isBlank()) {
            return new ArrayList<>();
        }

        try {
            // Générer l'embedding de la requête
            float[] queryEmbedding = vectorEmbeddingService.generateEmbedding(query);
            if (queryEmbedding.length == 0) {
                logger.warn("Échec de la génération de l'embedding pour la requête - fallback automatique sur recherche textuelle");
                return textSearch(query, documentType);
            }

            // Récupérer tous les documents
            List<KnowledgeDocument> documents;
            if (documentType != null) {
                documents = knowledgeDocumentRepository.findByDocumentType(documentType);
            } else {
                documents = knowledgeDocumentRepository.findAll();
            }

            // Calculer les similarités
            List<RAGSearchResult> results = new ArrayList<>();
            for (KnowledgeDocument doc : documents) {
                if (doc.getEmbedding() != null && doc.getEmbedding().length > 0) {
                    double similarity = cosineSimilarity(queryEmbedding, doc.getEmbedding());
                    if (similarity >= similarityThreshold) {
                        RAGSearchResult result = new RAGSearchResult();
                        result.setDocumentId(doc.getId());
                        result.setDocumentType(doc.getDocumentType());
                        result.setEntityId(doc.getEntityId());
                        result.setTitle(doc.getTitle());
                        result.setContent(doc.getContent());
                        result.setSimilarity(similarity);
                        result.setMetadata(doc.getMetadata());
                        results.add(result);
                    }
                }
            }

            // Si aucun résultat sémantique, fallback sur recherche textuelle
            if (results.isEmpty()) {
                logger.debug("Aucun résultat sémantique trouvé - fallback sur recherche textuelle");
                return textSearch(query, documentType);
            }

            // Trier par similarité décroissante et limiter aux top-k résultats
            return results.stream()
                    .sorted(Comparator.comparingDouble(RAGSearchResult::getSimilarity).reversed())
                    .limit(topK)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Erreur lors de la recherche RAG: {} - fallback sur recherche textuelle", e.getMessage(), e);
            return textSearch(query, documentType);
        }
    }
// Recherche sémantique avec fallback sur recherche textuelle
    public List<RAGSearchResult> searchWithFallback(String query) {
        List<RAGSearchResult> semanticResults = search(query);

        if (semanticResults.isEmpty()) {
            logger.debug("Aucun résultat sémantique, fallback sur recherche textuelle");
            return textSearch(query);
        }

        return semanticResults;
    }
// Recherche textuelle simple (fallback)
    public List<RAGSearchResult> textSearch(String query) {
        return textSearch(query, null);
    }

    public List<RAGSearchResult> textSearch(String query, String documentType) {
        List<RAGSearchResult> results = new ArrayList<>();

        try {
            List<KnowledgeDocument> documents;
            if (documentType != null) {
                documents = knowledgeDocumentRepository.findByDocumentType(documentType);
            } else {
                documents = knowledgeDocumentRepository.searchAllByContent(query);
            }

            for (KnowledgeDocument doc : documents) {
                RAGSearchResult result = new RAGSearchResult();
                result.setDocumentId(doc.getId());
                result.setDocumentType(doc.getDocumentType());
                result.setEntityId(doc.getEntityId());
                result.setTitle(doc.getTitle());
                result.setContent(doc.getContent());
                result.setSimilarity(0.5); // Score par défaut pour recherche textuelle
                result.setMetadata(doc.getMetadata());
                results.add(result);
            }

            return results.stream()
                    .limit(topK)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Erreur lors de la recherche textuelle: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
// Génère un contexte enrichi à partir des résultats de recherche
    public String generateContext(List<RAGSearchResult> results) {
        if (results == null || results.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        context.append("Contexte de connaissance:\n");

        for (RAGSearchResult result : results) {
            context.append(String.format("- [%s] %s: %s (similarité: %.2f)\n",
                    result.getDocumentType(),
                    result.getTitle(),
                    result.getContent(),
                    result.getSimilarity()));
        }

        return context.toString();
    }
// Calcule la similarité cosinus entre deux vecteurs
    private double cosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA.length != vectorB.length) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }

        if (normA == 0 || normB == 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
//Résultat de recherche RAG
    public static class RAGSearchResult {
        private String documentId;
        private String documentType;
        private String entityId;
        private String title;
        private String content;
        private double similarity;
        private Map<String, Object> metadata;

        // Getters & Setters
        public String getDocumentId() {
            return documentId;
        }
        public void setDocumentId(String documentId) {
            this.documentId = documentId;
        }
        public String getDocumentType() {
            return documentType;
        }
        public void setDocumentType(String documentType) {
            this.documentType = documentType;
        }
        public String getEntityId() {
            return entityId;
        }
        public void setEntityId(String entityId) {
            this.entityId = entityId;
        }
        public String getTitle() {
            return title;
        }
        public void setTitle(String title) {
            this.title = title;
        }
        public String getContent() {
            return content;
        }
        public void setContent(String content) {
            this.content = content;
        }
        public double getSimilarity() {
            return similarity;
        }
        public void setSimilarity(double similarity) {
            this.similarity = similarity;
        }
        public Map<String, Object> getMetadata() {
            return metadata;
        }
        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
    }
}
