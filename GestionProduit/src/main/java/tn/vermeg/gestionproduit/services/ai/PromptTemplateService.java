package tn.vermeg.gestionproduit.services.ai;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class PromptTemplateService {

    public String buildGarantieExtractionPrompt(String userPrompt) {
        return """
            Tu es un expert en extraction d'informations pour les assurances. Analyse le prompt et extrais les données structurées pour créer une garantie.
            
            Règles d'extraction:
            - nom: nom exact de la garantie (ex: "hospitalisation", "optique", "dentaire")
            - type: type de garantie (HOSPITALISATION, OPTIQUE, DENTAIRE, MATERNITE, PHARMACIE, CONSULTATION, AUTRE)
            - description: description détaillée si fournie, sinon null
            - tauxRemboursement: taux en décimal (0.5 pour 50%, 0.8 pour 80%)
            - typeMontant: FRAIS_REELS, POURCENTAGE ou FORFAITAIRE
            - plafondAnnuel: montant annuel maximum en nombre
            - plafondMensuel: montant mensuel maximum en nombre
            - plafondParActe: montant par acte en nombre
            - franchise: montant de la franchise en nombre
            - coutMoyenParSinistre: coût moyen par sinistre en nombre
            - dureeMinContrat: durée minimum en mois
            - dureeMaxContrat: durée maximum en mois
            - resiliableAnnuellement: true/false
            - statut: ACTIF ou INACTIF
            
            Retourne UNIQUEMENT un JSON valide sans texte autour.
            
            Prompt utilisateur: %s
            """.formatted(userPrompt);
    }

    public String buildRecommendationPrompt(Map<String, Object> criteria, String originalPrompt) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Tu es un assistant de recommandation d'assurance expert.\n\n");
        prompt.append("Demande client : ").append(originalPrompt).append("\n\n");
        prompt.append("Critères extraits :\n");
        
        if (criteria.containsKey("age")) {
            prompt.append("- Âge : ").append(criteria.get("age")).append(" ans\n");
        }
        if (criteria.containsKey("budgetMensuel")) {
            prompt.append("- Budget mensuel maximal : ").append(criteria.get("budgetMensuel")).append(" DT\n");
        }
        if (criteria.containsKey("typeProduit")) {
            prompt.append("- Type de produit préféré : ").append(criteria.get("typeProduit")).append("\n");
        }
        if (criteria.containsKey("typeClient")) {
            prompt.append("- Type de client : ").append(criteria.get("typeClient")).append("\n");
        }
        if (criteria.containsKey("niveauCouverture")) {
            prompt.append("- Niveau de couverture : ").append(criteria.get("niveauCouverture")).append("\n");
        }
        if (criteria.containsKey("couvertureGeographique")) {
            prompt.append("- Couverture géographique : ").append(criteria.get("couvertureGeographique")).append("\n");
        }
        if (criteria.containsKey("garantiesRecherchees")) {
            prompt.append("- Garanties recherchées : ").append(criteria.get("garantiesRecherchees")).append("\n");
        }
        
        prompt.append("\nTâche :\n");
        prompt.append("1. Analyse les besoins du client.\n");
        prompt.append("2. Vérifie la cohérence des critères.\n");
        prompt.append("3. Propose une réponse structurée et facile à comprendre.\n");
        prompt.append("4. Incluez une explication des critères de sélection.\n");
        prompt.append("5. Sois empathique et professionnel dans ton ton.\n");
        prompt.append("\nRetourne UNIQUEMENT le texte de la recommandation, sans métadonnées internes.\n");
        
        return prompt.toString();
    }

    public String buildSystemInstruction() {
        return """
            Tu es un moteur d'extraction et de recommandation pour un domaine d'assurance.
            Contraintes OBLIGATOIRES :
            - Réponds UNIQUEMENT par un JSON valide (pas de texte, pas de markdown).
            - N'invente pas de valeurs : mets null si l'information n'est pas présente.
            - Les montants sont des nombres (ex: 20000). Les durées sont en mois.
            - Les taux sont entre 0 et 1 (ex: 0.5 pour 50%).
            - Les scores de recommandation sont entre 0 et 100.
            - Utilise ces enums quand applicable :
              * statut: ACTIF | INACTIF | EN_ATTENTE
              * typeProduit: SANTE | AUTO | HABITATION | VIE | PREVOYANCE | EPARGNE
              * niveauCouverture: BASIC | STANDARD | PREMIUM | GOLD
              * couvertureGeographique: NATIONAL | LOCAL | UE | MAGHREB | INTERNATIONAL
              * typeClients: tableau de INDIVIDUEL | FAMILLE | ENTREPRISE | SENIOR
              * typeGarantie: HOSPITALISATION | DENTAIRE | OPTIQUE | CONSULTATION | EXAMEN | MEDICAMENTS | SOINS_GENERAUX | INTERNATIONAL
              * typeMontant: FRAIS_REELS | FORFAIT | TARIF_CONVENTIONNE
            """;
    }

    public String buildChatResponsePrompt(String userMessage, String conversationContext) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Tu es un assistant intelligent pour une plateforme d'assurance.\n\n");
        
        if (conversationContext != null && !conversationContext.isBlank()) {
            prompt.append("Contexte de la conversation :\n").append(conversationContext).append("\n\n");
        }
        
        prompt.append("Message utilisateur : ").append(userMessage).append("\n\n");
        prompt.append("Instructions :\n");
        prompt.append("- Sois concis et professionnel\n");
        prompt.append("- Aide l'utilisateur à créer/configurer des garanties, produits ou packs\n");
        prompt.append("- Si l'utilisateur demande une recommandation, pose des questions clarifiantes si nécessaire\n");
        prompt.append("- Utilise un ton empathique et orienté solution\n");
        prompt.append("\nRéponds en français.");
        
        return prompt.toString();
    }
}
