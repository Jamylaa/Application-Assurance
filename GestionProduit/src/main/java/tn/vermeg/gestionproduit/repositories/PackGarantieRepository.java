package tn.vermeg.gestionproduit.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tn.vermeg.gestionproduit.entities.PackGarantie;

import java.util.List;

@Repository
public interface PackGarantieRepository extends MongoRepository<PackGarantie, String> {

    List<PackGarantie> findByPackId(String packId);
    List<PackGarantie> findByGarantieId(String garantieId);
    List<PackGarantie> findByPackIdAndOptionnelle(String packId, boolean optionnelle);
    boolean existsByPackIdAndGarantieId(String packId, String garantieId);
    void deleteByPackId(String packId);
    void deleteByGarantieId(String garantieId);
}