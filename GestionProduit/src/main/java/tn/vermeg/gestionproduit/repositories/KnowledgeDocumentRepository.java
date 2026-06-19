package tn.vermeg.gestionproduit.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import tn.vermeg.gestionproduit.entities.KnowledgeDocument;

import java.util.List;

@Repository
public interface KnowledgeDocumentRepository extends MongoRepository<KnowledgeDocument, String> {

    List<KnowledgeDocument> findByDocumentType(String documentType);
    List<KnowledgeDocument> findByEntityId(String entityId);
    List<KnowledgeDocument> findByDocumentTypeAndEntityId(String documentType, String entityId);
    @Query("{ 'documentType': ?0, 'content': { $regex: ?1, $options: 'i' } }")
    List<KnowledgeDocument> searchByContent(String documentType, String searchTerm);

    @Query("{ 'content': { $regex: ?0, $options: 'i' } }")
    List<KnowledgeDocument> searchAllByContent(String searchTerm);

    void deleteByEntityId(String entityId);

    void deleteByDocumentType(String documentType);
}
