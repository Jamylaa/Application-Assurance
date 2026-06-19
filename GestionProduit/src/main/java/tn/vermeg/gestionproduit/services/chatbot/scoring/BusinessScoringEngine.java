package tn.vermeg.gestionproduit.services.chatbot.scoring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tn.vermeg.gestionproduit.dto.ClientProfile;
import tn.vermeg.gestionproduit.dto.ScoringResult;
import tn.vermeg.gestionproduit.entities.Pack;
import tn.vermeg.gestionproduit.entities.Produit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Service
public class BusinessScoringEngine {
    private static final Logger logger = LoggerFactory.getLogger(BusinessScoringEngine.class);
    // Pondérations par défaut - Améliorées selon l'analyse métier
    private static final double AGE_WEIGHT = 0.20;
    private static final double BUDGET_WEIGHT = 0.15;
    private static final double BENEFICIARY_WEIGHT = 0.10;
    private static final double MEDICAL_RISK_WEIGHT = 0.25;
    private static final double GUARANTEE_MATCH_WEIGHT = 0.20;
    private static final double COVERAGE_LEVEL_WEIGHT = 0.10;
    private static final double GENDER_WEIGHT = 0.05; // Nouvelle pondération pour le genre
    // Score minimum requis pour recommandation
    private static final double MINIMUM_SCORE = 60.0;
    public ScoringResult calculatePackScore(ClientProfile profile, Pack pack) {
        ScoringResult result = new ScoringResult();
        // Calcul des scores par critère
        result.setAgeScore(calculateAgeScore(profile, pack));
        result.setBudgetScore(calculateBudgetScore(profile, pack));
        result.setBeneficiaryScore(calculateBeneficiaryScore(profile, pack));
        result.setMedicalRiskScore(calculateMedicalRiskScore(profile));
        result.setGuaranteeMatchScore(calculateGuaranteeMatchScore(profile, pack));
        result.setCoverageLevelScore(calculateCoverageLevelScore(profile, pack));
        result.setGenderScore(calculateGenderScore(profile, pack));

        // Validation stricte: bloquer si non éligible selon l'âge
        if (result.getAgeScore() == 0.0) {
            result.setGlobalScore(0.0);
            result.setRecommended(false);
            result.setJustification("Non éligible selon l'âge. Ce pack n'est pas recommandé pour ce profil.");
            return result;
        }

        // Validation stricte: bloquer si le prix dépasse significativement le budget
        if (result.getBudgetScore() == 0.0) {
            result.setGlobalScore(0.0);
            result.setRecommended(false);
            result.setJustification("Le prix dépasse largement le budget. Ce pack n'est pas recommandé pour ce profil.");
            return result;
        }

        // Calcul du score global pondéré
        double globalScore = (result.getAgeScore() * AGE_WEIGHT) +
                           (result.getBudgetScore() * BUDGET_WEIGHT) +
                           (result.getBeneficiaryScore() * BENEFICIARY_WEIGHT) +
                           (result.getMedicalRiskScore() * MEDICAL_RISK_WEIGHT) +
                           (result.getGuaranteeMatchScore() * GUARANTEE_MATCH_WEIGHT) +
                           (result.getCoverageLevelScore() * COVERAGE_LEVEL_WEIGHT) +
                           (result.getGenderScore() * GENDER_WEIGHT);

        result.setGlobalScore(Math.min(100, Math.max(0, globalScore)));
        // Déterminer si le pack est recommandé
        result.setRecommended(result.getGlobalScore() >= MINIMUM_SCORE);
        // Définir les pondérations
        Map<String, Double> weights = new HashMap<>();
        weights.put("age", AGE_WEIGHT);
        weights.put("budget", BUDGET_WEIGHT);
        weights.put("beneficiary", BENEFICIARY_WEIGHT);
        weights.put("medicalRisk", MEDICAL_RISK_WEIGHT);
        weights.put("guaranteeMatch", GUARANTEE_MATCH_WEIGHT);
        weights.put("coverageLevel", COVERAGE_LEVEL_WEIGHT);
        weights.put("gender", GENDER_WEIGHT);
        result.setWeights(weights);
        // Générer la justification
        result.setJustification(generateJustification(result, pack));
        return result;
    }
    public ScoringResult calculateProductScore(ClientProfile profile, Produit produit) {
        ScoringResult result = new ScoringResult();
        // Calcul des scores par critère
        result.setAgeScore(calculateProductAgeScore(profile, produit));
        result.setBudgetScore(50.0); // Score par défaut pour les produits
        result.setBeneficiaryScore(50.0);
        result.setMedicalRiskScore(calculateMedicalRiskScore(profile));
        result.setGuaranteeMatchScore(50.0);
        result.setCoverageLevelScore(50.0);
        result.setGenderScore(100.0); // Score neutre pour les produits
        // Calcul du score global pondéré
        double globalScore = (result.getAgeScore() * AGE_WEIGHT) +
                           (result.getBudgetScore() * BUDGET_WEIGHT) +
                           (result.getBeneficiaryScore() * BENEFICIARY_WEIGHT) +
                           (result.getMedicalRiskScore() * MEDICAL_RISK_WEIGHT) +
                           (result.getGuaranteeMatchScore() * GUARANTEE_MATCH_WEIGHT) +
                           (result.getCoverageLevelScore() * COVERAGE_LEVEL_WEIGHT) +
                           (result.getGenderScore() * GENDER_WEIGHT);
        result.setGlobalScore(Math.min(100, Math.max(0, globalScore)));
        result.setRecommended(result.getGlobalScore() >= MINIMUM_SCORE);
        result.setJustification(generateProductJustification(result, produit));
        return result;
    }
    private double calculateAgeScore(ClientProfile profile, Pack pack) {
        if (profile.getAge() == null) return 50.0;
        
        Integer age = profile.getAge();
        Integer ageMin = pack.getAgeMinimum();
        Integer ageMax = pack.getAgeMaximum();
        
        // Si l'âge est hors limites, retourner 0
        if (ageMin != null && age < ageMin) {
            return 0.0;
        }
        if (ageMax != null && age > ageMax) {
            return 0.0;
        }
        
        // Si aucune limite n'est définie, retourner 100
        if (ageMin == null && ageMax == null) {
            return 100.0;
        }
        
        // Score graduel basé sur la position dans la plage d'âge
        double score = 100.0;
        
        if (ageMin != null && ageMax != null) {
            // Plage complète définie
            int range = ageMax - ageMin;
            if (range > 0) {
                // Plus proche du centre = meilleur score
                int midPoint = ageMin + range / 2;
                int distanceFromCenter = Math.abs(age - midPoint);
                // Pénalité proportionnelle à la distance du centre (max 30 points)
                double penalty = (distanceFromCenter / (double) range) * 30.0;
                score = 100.0 - penalty;
            }
        } else if (ageMin != null) {
            // Seulement âge minimum
            int yearsAboveMin = age - ageMin;
            // Pénalité légère pour les clients très âgés par rapport au minimum
            double penalty = Math.min(yearsAboveMin * 0.5, 20.0);
            score = 100.0 - penalty;
        } else if (ageMax != null) {
            // Seulement âge maximum
            int yearsBelowMax = ageMax - age;
            // Pénalité légère pour les clients très jeunes par rapport au maximum
            double penalty = Math.min(yearsBelowMax * 0.5, 20.0);
            score = 100.0 - penalty;
        }
        
        logger.debug("AgeScore: age={}, range=[{},{}], score={}", age, ageMin, ageMax, score);
        return Math.max(0, score);
    }
    private double calculateProductAgeScore(ClientProfile profile, Produit produit) {
        if (profile.getAge() == null) return 50.0;
        return 100.0; // Par défaut pour les produits
    }

