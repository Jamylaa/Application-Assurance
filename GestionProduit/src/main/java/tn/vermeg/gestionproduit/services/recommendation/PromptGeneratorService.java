package tn.vermeg.gestionproduit.services.recommendation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tn.vermeg.gestionproduit.dto.BeneficiaireDTO;
import tn.vermeg.gestionproduit.dto.ClientProfileDTO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service de génération de prompts intelligents pour l'IA
 * Crée des prompts bien structurés et informatifs à partir du profil client
 */
@Service
public class PromptGeneratorService {
    
    private static final Logger logger = LoggerFactory.getLogger(PromptGeneratorService.class);
    
    /**
     * Génère un prompt structuré et intelligent pour l'IA de recommandation
     * @param profilClient Le profil du client
     * @return Le prompt formaté pour l'IA
     */
    public String genererPromptRecommandation(ClientProfileDTO profilClient) {
        logger.info(" Génération du prompt IA pour client: {} {}", profilClient.getPrenom(), profilClient.getNom());
        
        StringBuilder prompt = new StringBuilder();
        
        // En-tête du prompt
        prompt.append("=== DEMANDE DE RECOMMANDATION D'ASSURANCE PERSONNALISÉE ===\n\n");
        
        // Section 1: Informations démographiques
        prompt.append("## PROFIL CLIENT\n");
        prompt.append(String.format("- Nom: %s %s\n", profilClient.getPrenom(), profilClient.getNom()));
        prompt.append(String.format("- Âge: %d ans\n", profilClient.getAge() != null ? profilClient.getAge() : 0));
        prompt.append(String.format("- Sexe: %s\n", profilClient.getSexe() != null ? profilClient.getSexe() : "Non spécifié"));
        prompt.append(String.format("- Profession: %s\n", profilClient.getProfession() != null ? profilClient.getProfession() : "Non spécifiée"));
        prompt.append(String.format("- Situation familiale: %s\n", profilClient.getSituationFamiliale() != null ? profilClient.getSituationFamiliale() : "Non spécifiée"));
        
        // Section 2: Informations sur les bénéficiaires
        if (profilClient.getBeneficiaires() != null && !profilClient.getBeneficiaires().isEmpty()) {
            prompt.append("\n## BÉNÉFICIAIRES\n");
            prompt.append(String.format("- Nombre total: %d\n", profilClient.getNombreBeneficiaires() != null ? profilClient.getNombreBeneficiaires() : profilClient.getBeneficiaires().size()));
            for (BeneficiaireDTO ben : profilClient.getBeneficiaires()) {
                prompt.append(String.format("  • %s %s (%d ans) - Lien: %s, État santé: %s\n",
                    ben.getPrenom(), ben.getNom(), ben.getAge(),
                    ben.getLien(), ben.getEtatSante() != null ? ben.getEtatSante() : "Non spécifié"));
            }
        }
        
        // Section 3: Informations de santé
        prompt.append("\n## ÉTAT DE SANTÉ ET BIEN-ÊTRE\n");
        prompt.append(String.format("- État général: %s\n", profilClient.getEtatSante() != null ? profilClient.getEtatSante() : "Non spécifié"));
        prompt.append(String.format("- Fumeur: %s\n", profilClient.isFumeur() ? "Oui" : "Non"));
        
        if (profilClient.getMaladiesChroniques() != null && !profilClient.getMaladiesChroniques().isEmpty()) {
            prompt.append(String.format("- Maladies chroniques: %s\n", String.join(", ", profilClient.getMaladiesChroniques())));
        }
        
        if (profilClient.getAllergie() != null && !profilClient.getAllergie().isEmpty()) {
            prompt.append(String.format("- Allergies: %s\n", String.join(", ", profilClient.getAllergie())));
        }
        
        // Section 4: Préférences et budget
        prompt.append("\n## PRÉFÉRENCES ET BUDGET\n");
        prompt.append(String.format("- Budget mensuel maximal: %.2f TND\n", profilClient.getBudgetMensuelMax() != null ? profilClient.getBudgetMensuelMax() : 0));
        prompt.append(String.format("- Budget annuel maximal: %.2f TND\n", profilClient.getBudgetAnnuelMax() != null ? profilClient.getBudgetAnnuelMax() : 0));
        prompt.append(String.format("- Niveau de couverture préféré: %s\n", profilClient.getNiveauCouverturePreferee() != null ? profilClient.getNiveauCouverturePreferee() : "Sans préférence"));
        
        // Garanties souhaitées
        if (profilClient.getGarantiesChoisies() != null && !profilClient.getGarantiesChoisies().isEmpty()) {
            prompt.append(String.format("- Garanties souhaitées: %s\n", String.join(", ", profilClient.getGarantiesChoisies())));
        }
        
        // Plafonds
        if (profilClient.getPlafonds() != null && !profilClient.getPlafonds().isEmpty()) {
            prompt.append("- Plafonds demandés:\n");
            profilClient.getPlafonds().forEach((clé, valeur) -> 
                prompt.append(String.format("  • %s: %.2f TND\n", clé, valeur))
            );
        }
        
        // Section 5: Type de produit et couverture géographique
        if (profilClient.getTypeProduitPreferee() != null) {
            prompt.append(String.format("\n## PRODUIT D'ASSURANCE\n"));
            prompt.append(String.format("- Type préféré: %s\n", profilClient.getTypeProduitPreferee()));
        }
        
        if (profilClient.getCouvertureGeographique() != null) {
            prompt.append(String.format("- Couverture géographique: %s\n", profilClient.getCouvertureGeographique()));
        }
        
        // Section 6: Historique
        if (profilClient.isClientExistant()) {
            prompt.append("\n## HISTORIQUE CLIENT\n");
            prompt.append(String.format("- Client existant: Oui\n"));
            if (profilClient.getAncienneteAnnees() != null) {
                prompt.append(String.format("- Ancienneté: %s\n", profilClient.getAncienneteAnnees()));
            }
            if (profilClient.getHistoriquePrecedentClient() != null) {
                prompt.append(String.format("- Parcours antérieur: %s\n", profilClient.getHistoriquePrecedentClient()));
            }
        }
        
        // Section 7: Remarques spéciales
        if (profilClient.getRemarquesSpeciales() != null && !profilClient.getRemarquesSpeciales().isEmpty()) {
            prompt.append(String.format("\n## REMARQUES SPÉCIALES\n"));
            prompt.append(String.format("%s\n", profilClient.getRemarquesSpeciales()));
        }
        
        // Section 8: Instructions pour l'IA
        prompt.append("\n=== TÂCHE ===\n");
        prompt.append("Basé sur le profil client ci-dessus, recommandez les meilleures assurances (produits, packs et garanties) de notre catalogue.\n");
        prompt.append("Pour chaque recommandation:\n");
        prompt.append("1. Expliquez pourquoi c'est une bonne match pour ce client\n");
        prompt.append("2. Énumérez les avantages spécifiques\n");
        prompt.append("3. Énumérez les points importants à considérer\n");
        prompt.append("4. Calculez un score de pertinence (0-100)\n");
        prompt.append("\nRetournez les résultats en JSON avec la structure: {\"recommandations\": [{\"type\": \"PRODUIT/PACK/GARANTIE\", \"nom\": \"\", \"score\": 0, \"raison\": \"\", \"avantages\": [], \"considerationsPertinentes\": []}]}\n");
        
        String promptFinal = prompt.toString();
        logger.debug("✅ Prompt généré avec succès ({} caractères)", promptFinal.length());
        
        return promptFinal;
    }
    
