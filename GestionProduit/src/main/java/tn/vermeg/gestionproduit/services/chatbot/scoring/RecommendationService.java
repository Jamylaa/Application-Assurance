package tn.vermeg.gestionproduit.services.chatbot.scoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tn.vermeg.gestionproduit.dto.ClientProfile;
import tn.vermeg.gestionproduit.dto.RecommendationRequestDTO;
import tn.vermeg.gestionproduit.dto.RecommendationResponseDTO;
import tn.vermeg.gestionproduit.dto.RecommendationResultDTO;
import tn.vermeg.gestionproduit.dto.ScoringResult;
import tn.vermeg.gestionproduit.entities.Pack;
import tn.vermeg.gestionproduit.repositories.PackUnifiedRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationService.class);

    private final BusinessScoringEngine businessScoringEngine;
    private final PackUnifiedRepository packRepository;

    public RecommendationService(BusinessScoringEngine businessScoringEngine, 
                                 PackUnifiedRepository packRepository) {
        this.businessScoringEngine = businessScoringEngine;
        this.packRepository = packRepository;
    }
    public RecommendationResponseDTO generateRecommendations(RecommendationRequestDTO request) {
        logger.info("Génération de recommandations pour le profil: {}", request.getSessionId());

        RecommendationResponseDTO response = new RecommendationResponseDTO();
        response.setSessionId(request.getSessionId());
        
        try {ClientProfile profile = convertToClientProfile(request);
            // Récupérer tous les packs actifs
            List<Pack> allPacks = packRepository.findAll().stream()
                    .filter(pack -> pack.getStatut() != null && pack.getStatut().name().equals("ACTIF"))
                    .collect(Collectors.toList());
            // Calculer les scores pour chaque pack
            List<RecommendationResultDTO> recommendedPacks = new ArrayList<>();
            for (Pack pack : allPacks) {
                ScoringResult scoringResult = businessScoringEngine.calculatePackScore(profile, pack);
                
                if (scoringResult.isRecommended()) {
                    RecommendationResultDTO resultDTO = new RecommendationResultDTO();
                    resultDTO.setId(pack.getIdPack());
                    resultDTO.setNom(pack.getNomPack());
                    resultDTO.setDescription(pack.getDescription());
                    resultDTO.setCompatibilityScore(scoringResult.getGlobalScore());
                    resultDTO.setScoringResult(scoringResult);
                    resultDTO.setWhyRecommended(scoringResult.getJustification());
                    resultDTO.setMonthlyPrice(pack.getPrixMensuel());
                    resultDTO.setCoverageLevel(pack.getNiveauCouverture() != null ? pack.getNiveauCouverture().name() : "BASIC");
                    recommendedPacks.add(resultDTO);
                }
            }
            // Trier par score décroissant
            recommendedPacks.sort((a, b) -> Double.compare(b.getCompatibilityScore(), a.getCompatibilityScore()));
            // Limiter aux 3 meilleurs packs
            List<RecommendationResultDTO> topPacks = recommendedPacks.stream()
                    .limit(3)
                    .collect(Collectors.toList());
            response.setRecommendedPacks(topPacks);
            response.setRecommendedProducts(new ArrayList<>());
            response.setSuccess(true);
            if (topPacks.isEmpty()) {response.setExplanation("Aucun pack ne correspond à votre profil. Essayez d'ajuster votre budget ou vos critères.");
                response.setMessage("Aucune recommandation disponible");
            } else {response.setExplanation("Basé sur votre profil de " + profile.getAge() + " ans, " +
                                       (profile.getChronicDiseases() != null && !profile.getChronicDiseases().isEmpty() ? 
                                        "avec conditions médicales" : "sans conditions médicales") + 
                                       ", voici les packs les plus adaptés.");
                response.setMessage("Recommandations générées avec succès");
            }
        } catch (Exception e) {logger.error("Erreur lors de la génération des recommandations", e);
            response.setSuccess(false);
            response.setMessage("Erreur lors de la génération des recommandations: " + e.getMessage());
            response.setExplanation("Une erreur est survenue lors du traitement de votre demande.");
        }
        return response;
    }
    
    private ClientProfile convertToClientProfile(RecommendationRequestDTO request) {
        ClientProfile profile = new ClientProfile();
        profile.setAge(request.getAge());
        profile.setGender(request.getGender());
        profile.setMaritalStatus(request.getMaritalStatus());
        profile.setNumberOfBeneficiaries(request.getNumberOfChildren() != null ? request.getNumberOfChildren() : 1);
        profile.setMonthlyBudget(request.getMonthlyBudget());
        profile.setChronicDiseases(request.getChronicDiseases());
        
        // Définir le niveau de couverture par défaut
        if (request.getMonthlyBudget() != null) {
            if (request.getMonthlyBudget() < 50) {profile.setCoverageLevel("BASIC");
            } else if (request.getMonthlyBudget() < 100) {profile.setCoverageLevel("BASIC");
            } else {profile.setCoverageLevel("PREMIUM");}
        }
        return profile;
    }
}