    private double calculateBudgetScore(ClientProfile profile, Pack pack) {
        Double monthlyBudget = profile.getMonthlyBudget();
        Double prixMensuel = pack.getPrixMensuel();
        
        if (monthlyBudget == null || prixMensuel == null) {return 50.0;}

        double budget = monthlyBudget;
        double prix = prixMensuel;
        
        // Éviter la division par zéro
        if (budget <= 0.0) {return 50.0;}

        double ratio = prix / budget;

        if (ratio <= 0.7) {
            return 100.0; // Excellent - bien en dessous du budget
        } else if (ratio <= 0.9) {
            return 90.0; // Très bon - confortable dans le budget
        } else if (ratio <= 1.0) {
            return 80.0; // Bon - exactement dans le budget
        } else if (ratio <= 1.15) {
            return 60.0; // Acceptable - légèrement au-dessus
        } else if (ratio <= 1.3) {
            return 30.0; // Pauvre - significativement au-dessus
        } else {
            return 0.0; // Rejet - beaucoup trop cher
        }
    }

    private double calculateBeneficiaryScore(ClientProfile profile, Pack pack) {
        if (pack.getTypeClients() == null || pack.getTypeClients().isEmpty()) {
            return 50.0;}

        // Si le pack est pour famille et le client a des bénéficiaires
        if (profile.getNumberOfBeneficiaries() != null && profile.getNumberOfBeneficiaries() > 0) {
            return pack.getTypeClients().stream()
                    .anyMatch(type -> type.name().contains("FAMILLE")) ? 100.0 : 30.0;
        }
        return 50.0;}