    /**
     * Génère un prompt pour l'explication détaillée des recommandations
     * @param resumeRecommandations Un résumé des recommandations proposées
     * @param profilClient Le profil du client
     * @return Le prompt pour l'explication
     */
    public String genererPromptExplication(String resumeRecommandations, ClientProfileDTO profilClient) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("=== DEMANDE D'EXPLICATION DÉTAILLÉE ===\n\n");
        prompt.append(String.format("Client: %s %s (%d ans, %s)\n", 
            profilClient.getPrenom(), profilClient.getNom(), 
            profilClient.getAge() != null ? profilClient.getAge() : 0,
            profilClient.getProfession() != null ? profilClient.getProfession() : "profession non spécifiée"));
        
        prompt.append(String.format("Budget mensuel: %.2f TND\n", profilClient.getBudgetMensuelMax() != null ? profilClient.getBudgetMensuelMax() : 0));
        
        if (profilClient.getGarantiesChoisies() != null && !profilClient.getGarantiesChoisies().isEmpty()) {
            prompt.append(String.format("Garanties demandées: %s\n", String.join(", ", profilClient.getGarantiesChoisies())));
        }
        
        prompt.append("\n## RECOMMANDATIONS PROPOSÉES\n");
        prompt.append(resumeRecommandations);
        
        prompt.append("\n## TÂCHE\n");
        prompt.append("Fournissez une explication complète et détaillée en français:\n");
        prompt.append("1. Résumé global des recommandations\n");
        prompt.append("2. Explication de la stratégie de recommandation\n");
        prompt.append("3. Justification de chaque recommandation par rapport aux besoins du client\n");
        prompt.append("4. Points forts de la sélection\n");
        prompt.append("5. Points d'amélioration ou précautions à considérer\n");
        prompt.append("6. Prochaines étapes recommandées pour le client\n");
        
        return prompt.toString();
    }
}

