package tn.vermeg.gestionproduit.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tn.vermeg.gestionproduit.entities.Pack;
import tn.vermeg.gestionproduit.entities.Produit;
import tn.vermeg.gestionproduit.entities.PackGarantie;
import tn.vermeg.gestionproduit.entities.NiveauCouverture;
import tn.vermeg.gestionproduit.entities.CouvertureGeographique;
import tn.vermeg.gestionproduit.entities.Statut;
import tn.vermeg.gestionproduit.repositories.PackUnifiedRepository;
import tn.vermeg.gestionproduit.repositories.ProduitRepository;
import tn.vermeg.gestionproduit.repositories.PackGarantieRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScoringService {

    private final PackUnifiedRepository packRepository;
    private final PackGarantieRepository packGarantieRepository;
    private final ProduitRepository produitRepository;

    @Value("${scoring.weight.age:0.25}")
    private double ageWeight;

    @Value("${scoring.weight.budget:0.25}")
    private double budgetWeight;

    @Value("${scoring.weight.guarantees:0.20}")
    private double guaranteesWeight;

    @Value("${scoring.weight.coverage:0.10}")
    private double coverageWeight;

    @Value("${scoring.weight.geographic:0.10}")
    private double geographicWeight;

    @Value("${scoring.weight.clientType:0.10}")
    private double clientTypeWeight;

    @Value("${scoring.weight.productType:0.10}")
    private double productTypeWeight;

    @Value("${scoring.weight.medical:0.10}")
    private double medicalWeight;

    @Value("${scoring.weight.beneficiaries:0.05}")
    private double beneficiariesWeight;

    public ScoringService(PackUnifiedRepository packRepository,
                          PackGarantieRepository packGarantieRepository,
                          ProduitRepository produitRepository) {
        this.packRepository = packRepository;
        this.packGarantieRepository = packGarantieRepository;
        this.produitRepository = produitRepository;
    }

    public List<RecommendationDTO> recommendPacks(ClientProfile profile) {
        List<Pack> packs = packRepository.findAll();

        List<RecommendationDTO> scored = new ArrayList<>();

        for (Pack p : packs) {
            if (p.getStatut() != Statut.ACTIF) continue;

            double score = 0.0;
            List<String> reasons = new ArrayList<>();

            // Age compatibility (weight: 0.25)
            Integer min = p.getAgeMinimum();
            Integer max = p.getAgeMaximum();
            if (min != null && max != null) {
                if (profile.age() >= min && profile.age() <= max) {
                    score += 25.0;
                    reasons.add("Âge compatible (+25)");
                } else if (profile.age() < min && (min - profile.age()) <= 5) {
                    score += 10.0;
                    reasons.add("Âge proche du minimum (+10)");
                } else if (profile.age() > max && (profile.age() - max) <= 5) {
                    score += 10.0;
                    reasons.add("Âge proche du maximum (+10)");
                }
            }

            // Budget fit (weight: 0.25)
            double price = p.getPrixMensuel();
            if (price > 0) {
                double budgetRatio = profile.budget() / price;
                if (budgetRatio >= 1.0) {
                    score += 25.0;
                    reasons.add("Budget respecté (+25)");
                    if (budgetRatio >= 1.5) {
                        score += 10.0;
                        reasons.add("Budget confortable (+10)");
                    }
                } else if (budgetRatio >= 0.8) {
                    score += 15.0;
                    reasons.add("Budget légèrement dépassé mais acceptable (+15)");
                }
            }

            // Guarantees match with fuzzy matching (weight: 0.20)
            List<PackGarantie> garanties = packGarantieRepository.findByPackId(p.getIdPack());
            Set<String> packGarantiesNames = garanties.stream()
                    .filter(Objects::nonNull)
                    .map(PackGarantie::getNomGarantie)
                    .filter(Objects::nonNull)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());

            int matches = 0;
            int partialMatches = 0;
            for (String wanted : profile.garantiesVoulu()) {
                if (wanted == null) continue;
                String wantedLower = wanted.toLowerCase();
                
                // Exact match
                if (packGarantiesNames.contains(wantedLower)) {
                    score += 8.0;
                    matches++;
                } 
                // Partial/fuzzy match
                else {
                    for (String packGarantie : packGarantiesNames) {
                        if (wantedLower.contains(packGarantie) || packGarantie.contains(wantedLower)) {
                            score += 4.0;
                            partialMatches++;
                            break;
                        }
                    }
                }
            }
            if (matches > 0) reasons.add("Garanties correspondantes: " + matches + " (+" + (matches * 8) + ")");
            if (partialMatches > 0) reasons.add("Correspondances partielles: " + partialMatches + " (+" + (partialMatches * 4) + ")");

            // Coverage level (weight: 0.10)
            NiveauCouverture niv = p.getNiveauCouverture();
            if (niv != null) {
                double add = switch (niv) {
                    case PREMIUM -> 15.0;
                    case GOLD -> 12.0;
                    case BASIC -> 5.0;
                    default -> 3.0;
                };
                score += add;
                reasons.add("Niveau de couverture " + niv + " (+" + add + ")");
            }

            // Geographic coverage (weight: 0.10)
            CouvertureGeographique cov = p.getCouvertureGeographique();
            if (cov != null && profile.couvertureGeographique() != null) {
                try {
                    CouvertureGeographique clientCov = CouvertureGeographique.valueOf(profile.couvertureGeographique().toUpperCase());
                    if (cov == clientCov) {
                        score += 10.0;
                        reasons.add("Couverture géographique correspondante (+10)");
                    } else if (cov == CouvertureGeographique.INTERNATIONAL && clientCov != CouvertureGeographique.LOCAL) {
                        score += 5.0;
                        reasons.add("Couverture internationale supérieure (+5)");
                    }
                } catch (Exception ignored) { }
            }

            // Client type matching (weight: 0.10)
            if (p.getTypeClients() != null && !p.getTypeClients().isEmpty()) {
                for (var clientType : p.getTypeClients()) {
                    if (profile.sexe() != null && !profile.sexe().isBlank()) {
                        if (clientType.name().equalsIgnoreCase(profile.sexe()) ||
                            (clientType.name().equals("FAMILLE") && profile.nombreBeneficiaires() > 1) ||
                            (clientType.name().equals("SENIOR") && profile.age() >= 60)) {
                            score += 10.0;
                            reasons.add("Type de client adapté: " + clientType + " (+10)");
                            break;
                        }
                    }
                }
            }

            // Product type preference bonus (weight: 0.10)
            String preferredType = profile.produitTypePreferee();
            if (preferredType != null && !preferredType.isBlank() && p.getProduitId() != null) {
                var matchedProduit = produitRepository.findById(p.getProduitId());
                if (matchedProduit.isPresent()) {
                    var prod = matchedProduit.get();
                    if (prod.getTypeProduit() != null && prod.getTypeProduit().name().equalsIgnoreCase(preferredType)) {
                        score += 10.0;
                        reasons.add("Produit associé correspond au type demandé (+10)");
                    }
                }
            }

            // Medical risk bonus — maladies chroniques (weight: 0.10)
            if (profile.maladiesChroniques() != null && !profile.maladiesChroniques().isEmpty()) {
                Set<String> maladiesLower = profile.maladiesChroniques().stream()
                        .filter(Objects::nonNull)
                        .map(String::toLowerCase)
                        .collect(Collectors.toSet());
                boolean hasHighRiskDisease = maladiesLower.stream().anyMatch(m ->
                        m.contains("diab") || m.contains("cancer") || m.contains("cardio") ||
                        m.contains("hypertension") || m.contains("insuffisance") || m.contains("oncologie"));
                if (hasHighRiskDisease) {
                    if (niv == NiveauCouverture.PREMIUM || niv == NiveauCouverture.GOLD) {
                        score += 10.0;
                        reasons.add("Couverture adaptée aux risques médicaux élevés (+10)");
                    }
                } else if (!maladiesLower.isEmpty()) {
                    score += 5.0;
                    reasons.add("Risque médical modéré pris en compte (+5)");
                }
            }

            // Beneficiaries bonus (weight: 0.05)
            if (profile.nombreBeneficiaires() > 1 && p.getTypeClients() != null) {
                boolean supportsFamille = p.getTypeClients().stream()
                        .anyMatch(t -> t.name().equals("FAMILLE") || t.name().equals("ENTREPRISE"));
                if (supportsFamille) {
                    score += 5.0;
                    reasons.add("Pack adapté aux familles (" + profile.nombreBeneficiaires() + " bénéficiaires) (+5)");
                }
            }

            // Normalize score to 0-100
            score = Math.min(100.0, Math.max(0.0, score));

            String reason = String.join("; ", reasons);
            scored.add(new RecommendationDTO(p.getIdPack(), p.getNomPack(), (int) Math.round(score), reason, price));
        }

        return scored.stream()
                .sorted(Comparator.comparingDouble(RecommendationDTO::score).reversed())
                .collect(Collectors.toList());
    }

    public List<RecommendationDTO> recommendProduits(ClientProfile profile) {
        List<Produit> produits = produitRepository.findAll();

        List<RecommendationDTO> scored = new ArrayList<>();

        for (Produit prod : produits) {
            if (prod.getStatut() != Statut.ACTIF) {
                continue;
            }

            double score = 0.0;
            List<String> reasons = new ArrayList<>();

            score += 30.0;
            reasons.add("Produit actif (+30)");

            if (profile.produitTypePreferee() != null && !profile.produitTypePreferee().isBlank() && prod.getTypeProduit() != null
                    && prod.getTypeProduit().name().equalsIgnoreCase(profile.produitTypePreferee())) {
                score += 35.0;
                reasons.add("Correspondance avec le type demandé (+35)");
            }

            if (prod.getTypeProduit() != null) {
                double add = switch (prod.getTypeProduit()) {
                    case SANTE -> 20.0;
                    case VIE -> 15.0;
                    case AUTO, HABITATION -> 10.0;
                    default -> 5.0;
                };
                score += add;
                reasons.add("Type %s (+%.0f)".formatted(prod.getTypeProduit(), add));
            }

            double avgPrice = 0.0;
            if (prod.getIdProduit() != null) {
                List<Pack> productPacks = packRepository.findByProduitId(prod.getIdProduit());
                if (!productPacks.isEmpty()) {
                    avgPrice = productPacks.stream()
                            .mapToDouble(Pack::getPrixMensuel)
                            .average()
                            .orElse(0.0);

                    if (avgPrice > 0 && profile.budget() >= avgPrice) {
                        score += 15.0;
                        reasons.add("Prix moyen compatible avec budget (+15)");
                    }
                }
            }

            score = Math.min(100.0, Math.max(0.0, score));

            scored.add(new RecommendationDTO(prod.getIdProduit(), prod.getNomProduit(), (int) Math.round(score), String.join("; ", reasons), avgPrice));
        }

        return scored.stream()
                .sorted(Comparator.comparingDouble(RecommendationDTO::score).reversed())
                .collect(Collectors.toList());
    }

    // === DTOs ===
    public static record ClientProfile(
            String sexe,
            int age,
            int nombreBeneficiaires,
            double budget,
            List<String> garantiesVoulu,
            String couvertureGeographique,
            String produitTypePreferee,
            List<String> maladiesChroniques,
            String situationFamiliale
    ) {
        public ClientProfile(String sexe, int age, int nombreBeneficiaires, double budget,
                             List<String> garantiesVoulu, String couvertureGeographique,
                             String produitTypePreferee) {
            this(sexe, age, nombreBeneficiaires, budget, garantiesVoulu,
                 couvertureGeographique, produitTypePreferee,
                 List.of(), "");
        }
    }

    public static record RecommendationDTO(String packId, String nomPack, int score, String reason, double price) {}

}