    private double calculateMedicalRiskScore(ClientProfile profile) {
        if (profile.getChronicDiseases() == null || profile.getChronicDiseases().isEmpty()) {
            return 100.0;}

        // Pénalité selon la gravité des maladies - Amélioré
        int severeDiseases = (int) profile.getChronicDiseases().stream()
                .filter(disease -> disease.toLowerCase().contains("cancer") ||
                                 disease.toLowerCase().contains("insuffisance") ||
                                 disease.toLowerCase().contains("sida") ||
                                 disease.toLowerCase().contains("dialyse"))
                .count();

        int moderateDiseases = (int) profile.getChronicDiseases().stream()
                .filter(disease -> disease.toLowerCase().contains("diabète") ||
                                 disease.toLowerCase().contains("diabete") ||
                                 disease.toLowerCase().contains("hypertension") ||
                                 disease.toLowerCase().contains("tension") ||
                                 disease.toLowerCase().contains("cardiaque") ||
                                 disease.toLowerCase().contains("asthme"))
                .count();

        int mildDiseases = (int) profile.getChronicDiseases().stream()
                .filter(disease -> disease.toLowerCase().contains("cholestérol") ||
                                 disease.toLowerCase().contains("cholesterol") ||
                                 disease.toLowerCase().contains("arthrite"))
                .count();

        // Scoring plus granulaire
        if (severeDiseases > 0) {
            return 0.0; // Exclusion totale pour maladies graves
        }
        if (moderateDiseases >= 2) {
            return 15.0; // Pénalité forte pour maladies multiples modérées (réduit de 20 à 15)
        }
        if (moderateDiseases == 1) {
            return 30.0; // Pénalité modérée pour une maladie modérée (réduit de 40 à 30)
        }

        if (mildDiseases > 0) {
            return 80.0; // Pénalité légère pour maladies bénignes (amélioré de 70 à 80)
        }
        return 90.0; // Pénalité par défaut pour autres conditions (amélioré de 85 à 90)
    }
    private double calculateGuaranteeMatchScore(ClientProfile profile, Pack pack) {
        // Si le client n'a pas spécifié de garanties/domaines, retourner un score neutre
        if ((profile.getDesiredGuarantees() == null || profile.getDesiredGuarantees().isEmpty()) &&
            (profile.getDesiredMedicalDomains() == null || profile.getDesiredMedicalDomains().isEmpty())) {
            return 70.0; // Score neutre si aucune préférence
        }

        // Récupérer les domaines médicaux du pack
        List<String> packDomains = pack.getDomainesMedicaux();
        if (packDomains == null || packDomains.isEmpty()) {
            return 50.0; // Pénalité si le pack n'a pas de domaines médicaux définis
        }

        // Calculer le score basé sur les correspondances
        double score = 0.0;
        int totalCriteria = 0;
        int matchedCriteria = 0;

        // Vérifier les garanties souhaitées
        if (profile.getDesiredGuarantees() != null && !profile.getDesiredGuarantees().isEmpty()) {
            totalCriteria += profile.getDesiredGuarantees().size();
            for (String desiredGuarantee : profile.getDesiredGuarantees()) {
                String lowerDesired = desiredGuarantee.toLowerCase();
                for (String packDomain : packDomains) {
                    String lowerPackDomain = packDomain.toLowerCase();
                    // Correspondance exacte ou partielle
                    if (lowerPackDomain.contains(lowerDesired) || lowerDesired.contains(lowerPackDomain)) {
                        matchedCriteria++;
                        break;
                    }
                }
            }
        }

        // Vérifier les domaines médicaux souhaités
        if (profile.getDesiredMedicalDomains() != null && !profile.getDesiredMedicalDomains().isEmpty()) {
            totalCriteria += profile.getDesiredMedicalDomains().size();
            for (String desiredDomain : profile.getDesiredMedicalDomains()) {
                String lowerDesired = desiredDomain.toLowerCase();
                for (String packDomain : packDomains) {
                    String lowerPackDomain = packDomain.toLowerCase();
                    // Correspondance exacte ou partielle
                    if (lowerPackDomain.contains(lowerDesired) || lowerDesired.contains(lowerPackDomain)) {
                        matchedCriteria++;
                        break;
                    }
                }
            }
        }

        // Calculer le score de correspondance
        if (totalCriteria > 0) {
            double matchRatio = (double) matchedCriteria / totalCriteria;
            // Score entre 0 et 100 basé sur le ratio de correspondance
            score = matchRatio * 100.0;
        } else {
            score = 70.0; // Score neutre si aucun critère
        }

        logger.debug("GuaranteeMatchScore: {}/{} correspondances, score: {}", matchedCriteria, totalCriteria, score);
        return score;
    }

