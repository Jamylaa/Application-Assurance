package tn.vermeg.gestionproduit.services.chatbot.orchestration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tn.vermeg.gestionproduit.entities.Garantie;
import tn.vermeg.gestionproduit.entities.Pack;
import tn.vermeg.gestionproduit.entities.PackGarantie;
import tn.vermeg.gestionproduit.entities.Produit;
import tn.vermeg.gestionproduit.enums.CouvertureGeographique;
import tn.vermeg.gestionproduit.enums.DomaineMedical;
import tn.vermeg.gestionproduit.enums.NiveauCouverture;
import tn.vermeg.gestionproduit.enums.Statut;
import tn.vermeg.gestionproduit.enums.TypeClient;
import tn.vermeg.gestionproduit.enums.TypeMontant;
import tn.vermeg.gestionproduit.enums.TypePlafond;
import tn.vermeg.gestionproduit.enums.TypeProduit;
import tn.vermeg.gestionproduit.services.chatbot.core.ChatbotAction;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
 // Service de normalisation des actions et données pour le chatbot
@Service
public class ActionNormalizationService {
    private static final Logger logger = LoggerFactory.getLogger(ActionNormalizationService.class);
  // Normalise une action textuelle en utilisant l'énumération ChatbotAction
    public ChatbotAction normalizeAction(String action) {
        return ChatbotAction.normalize(action);
    }

     //Normalise le type de produit
    public TypeProduit normalizeTypeProduit(String typeProduit) {
        if (typeProduit == null || typeProduit.trim().isEmpty()) {
            return null; // Ne pas appliquer de valeur par défaut - laisser le système demander explicitement
        }

        String normalized = typeProduit.toUpperCase().trim();
        return switch (normalized) {
            case "AUTO", "VOITURE", "VÉHICULE", "VEHICULE" -> TypeProduit.AUTO;
            case "HABITATION", "LOGEMENT", "MAISON", "HOME" -> TypeProduit.HABITATION;
            case "VIE", "DÉCÈS", "DECES", "SURVIE" -> TypeProduit.VIE;
            case "PRÉVOYANCE", "PREVOYANCE", "PRÉVISION", "PREVISION" -> TypeProduit.VIE;
            case "ÉPARGNE", "EPARGNE", "INVESTISSEMENT", "PLACEMENT" -> TypeProduit.EPARGNE;
            case "SANTE", "SANTÉ", "HEALTH" -> TypeProduit.SANTE;
            default -> null; // Type non reconnu - retourner null au lieu d'appliquer SANTE par défaut
        };
    }
     // Normalise le domaine médical - Utilise DomaineMedical comme source de vérité unique
    public DomaineMedical normalizeDomaineMedical(String domaine) {
        if (domaine == null || domaine.trim().isEmpty()) {
            return DomaineMedical.AUTRE;
        }

        // Utiliser la méthode fromString() de DomaineMedical qui gère la conversion robuste
        // avec gestion des espaces, tirets, slashes et fallback sur AUTRE
        return DomaineMedical.fromString(domaine);
    }

