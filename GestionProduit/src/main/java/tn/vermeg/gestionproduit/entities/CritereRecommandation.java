package tn.vermeg.gestionproduit.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Entité définissant les règles de recommandation d'un produit/pack par le chatbot IA.
 *
 * <p>Le pipeline du chatbot utilise ces critères pour scorer et classer les produits
 * selon le profil détecté dans le message de l'utilisateur.
 * Ce modèle rend les règles de recommandation éditables depuis l'interface admin,
 * sans modifier le code du chatbot.
 *
 * <p>Relation : un CritereRecommandation est lié à UN Pack (ou à un Produit seul).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "criteres_recommandation")
public class CritereRecommandation {

    @Id
    private String idCritere;

    // ─── CIBLE ────────────────────────────────────────────────────────────────

    /** ID du produit associé. */
    @NotBlank
    @Indexed
    private String produitId;

    /** ID du pack spécifique (null = critères génériques du produit). */
    @Indexed
    private String packId;

    // ─── MOTS CLÉS ────────────────────────────────────────────────────────────

    /**
     * Mots-clés qui déclenchent la recommandation de ce produit/pack.
     * Exemple : ["famille", "enfants", "hospitalisation", "maternité"]
     */
    private List<String> motsCles;

    /**
     * Synonymes et variations acceptées (pour le NLP du chatbot).
     * Exemple : ["hospit", "hôpital", "clinique", "urgences"]
     */
    private List<String> synonymes;

    // ─── CAS D'USAGE ──────────────────────────────────────────────────────────

    /**
     * Situations typiques pour lesquelles ce produit est recommandé.
     * Exemple : ["Jeune actif sans enfant", "Famille avec enfants en bas âge"]
     */
    private List<String> casUsages;

    /**
     * Profils types des clients susceptibles de souscrire.
     * Exemple : ["25-35 ans", "Salarié", "Budget < 100 TND/mois"]
     */
    private List<String> profilsRecommandes;

    // ─── SCORING ──────────────────────────────────────────────────────────────

    /**
     * Score de base du produit/pack (0–100).
     * Modifié dynamiquement par les pondérations selon le profil détecté.
     */
    @Min(0) @Max(100)
    @Builder.Default
    private double scoreBase = 50.0;

    /**
     * Poids des attributs dans le calcul du score final.
     * Clés : "budget", "age", "typeClient", "couverture", "duree"
     * Valeurs : multiplicateur (ex: 0.3 = 30% du score)
     */
    private Map<String, Double> poidsAttributs;

    /**
     * Bonus de score si certaines conditions sont remplies.
     * Ex: {"typeClient=FAMILLE": 15.0, "ageMin=25_ageMax=40": 10.0}
     */
    private Map<String, Double> bonusConditionnels;

    // ─── RÈGLES DE DISQUALIFICATION ───────────────────────────────────────────

    /**
     * Conditions qui éliminent ce produit/pack de la recommandation.
     * Ex: ["age > 70", "typeClient = ENTREPRISE", "budget < 30"]
     */
    private List<String> reglesDisqualification;

    // ─── ORDRE D'AFFICHAGE ────────────────────────────────────────────────────

    /**
     * Priorité d'affichage en cas d'égalité de score (valeur plus basse = affiché en premier).
     */
    @Builder.Default
    private int prioriteAffichage = 5;

    // ─── STATUT ───────────────────────────────────────────────────────────────

    @Builder.Default
    private boolean actif = true;

    // ─── AUDIT ────────────────────────────────────────────────────────────────

    private String creePar;

    @CreatedDate
    private Instant dateCreation;

    @LastModifiedDate
    private Instant dateModification;

    // ─── MÉTHODES MÉTIER ──────────────────────────────────────────────────────

    /**
     * Calcule un score de recommandation contextualisé selon les attributs du profil client.
     *
     * @param attributsProfil map des attributs détectés (ex: {"age": 32.0, "budget": 80.0})
     * @return score final entre 0 et 100
     */
    public double calculerScore(Map<String, Double> attributsProfil) {
        double score = scoreBase;
        if (poidsAttributs != null && attributsProfil != null) {
            for (Map.Entry<String, Double> poids : poidsAttributs.entrySet()) {
                Double valeur = attributsProfil.get(poids.getKey());
                if (valeur != null) {
                    score += valeur * poids.getValue();
                }
            }
        }
        return Math.max(0, Math.min(100, score));
    }

    public boolean contientMotCle(String terme) {
        String termeLower = terme.toLowerCase();
        return (motsCles != null && motsCles.stream().anyMatch(m -> m.toLowerCase().contains(termeLower)))
                || (synonymes != null && synonymes.stream().anyMatch(s -> s.toLowerCase().contains(termeLower)));
    }
}
