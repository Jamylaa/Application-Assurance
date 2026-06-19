package tn.vermeg.gestionproduit.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import tn.vermeg.gestionproduit.entities.ConversationMemory;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationMemoryRepository extends MongoRepository<ConversationMemory, String> {

    Optional<ConversationMemory> findByUserId(String userId);
    Optional<ConversationMemory> findBySessionId(String sessionId);
    List<ConversationMemory> findByUserIdOrderByUpdatedAtDesc(String userId);
    @Query("{ 'userId': ?0, 'updatedAt': { $gte: ?1 } }")
    List<ConversationMemory> findByUserIdAndUpdateDateAfter(String userId, Instant date);
    @Query(value = "{ 'userId': ?0, 'messageHistory': { $elemMatch: { 'action': ?1 } } }", sort = "{ 'updatedAt': -1 }")
    List<ConversationMemory> findByUserIdAndAction(String userId, String action);
    @Query(value = "{ 'userId': ?0, 'customerProfile.medicalNeeds': { $in: ?1 } }")
    List<ConversationMemory> findByUserIdAndMedicalNeeds(String userId, List<String> medicalNeeds);
    void deleteBySessionId(String sessionId);
    @Query("{ 'updatedAt': { $lt: ?0 } }")
    void deleteOlderThan(Instant date);
}