    private double calculateCoverageLevelScore(ClientProfile profile, Pack pack) {
        if (profile.getCoverageLevel() == null || pack.getNiveauCouverture() == null) {return 50.0;}

        String clientLevel = profile.getCoverageLevel().toUpperCase();
        String packLevel = pack.getNiveauCouverture().name().toUpperCase();

        if (clientLevel.equals(packLevel)) {return 100.0;}
        else if (isHigherLevel(packLevel, clientLevel)) {return 90.0;} else {return 40.0;}
    }

    private double calculateGenderScore(ClientProfile profile, Pack pack) {
        // Si le genre n'est pas spécifié, retourner un score neutre
        if (profile.getGender() == null || profile.getGender().trim().isEmpty()) {
            return 100.0; // Score neutre si aucune préférence de genre
        }

        String gender = profile.getGender().toLowerCase();
        
        // Vérifier si le pack a des types de clients spécifiques qui peuvent être liés au genre
        if (pack.getTypeClients() != null && !pack.getTypeClients().isEmpty()) {
            // Certains packs peuvent être spécifiquement conçus pour certains profils
            // Par exemple, les packs "MATERNITE" sont plus adaptés aux femmes
            String packNameLower = pack.getNomPack() != null ? pack.getNomPack().toLowerCase() : "";
            
            // Bonus pour les packs spécifiquement adaptés au genre
            if (gender.contains("femme") || gender.contains("féminin") || gender.contains("female")) {
                if (packNameLower.contains("maternité") || packNameLower.contains("maternite") || 
                    packNameLower.contains("gynécologie") || packNameLower.contains("gynecologie")) {
                    return 100.0;
                }
            } else if (gender.contains("homme") || gender.contains("masculin") || gender.contains("male")) {
                // Packs spécifiques pour hommes (moins courants mais possibles)
                if (packNameLower.contains("prostate") || packNameLower.contains("andrologie")) {
                    return 100.0;
                }
            }
        }
        
        // Score neutre par défaut - la plupart des packs sont unisexes
        return 100.0;
    }

    private boolean isHigherLevel(String level1, String level2) {
        String[] levels = {"BASIC", "PREMIUM", "GOLD"};
        int index1 = -1, index2 = -1;
        for (int i = 0; i < levels.length; i++) {
            if (levels[i].equals(level1)) index1 = i;
            if (levels[i].equals(level2)) index2 = i;}
        return index1 > index2;
    }
    private String generateJustification(ScoringResult result, Pack pack) {
        StringBuilder justification = new StringBuilder();
        justification.append("Score global: ").append(String.format("%.1f", result.getGlobalScore())).append("/100. ");
        if (result.getBudgetScore() < 50) {justification.append("Le prix dépasse le budget. ");}
        if (result.getAgeScore() == 0) {justification.append("Non éligible selon l'âge. ");}
        if (result.isRecommended()) {justification.append("Pack recommandé pour ce profil. ");} else {justification.append("Pack non recommandé. ");}
        return justification.toString();
    }
    private String generateProductJustification(ScoringResult result, Produit produit) {
        StringBuilder justification = new StringBuilder();
        justification.append("Score global: ").append(String.format("%.1f", result.getGlobalScore())).append("/100. ");
        justification.append("Produit ").append(produit.getNomProduit()).append(". ");
        if (result.isRecommended()) {justification.append("Produit recommandé. ");} else {justification.append("Produit non recommandé. ");}
        return justification.toString();
    }
}