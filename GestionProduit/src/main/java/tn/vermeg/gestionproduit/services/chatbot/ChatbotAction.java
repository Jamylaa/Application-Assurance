package tn.vermeg.gestionproduit.services.chatbot;

//Énumération normalisée pour toutes les actions du chatbot pour éviter les doublons
public enum ChatbotAction {
    
    // Actions de création
    GARANTIE("Création d'une garantie"),
    PRODUIT("Création d'un produit"),
    PACK("Création d'un pack"),
    
    // Actions de configuration
    CONFIGURATION_PACK("Configuration d'un pack"),
    AJOUT_GARANTIE_PACK("Ajout de garantie à un pack"),
    
    // Actions de consultation
    CONSULTATION_GARANTIE("Consultation des garanties"),
    CONSULTATION_PRODUIT("Consultation des produits"),
    CONSULTATION_PACK("Consultation des packs"),
    
    // Actions de modification
    MODIFICATION_GARANTIE("Modification d'une garantie"),
    MODIFICATION_PRODUIT("Modification d'un produit"),
    MODIFICATION_PACK("Modification d'un pack"),
    
    // Actions de suppression
    SUPPRESSION_GARANTIE("Suppression d'une garantie"),
    SUPPRESSION_PRODUIT("Suppression d'un produit"),
    SUPPRESSION_PACK("Suppression d'un pack"),

    // Action de recommandation
    RECOMMENDATION("Recommandation personnalisée");
    
    private final String description;
    ChatbotAction(String description) {
        this.description = description;
    }
    public String getDescription() {
        return description;
    }

    public static ChatbotAction normalize(String actionText) {
        if (actionText == null) {
            return null;
        }
        
        String normalized = actionText.toUpperCase().trim();
        
        // Gestion des variations pour GARANTIE
        if (normalized.contains("GARANTIE") || normalized.contains("WARRANTY") || 
            normalized.contains("COVERAGE") || normalized.contains("ASSURANCE")) {
            if (normalized.contains("CRÉER") || normalized.contains("CREATE") || 
                normalized.contains("AJOUTER") || normalized.contains("ADD")) {
                return GARANTIE;
            }
            if (normalized.contains("MODIFIER") || normalized.contains("UPDATE") || 
                normalized.contains("MODIFIE")) {
                return MODIFICATION_GARANTIE;
            }
            if (normalized.contains("SUPPRIMER") || normalized.contains("DELETE") || 
                normalized.contains("SUPPRIME")) {
                return SUPPRESSION_GARANTIE;
            }
            if (normalized.contains("CONSULT") || normalized.contains("VOIR") || 
                normalized.contains("LISTE")) {
                return CONSULTATION_GARANTIE;
            }
        }
        
        // Gestion des variations pour PRODUIT
        if (normalized.contains("PRODUIT") || normalized.contains("PRODUCT") || 
            normalized.contains("PRODUCTION")) {
            if (normalized.contains("CRÉER") || normalized.contains("CREATE") || 
                normalized.contains("AJOUTER") || normalized.contains("ADD")) {
                return PRODUIT;
            }
            if (normalized.contains("MODIFIER") || normalized.contains("UPDATE") || 
                normalized.contains("MODIFIE")) {
                return MODIFICATION_PRODUIT;
            }
            if (normalized.contains("SUPPRIMER") || normalized.contains("DELETE") || 
                normalized.contains("SUPPRIME")) {
                return SUPPRESSION_PRODUIT;
            }
            if (normalized.contains("CONSULT") || normalized.contains("VOIR") || 
                normalized.contains("LISTE")) {
                return CONSULTATION_PRODUIT;
            }
        }
        
        // Gestion des variations pour PACK
        if (normalized.contains("PACK") || normalized.contains("PACKAGE") || 
            normalized.contains("PACKAGING") || normalized.contains("OFFRE")) {
            if (normalized.contains("CRÉER") || normalized.contains("CREATE") || 
                normalized.contains("AJOUTER") || normalized.contains("ADD")) {
                return PACK;
            }
            if (normalized.contains("CONFIG") || normalized.contains("CONFIGUR") || 
                normalized.contains("PARAMÈTRE") || normalized.contains("SETTING")) {
                return CONFIGURATION_PACK;
            }
            if (normalized.contains("AJOUT") && normalized.contains("GARANTIE")) {
                return AJOUT_GARANTIE_PACK;
            }
            if (normalized.contains("MODIFIER") || normalized.contains("UPDATE") || 
                normalized.contains("MODIFIE")) {
                return MODIFICATION_PACK;
            }
            if (normalized.contains("SUPPRIMER") || normalized.contains("DELETE") || 
                normalized.contains("SUPPRIME")) {
                return SUPPRESSION_PACK;
            }
            if (normalized.contains("CONSULT") || normalized.contains("VOIR") || 
                normalized.contains("LISTE")) {
                return CONSULTATION_PACK;
            }
        }
        // Recommandation
        if (normalized.contains("RECOMMAND") || normalized.contains("RECOMMEND") ||
            normalized.contains("SUGGÉR") || normalized.contains("SUGGER") ||
            normalized.contains("CONSEIL") || normalized.contains("PROPOSER") ||
            normalized.contains("BESOIN") || normalized.contains("BESOINS") ||
            normalized.contains("QUEL PRODUIT") || normalized.contains("QUELLE GARANTIE") ||
            normalized.contains("QUE ME") || normalized.contains("ADAPTÉ") || normalized.contains("ADAPTE")) {
            return RECOMMENDATION;
        }

        // Actions spécifiques
        if (normalized.contains("CONFIGURATION") || normalized.contains("CONFIG")) {
            return CONFIGURATION_PACK;
        }
        
        if (normalized.contains("AJOUT") && normalized.contains("GARANTIE") && 
            normalized.contains("PACK")) {
            return AJOUT_GARANTIE_PACK;
        }
        
        return null;
    }
     // Vérifie si l'action est une action de création
    public boolean isCreationAction() {
        return this == GARANTIE || this == PRODUIT || this == PACK;
    }

     // Vérifie si l'action est une action de configuration
    public boolean isConfigurationAction() {
        return this == CONFIGURATION_PACK || this == AJOUT_GARANTIE_PACK;
    }

     //Vérifie si l'action est une action de consultation
    public boolean isConsultationAction() {
        return this == CONSULTATION_GARANTIE || this == CONSULTATION_PRODUIT || 
               this == CONSULTATION_PACK;
    }

   //Vérifie si l'action est une action de modification
    public boolean isModificationAction() {
        return this == MODIFICATION_GARANTIE || this == MODIFICATION_PRODUIT || 
               this == MODIFICATION_PACK;
    }

     //Vérifie si l'action est une action de suppression
    public boolean isSuppressionAction() {
        return this == SUPPRESSION_GARANTIE || this == SUPPRESSION_PRODUIT || 
               this == SUPPRESSION_PACK;
    }
    
    @Override
    public String toString() {
        return this.name() + " (" + description + ")";
    }
}