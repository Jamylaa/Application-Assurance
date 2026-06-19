package tn.vermeg.gestionproduit.enums;

public enum DomaineMedical {
    // Médecine générale
    CONSULTATION_GENERALE("Consultation générale"),
    MEDECINE_FAMILIALE("Médecine familiale"),
    MEDECINE_INTERNE("Médecine interne"),
    MEDECINE_PREVENTIVE("Médecine préventive"),
    // Spécialités médicales
    CARDIOLOGIE("Cardiologie"),
    DERMATOLOGIE("Dermatologie"),
    ENDOCRINOLOGIE("Endocrinologie"),
    GASTRO_ENTEROLOGIE("Gastro-entérologie"),
    HEMATOLOGIE("Hématologie"),
    INFECTIOLOGIE("Infectiologie"),
    NEPHROLOGIE("Néphrologie"),
    NEUROLOGIE("Neurologie"),
    ONCOLOGIE("Oncologie"),
    PNEUMOLOGIE("Pneumologie"),
    RHUMATOLOGIE("Rhumatologie"),
    ALLERGOLOGIE("Allergologie"),
    // Santé de la femme
    GYNECOLOGIE("Gynécologie"),
    OBSTETRIQUE("Obstétrique"),
    FERTILITE_PMA("Fertilité / PMA"),
    SUIVI_GROSSESSE("Suivi grossesse"),
    // Santé de l'enfant
    PEDIATRIE("Pédiatrie"),
    NEONATOLOGIE("Néonatologie"),
    PEDOPSYCHIATRIE("Pédopsychiatrie"),
    // Santé mentale
    PSYCHIATRIE("Psychiatrie"),
    PSYCHOLOGIE("Psychologie"),
    PSYCHOTHERAPIE("Psychothérapie"),
    ADDICTOLOGIE("Addictologie"),
    // Chirurgie
    CHIRURGIE_GENERALE("Chirurgie générale"),
    CHIRURGIE_ESTHETIQUE("Chirurgie esthétique"),
    CHIRURGIE_ORTHOPEDIQUE("Chirurgie orthopédique"),
    NEUROCHIRURGIE("Neurochirurgie"),
    CHIRURGIE_CARDIAQUE("Chirurgie cardiaque"),
    CHIRURGIE_DIGESTIVE("Chirurgie digestive"),
    CHIRURGIE_UROLOGIQUE("Chirurgie urologique"),
    // Dentaire
    DENTISTERIE_GENERALE("Dentisterie générale"),
    ORTHODONTIE("Orthodontie"),
    IMPLANTOLOGIE("Implantologie"),
    CHIRURGIE_DENTAIRE("Chirurgie dentaire"),
    // Vision et ORL
    OPHTALMOLOGIE("Ophtalmologie"),
    ORL("ORL"),
    AUDIOLOGIE("Audiologie"),
    // Rééducation et thérapies
    KINESITHERAPIE("Kinésithérapie"),
    PHYSIOTHERAPIE("Physiothérapie"),
    ERGOTHERAPIE("Ergothérapie"),
    ORTHOPHONIE("Orthophonie"),
    CHIROPRATIE("Chiropractie"),
    OSTEOPATHIE("Ostéopathie"),
    // Examens et diagnostic
    RADIOLOGIE("Radiologie"),
    IMAGERIE_MEDICALE("Imagerie médicale"),
    ANALYSES_BIOLOGIQUES("Analyses biologiques"),
    MEDECINE_NUCLEAIRE("Médecine nucléaire"),
    // Médecines spécialisées
    MEDECINE_SPORT("Médecine du sport"),
    MEDECINE_TRAVAIL("Médecine du travail"),
    GERIATRIE("Gériatrie"),
    NUTRITION_DIETETIQUE("Nutrition / Diététique"),
    MEDECINE_ESTHETIQUE("Médecine esthétique"),
    MEDECINE_ALTERNATIVE("Médecine alternative"),
    // Urgences et soins critiques
    URGENCES_MEDICALES("Urgences médicales"),
    REANIMATION("Réanimation"),
    SOINS_INTENSIFS("Soins intensifs"),
    // Téléconsultation
    CONSULTATION_VIDEO("Consultation vidéo"),
    SUIVI_DISTANCE("Suivi à distance"),
    DEUXIEME_AVIS_MEDICAL("Deuxième avis médical international"),
    // Domaines existants conservés pour compatibilité
    HOSPITALISATION("Hospitalisation"),
    DENTAIRE("Dentaire"),
    OPTIQUE("Optique"),
    PHARMACIE("Pharmacie"),
    MATERNITE("Maternité"),
    PREVENTION("Prévention"),
    SOINS_DENTAIRES("Soins dentaires"),
    SOINS_OPTIQUES("Soins optiques"),
    HOSPITALISATION_CHIRURGICALE("Hospitalisation chirurgicale"),
    HOSPITALISATION_MEDICALE("Hospitalisation médicale"),
    MEDECINE_GENERALE("Médecine générale"),
    SPECIALITES_MEDICALES("Spécialités médicales"),
    PARAMEDICAL("Paramédical"),
    ANALYSES_MEDICALES("Analyses médicales"),
    REPATRIEMENT("Rapatriement"),
    DECES("Décès"),
    INVALIDITE("Invalidité"),
    // Autre pour les nouveaux types non classés
    AUTRE("Autre");
    private final String description;
    DomaineMedical(String description) {this.description = description;}
    public String getDescription() {return description;}
    public static DomaineMedical fromString(String text) {
        if (text == null) {return AUTRE;}
        try {return DomaineMedical.valueOf(text.toUpperCase().replace(" ", "_").replace("-", "_").replace("/", "_"));
        } catch (IllegalArgumentException e) {
            return AUTRE;}
    }
}