     //Normalise le type de montant
    public TypeMontant normalizeTypeMontant(String typeMontant) {
        if (typeMontant == null || typeMontant.trim().isEmpty()) {
            logger.warn("TypeMontant est null ou vide, retourne null pour validation");
            return null; // Ne pas appliquer de valeur par défaut - laisser la validation gérer
        }
        String normalized = typeMontant.toUpperCase().trim();
        if (normalized.contains("FORFAIT")) {
            logger.info("TypeMontant normalisé: FORFAIT");
            return TypeMontant.FORFAIT;
        }
        if (normalized.contains("TARIF") && (normalized.contains("CONVENTION") || normalized.contains("CONVENTIONNE"))) {
            logger.info("TypeMontant normalisé: TARIF_CONVENTIONNE");
            return TypeMontant.TARIF_CONVENTIONNE;
        }
        if (normalized.contains("CONVENTION")) {
            logger.info("TypeMontant normalisé: TARIF_CONVENTIONNE");
            return TypeMontant.TARIF_CONVENTIONNE;
        }
        if (normalized.contains("FRAIS") && (normalized.contains("REEL") || normalized.contains("RÉEL"))) {
            logger.info("TypeMontant normalisé: FRAIS_REELS");
            return TypeMontant.FRAIS_REELS;
        }
        logger.warn("TypeMontant non reconnu: {}, retourne null pour validation", typeMontant);
        return null; // Retourner null au lieu d'une valeur par défaut
    }
     //Normalise le statut
    public Statut normalizeStatut(String statut) {
        if (statut == null || statut.trim().isEmpty()) {
            return Statut.ACTIF; // Valeur par défaut
        }
        String normalized = statut.toUpperCase().trim();
        if (normalized.contains("INACTIF") || normalized.contains("DÉSACTIV") || normalized.contains("DESACTIV")) {
            return Statut.INACTIF;
        }
        if (normalized.contains("EN_ATTENTE") || normalized.contains("ATTENTE")) {
            return Statut.EN_ATTENTE;}
        return Statut.ACTIF;
    }
     //Normalise la couverture géographique
    public CouvertureGeographique normalizeCouvertureGeographique(String couverture) {
        if (couverture == null || couverture.trim().isEmpty()) {
            return null; // Ne pas appliquer de valeur par défaut - laisser le système demander explicitement
        }
        String normalized = couverture.toUpperCase().trim();
        if (normalized.contains("INTERNATIONAL") || normalized.contains("MONDE")) {
            return CouvertureGeographique.INTERNATIONAL;
        }
        if (normalized.contains("EUROPE") || normalized.contains("UE") || normalized.contains("UNION EU")) {
            return CouvertureGeographique.UE;
        }

        if (normalized.contains("MAGHREB") || normalized.contains("AFRIQUE NORD")) {
            return CouvertureGeographique.MAGHREB;
        }

        if (normalized.contains("RÉGIONAL") || normalized.contains("REGIONAL") || normalized.contains("LOCAL")) {
            return CouvertureGeographique.LOCAL;
        }

        if (normalized.contains("NATIONAL") || normalized.contains("NATIONALE") || normalized.contains("TUNISIE")) {
            return CouvertureGeographique.NATIONAL;
        }

        return null; // Ne pas appliquer de valeur par défaut si non reconnu
    }
     // Normalise le niveau de couverture
    public NiveauCouverture normalizeNiveauCouverture(String niveau) {
        if (niveau == null || niveau.trim().isEmpty()) {
            return NiveauCouverture.BASIC; // Valeur par défaut
        }
        String normalized = niveau.toUpperCase().trim();
        if (normalized.contains("GOLD")) {
            return NiveauCouverture.GOLD;}
        if (normalized.contains("PREMIUM") || normalized.contains("SUPÉRIEUR") || normalized.contains("SUPERIEUR")) {
            return NiveauCouverture.PREMIUM;}
        if (normalized.contains("BASIC") || normalized.contains("BRONZE") || normalized.contains("MINIMUM")) {
            return NiveauCouverture.BASIC;}
        return NiveauCouverture.BASIC;
    }
     // Normalise les types de clients
    public List<TypeClient> normalizeTypeClients(List<String> typeClients) {
        if (typeClients == null || typeClients.isEmpty()) {
            return Arrays.asList(TypeClient.INDIVIDUEL); // Valeur par défaut
        }
        return typeClients.stream()
            .map(this::normalizeTypeClient)
            .distinct()
            .collect(Collectors.toList());
    }
     //Normalise un type de client individuel
    private TypeClient normalizeTypeClient(String typeClient) {
        if (typeClient == null || typeClient.trim().isEmpty()) {
            return TypeClient.INDIVIDUEL;
        }

        String normalized = typeClient.toUpperCase().trim();
        
        if (normalized.contains("FAMILLE") || normalized.contains("FAMILIAL")) {
            return TypeClient.FAMILLE;}
        
        if (normalized.contains("SENIOR") || normalized.contains("ÂGÉ") || normalized.contains("AGE")) {
            return TypeClient.SENIOR;
        }
        if (normalized.contains("ENTREPRISE") || normalized.contains("PROFESSIONNEL") || normalized.contains("PRO")) {
            return TypeClient.ENTREPRISE;
        }
        return TypeClient.INDIVIDUEL;
    }
     // Normalise le type de plafond
    public TypePlafond normalizeTypePlafond(String typePlafond) {
        if (typePlafond == null || typePlafond.trim().isEmpty()) {
            return TypePlafond.ANNUEL; // Valeur par défaut
        }
        String normalized = typePlafond.toUpperCase().trim();
        
        if (normalized.contains("MENSUEL")) {
            return TypePlafond.MENSUEL;
        }
        
        if (normalized.contains("PAR ACTE") || normalized.contains("ACTE")) {
            return TypePlafond.PAR_ACTE;
        }
        
        return TypePlafond.ANNUEL;
    }
     // Normalise une valeur booléenne
    public Boolean normalizeBoolean(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String normalized = value.toLowerCase().trim();
        if (normalized.equals("true") || normalized.equals("oui") || 
            normalized.equals("yes") || normalized.equals("actif") || 
            normalized.equals("activé") || normalized.equals("enabled")) {
            return true;
        }
        if (normalized.equals("false") || normalized.equals("non") || 
            normalized.equals("no") || normalized.equals("inactif") || 
            normalized.equals("désactivé") || normalized.equals("disabled")) {
            return false;
        }
        return null;
    }
     // Normalise une valeur numérique avec gestion des erreurs
    public Double normalizeDouble(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            String cleanValue = value.replaceAll("[^\\d.,-]", "").replace(",", ".");
            return Double.parseDouble(cleanValue);
        } catch (NumberFormatException e) {
            logger.warn("Impossible de normaliser la valeur numérique: {}", value);
            return null;
        }
    }
     // Normalise une valeur entière avec gestion des erreurs
    public Integer normalizeInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            String cleanValue = value.replaceAll("[^\\d-]", "");
            return Integer.parseInt(cleanValue);
        } catch (NumberFormatException e) {
            logger.warn("Impossible de normaliser la valeur entière: {}", value);
            return null;
        }
    }
     // Applique les valeurs par défaut pour une garantie
    public void applyGarantieDefaults(tn.vermeg.gestionproduit.entities.Garantie garantie) {
        logger.info("=== APPLICATION VALEURS PAR DÉFAUT GARANTIE ===");
        
        if (garantie.getTauxRemboursement() == 0.0) {
            logger.warn("Taux de remboursement = 0, application valeur par défaut: 0.8");
            garantie.setTauxRemboursement(0.8); // 80% par défaut
        }
        
        // NE PAS appliquer de valeur par défaut pour typeMontant
        // Laisser null pour validation bloquante
        if (garantie.getTypeMontant() == null) {
            logger.warn("TypeMontant est null, PAS de valeur par défaut appliquée (validation requise)");
        }
        
        if (garantie.getTypePlafond() == null) {
            logger.info("TypePlafond null, application valeur par défaut: ANNUEL");
            garantie.setTypePlafond(TypePlafond.ANNUEL);
        }
        
        // NE PAS appliquer de valeur par défaut pour coutMoyenParSinistre
        // Laisser 0.0 pour indiquer que l'information n'a pas été fournie
        if (garantie.getCoutMoyenParSinistre() == 0.0) {
            logger.warn("Coût moyen par sinistre = 0, PAS de valeur par défaut appliquée");
        }
        
        if (garantie.getDureeMinContrat() == 0) {
            logger.warn("Durée min contrat = 0, application valeur par défaut: 12");
            garantie.setDureeMinContrat(12);
        }
        
        // NE PAS appliquer de valeur par défaut pour dureeMaxContrat
        // Laisser l'extraction IA ou regex gérer cette valeur critique
        if (garantie.getDureeMaxContrat() == 0) {
            logger.warn("Durée max contrat = 0, PAS de valeur par défaut appliquée (validation requise)");
        }
        
        if (!garantie.isResiliableAnnuellement()) {
            logger.info("Résiliable annuellement = false, application valeur par défaut: true");
            garantie.setResiliableAnnuellement(true);
        }
        
        if (garantie.getStatut() == null) {
            logger.info("Statut null, application valeur par défaut: ACTIF");
            garantie.setStatut(Statut.ACTIF);
        }
        
        if (garantie.getFranchise() == 0.0) {
            logger.warn("Franchise = 0, application valeur par défaut: 0.0");
            garantie.setFranchise(0.0);
        }
        
        // Calculer les plafonds dérivés si nécessaire
        if (garantie.getPlafondAnnuel() != 0.0) {
            if (garantie.getPlafondMensuel() == 0.0) {
                logger.info("Calcul plafond mensuel à partir du plafond annuel");
                garantie.setPlafondMensuel(garantie.getPlafondAnnuel() / 12.0);
            }
            if (garantie.getPlafondParActe() == 0.0) {
                logger.info("Calcul plafond par acte à partir du plafond annuel");
                garantie.setPlafondParActe(garantie.getPlafondAnnuel() / 24.0);
            }
        }
        
        logger.info("=== FIN APPLICATION VALEURS PAR DÉFAUT ===");
    }

     // Applique les valeurs par défaut pour un pack (uniquement les valeurs critiques métier)
    public void applyPackDefaults(tn.vermeg.gestionproduit.entities.Pack pack) {
        // Ne plus appliquer de valeurs par défaut silencieuses pour éviter les incohérences
        // Les champs manquants doivent être explicitement demandés à l'utilisateur
        
        // Seul le statut peut avoir une valeur par défaut métier légitime
        if (pack.getStatut() == null) {
            pack.setStatut(Statut.ACTIF);
        }
        
        // Ancienneté peut rester à 0 (pas de contrainte)
        if (pack.getAncienneteContratMois() == 0) {
            pack.setAncienneteContratMois(0);
        }
    }

     //Applique les valeurs par défaut pour un produit
    public void applyProduitDefaults(tn.vermeg.gestionproduit.entities.Produit produit) {
        if (produit.getStatut() == null) {
            produit.setStatut(Statut.ACTIF);
        }
        
        if (produit.getTypeProduit() == null) {
            produit.setTypeProduit(TypeProduit.SANTE);
        }
    }
}