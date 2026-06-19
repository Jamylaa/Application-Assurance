package tn.vermeg.gestionproduit.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.Map;

@Document(collection = "knowledge_documents")
public class KnowledgeDocument {

    @Id
    private String id;

    @Indexed
    private String documentType; // "PRODUIT", "PACK", "GARANTIE", "REGLE_METIER", "DOCUMENT_ASSURANCE"
    @Indexed
    private String entityId; // ID de l'entité associée (produitId, packId, garantieId, etc.)
    private String title;
    private String content;
    private Map<String, Object> metadata;

    // Pour MongoDB Vector Search
    private float[] embedding;
    @CreatedDate
    private Instant createdAt;
    // Constructeurs
    public KnowledgeDocument() {
        this.createdAt = Instant.now();
    }
    public KnowledgeDocument(String documentType, String entityId, String title, String content) {
        this.documentType = documentType;
        this.entityId = entityId;
        this.title = title;
        this.content = content;
        this.createdAt = Instant.now();
    }

    // Getters & Setters
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
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
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    public float[] getEmbedding() {
        return embedding;
    }
    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
    }
    public Instant getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}