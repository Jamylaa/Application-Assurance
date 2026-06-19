package tn.vermeg.gestionproduit.services.chatbot.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tn.vermeg.gestionproduit.entities.Garantie;
import tn.vermeg.gestionproduit.entities.KnowledgeDocument;
import tn.vermeg.gestionproduit.entities.Pack;
import tn.vermeg.gestionproduit.entities.Produit;
import tn.vermeg.gestionproduit.repositories.GarantieRepository;
import tn.vermeg.gestionproduit.repositories.KnowledgeDocumentRepository;
import tn.vermeg.gestionproduit.repositories.PackUnifiedRepository;
import tn.vermeg.gestionproduit.repositories.ProduitRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VectorEmbeddingService {

    private static final Logger logger = LoggerFactory.getLogger(VectorEmbeddingService.class);

    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final PackUnifiedRepository packRepository;
    private final ProduitRepository produitRepository;
    private final GarantieRepository garantieRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${rag.embedding.enabled:true}")
    private boolean embeddingEnabled;

    @Value("${google.api.key:}")
    private String googleApiKey;

    @Value("${spring.ai.vertex.ai.embedding.model:embedding-001}")
    private String embeddingModel;

    private static final String GEMINI_EMBEDDING_URL = "https://generativelanguage.googleapis.com/v1beta/models/%s:embedContent?key=%s";

    public VectorEmbeddingService(
            KnowledgeDocumentRepository knowledgeDocumentRepository,
            PackUnifiedRepository packRepository,
            ProduitRepository produitRepository,
            GarantieRepository garantieRepository) {
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
        this.packRepository = packRepository;
        this.produitRepository = produitRepository;
        this.garantieRepository = garantieRepository;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public void indexAllKnowledge() {
        if (!embeddingEnabled) {
            logger.info("Embedding désactivé, indexation ignorée");
            return;}

        logger.info("Début de l'indexation des connaissances");
        try {
            // Indexer les produits
            indexProduits();
            // Indexer les packs
            indexPacks();
            // Indexer les garanties
            indexGaranties();
            logger.info("Indexation des connaissances terminée avec succès");
        } catch (Exception e) {
            logger.error("Erreur lors de l'indexation des connaissances: {}", e.getMessage(), e);
        }
    }
    public float[] generateEmbedding(String text) {
        if (!embeddingEnabled || text == null || text.isBlank()) {
            if (!embeddingEnabled) {
                logger.debug("Embedding désactivé via configuration (rag.embedding.enabled=false)");
            }
            return new float[0];
        }
        if (googleApiKey == null || googleApiKey.isBlank()) {
            logger.error("Google API Key non configurée. Veuillez définir la variable d'environnement GOOGLE_API_KEY");
            return new float[0];
        }
        try {
            logger.info("Génération d'embedding Google Gemini pour texte: {}", text.substring(0, Math.min(100, text.length())));
            String url = String.format(GEMINI_EMBEDDING_URL, embeddingModel, googleApiKey);
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> content = new HashMap<>();
            List<Map<String, String>> parts = new ArrayList<>();
            Map<String, String> textPart = new HashMap<>();
            textPart.put("text", text);
            parts.add(textPart);
            content.put("parts", parts);
            requestBody.put("content", content);
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode embeddingNode = root.path("embedding").path("values");
            
            float[] embeddingArray = new float[embeddingNode.size()];
            for (int i = 0; i < embeddingNode.size(); i++) {
                embeddingArray[i] = (float) embeddingNode.get(i).asDouble();
            }
            
            logger.info("Embedding généré avec succès via Google Gemini, taille: {}", embeddingArray.length);
            return embeddingArray;
        } catch (Exception e) {
            logger.error("Erreur critique lors de la génération de l'embedding Google Gemini: {}", e.getMessage(), e);
            logger.warn("Fallback: retour d'un embedding vide - le RAG fonctionnera sans sémantique");
            return new float[0];
        }
    }
    private void indexProduits() {
        List<Produit> produits = produitRepository.findAll();
        logger.info("Indexation de {} produits", produits.size());

        for (Produit produit : produits) {
            try {
                String content = buildProduitContent(produit);
                float[] embedding = generateEmbedding(content);

                KnowledgeDocument doc = new KnowledgeDocument();
                doc.setDocumentType("PRODUIT");
                doc.setEntityId(produit.getIdProduit());
                doc.setTitle(produit.getNomProduit());
                doc.setContent(content);
                doc.setEmbedding(embedding);

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("typeProduit", produit.getTypeProduit() != null ? produit.getTypeProduit().name() : null);
                metadata.put("statut", produit.getStatut() != null ? produit.getStatut().name() : null);
                doc.setMetadata(metadata);

                knowledgeDocumentRepository.save(doc);
            } catch (Exception e) {
                logger.warn("Erreur lors de l'indexation du produit {}: {}", produit.getIdProduit(), e.getMessage());
            }
        }
    }
    private void indexPacks() {
        List<Pack> packs = packRepository.findAll();
        logger.info("Indexation de {} packs", packs.size());

        for (Pack pack : packs) {
            try {
                String content = buildPackContent(pack);
                float[] embedding = generateEmbedding(content);

                KnowledgeDocument doc = new KnowledgeDocument();
                doc.setDocumentType("PACK");
                doc.setEntityId(pack.getIdPack());
                doc.setTitle(pack.getNomPack());
                doc.setContent(content);
                doc.setEmbedding(embedding);

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("prixMensuel", pack.getPrixMensuel());
                metadata.put("niveauCouverture", pack.getNiveauCouverture() != null ? pack.getNiveauCouverture().name() : null);
                metadata.put("typeClients", pack.getTypeClients());
                metadata.put("statut", pack.getStatut() != null ? pack.getStatut().name() : null);
                doc.setMetadata(metadata);

                knowledgeDocumentRepository.save(doc);
            } catch (Exception e) {logger.warn("Erreur lors de l'indexation du pack {}: {}", pack.getIdPack(), e.getMessage());
            }
        }
    }

    private void indexGaranties() {
        List<Garantie> garanties = garantieRepository.findAll();
        logger.info("Indexation de {} garanties", garanties.size());
        for (Garantie garantie : garanties) {
            try {String content = buildGarantieContent(garantie);
                float[] embedding = generateEmbedding(content);

                KnowledgeDocument doc = new KnowledgeDocument();
                doc.setDocumentType("GARANTIE");
                doc.setEntityId(garantie.getIdGarantie());
                doc.setTitle(garantie.getNomGarantie());
                doc.setContent(content);
                doc.setEmbedding(embedding);

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("type", garantie.getDomaine());
                metadata.put("tauxRemboursement", garantie.getTauxRemboursement());
                metadata.put("statut", garantie.getStatut() != null ? garantie.getStatut().name() : null);
                doc.setMetadata(metadata);

                knowledgeDocumentRepository.save(doc);
            } catch (Exception e) {logger.warn("Erreur lors de l'indexation de la garantie {}: {}", garantie.getIdGarantie(), e.getMessage());
            }
        }
    }
    private String buildProduitContent(Produit produit) {
        StringBuilder content = new StringBuilder();
        content.append("Produit d'assurance: ").append(produit.getNomProduit()).append(". ");
        if (produit.getDescription() != null) {
            content.append("Description: ").append(produit.getDescription()).append(". ");}
        if (produit.getTypeProduit() != null) {
            content.append("Type: ").append(produit.getTypeProduit().name()).append(". ");}
        return content.toString();
    }

    private String buildPackContent(Pack pack) {
        StringBuilder content = new StringBuilder();
        content.append("Pack d'assurance: ").append(pack.getNomPack()).append(". ");
        
        if (pack.getDescription() != null) {content.append("Description: ").append(pack.getDescription()).append(". ");}
        
        if (pack.getNiveauCouverture() != null) {
            content.append("Niveau de couverture: ").append(pack.getNiveauCouverture().name()).append(". ");}
        
        if (pack.getPrixMensuel() > 0) {
            content.append("Prix mensuel: ").append(pack.getPrixMensuel()).append(" euros. ");}
        
        if (pack.getTypeClients() != null && !pack.getTypeClients().isEmpty()) {
            content.append("Types de clients: ");
            for (Object type : pack.getTypeClients()) {content.append(type.toString()).append(" ");}
        }
        
        return content.toString();
    }
    private String buildGarantieContent(Garantie garantie) {
        StringBuilder content = new StringBuilder();
        content.append("Garantie: ").append(garantie.getNomGarantie()).append(". ");
        
        if (garantie.getDescription() != null) {
            content.append("Description: ").append(garantie.getDescription()).append(". ");}
        
        if (garantie.getDomaine() != null) {
            content.append(garantie.getDomaine()).append(". ");}
        
        if (garantie.getTauxRemboursement() > 0) {
            content.append("Taux de remboursement: ").append(garantie.getTauxRemboursement() * 100).append("%. ");
        }
        
        return content.toString();
    }
    public void reindexEntity(String documentType, String entityId) {
        if (!embeddingEnabled) {return;}

        try {logger.warn("Méthode deleteByDocumentTypeAndEntityId temporairement désactivée");
            
            // Créer le nouveau document
            switch (documentType) {
                case "PRODUIT":
                    produitRepository.findById(entityId).ifPresent(produit -> {
                        String content = buildProduitContent(produit);
                        float[] embedding = generateEmbedding(content);
                        KnowledgeDocument doc = new KnowledgeDocument("PRODUIT", entityId, produit.getNomProduit(), content);
                        doc.setEmbedding(embedding);
                        knowledgeDocumentRepository.save(doc);
                    });
                    break;
                case "PACK":
                    packRepository.findById(entityId).ifPresent(pack -> {
                        String content = buildPackContent(pack);
                        float[] embedding = generateEmbedding(content);
                        KnowledgeDocument doc = new KnowledgeDocument("PACK", entityId, pack.getNomPack(), content);
                        doc.setEmbedding(embedding);
                        knowledgeDocumentRepository.save(doc);
                    });
                    break;
                case "GARANTIE":
                    garantieRepository.findById(entityId).ifPresent(garantie -> {
                        String content = buildGarantieContent(garantie);
                        float[] embedding = generateEmbedding(content);
                        KnowledgeDocument doc = new KnowledgeDocument("GARANTIE", entityId, garantie.getNomGarantie(), content);
                        doc.setEmbedding(embedding);
                        knowledgeDocumentRepository.save(doc);
                    });
                    break;
            }
            logger.info("Entité réindexée: {} - {}", documentType, entityId);
        } catch (Exception e) {logger.error("Erreur lors de la réindexation: {}", e.getMessage(), e);}
    }
}