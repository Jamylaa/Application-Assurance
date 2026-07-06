package tn.vermeg.gestionproduit.services;

import org.springframework.stereotype.Service;
import tn.vermeg.gestionproduit.entities.CritereRecommandation;
import tn.vermeg.gestionproduit.exceptions.ResourceNotFoundException;
import tn.vermeg.gestionproduit.repositories.CritereRecommandationRepository;

import java.util.List;
import java.util.Map;

@Service
public class CritereRecommandationService {

    private final CritereRecommandationRepository critereRecommandationRepository;

    public CritereRecommandationService(CritereRecommandationRepository critereRecommandationRepository) {
        this.critereRecommandationRepository = critereRecommandationRepository;
    }

    public List<CritereRecommandation> getAllCriteres() {
        return critereRecommandationRepository.findAll();
    }

    public CritereRecommandation getCritereById(String idCritere) {
        return critereRecommandationRepository.findById(idCritere)
                .orElseThrow(() -> new ResourceNotFoundException("CritereRecommandation", idCritere,
                        "Critère de recommandation non trouvé avec l'ID: " + idCritere));
    }

    public List<CritereRecommandation> getCriteresByProduitId(String produitId) {
        return critereRecommandationRepository.findByProduitId(produitId);
    }

    public CritereRecommandation getCritereByPackId(String packId) {
        return critereRecommandationRepository.findByPackId(packId)
                .orElseThrow(() -> new ResourceNotFoundException("CritereRecommandation", packId,
                        "Aucun critère de recommandation configuré pour le pack: " + packId));
    }

    public List<CritereRecommandation> getCriteresActifs() {
        return critereRecommandationRepository.findByActifTrue();
    }

    public List<CritereRecommandation> searchByMotsCles(List<String> termes) {
        return critereRecommandationRepository.findByMotsClesOrSynonymesIn(termes);
    }

    public CritereRecommandation createCritere(CritereRecommandation critere) {
        validateCritere(critere);

        if (critere.getPackId() != null && !critere.getPackId().isBlank()
                && critereRecommandationRepository.existsByPackId(critere.getPackId())) {
            throw new IllegalArgumentException("Un critère de recommandation existe déjà pour ce pack: " + critere.getPackId());
        }

        critere.setActif(true);
        return critereRecommandationRepository.save(critere);
    }

    public CritereRecommandation updateCritere(String idCritere, CritereRecommandation details) {
        CritereRecommandation critere = getCritereById(idCritere);
        validateCritere(details);

        critere.setMotsCles(details.getMotsCles());
        critere.setSynonymes(details.getSynonymes());
        critere.setCasUsages(details.getCasUsages());
        critere.setProfilsRecommandes(details.getProfilsRecommandes());
        critere.setScoreBase(details.getScoreBase());
        critere.setPoidsAttributs(details.getPoidsAttributs());
        critere.setBonusConditionnels(details.getBonusConditionnels());
        critere.setReglesDisqualification(details.getReglesDisqualification());
        critere.setPrioriteAffichage(details.getPrioriteAffichage());

        return critereRecommandationRepository.save(critere);
    }

    public void deleteCritere(String idCritere) {
        if (!critereRecommandationRepository.existsById(idCritere)) {
            throw new ResourceNotFoundException("CritereRecommandation", idCritere,
                    "Critère de recommandation non trouvé avec l'ID: " + idCritere);
        }
        critereRecommandationRepository.deleteById(idCritere);
    }

    public double calculerScore(String idCritere, Map<String, Double> attributsProfil) {
        return getCritereById(idCritere).calculerScore(attributsProfil);
    }

    private void validateCritere(CritereRecommandation critere) {
        if (critere.getProduitId() == null || critere.getProduitId().isBlank()) {
            throw new IllegalArgumentException("L'identifiant du produit est obligatoire.");
        }
        if (critere.getScoreBase() < 0 || critere.getScoreBase() > 100) {
            throw new IllegalArgumentException("Le score de base doit être compris entre 0 et 100.");
        }
    }
}
