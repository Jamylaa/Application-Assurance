package tn.vermeg.gestionproduit.entities;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Types de garanties santé, chacun rattaché à un domaine médical.
 */
public enum TypeGarantie {
    HOSPITALISATION("Hospitalisation", DomaineSante.HOSPITALISATION),
    DENTAIRE("Soins dentaires", DomaineSante.DENTAIRE),
    OPTIQUE("Optique", DomaineSante.OPTIQUE),
    CONSULTATION("Consultations médicales", DomaineSante.SOINS_COURANTS),
    EXAMEN("Examens et analyses", DomaineSante.SOINS_COURANTS),
    MEDICAMENTS("Pharmacie et médicaments", DomaineSante.PHARMACIE),
    SOINS_GENERAUX("Soins généraux", DomaineSante.SOINS_COURANTS),
    INTERNATIONAL("Couverture internationale", DomaineSante.ASSISTANCE);

    private final String libelle;
    private final DomaineSante domaine;

    TypeGarantie(String libelle, DomaineSante domaine) {
        this.libelle = libelle;
        this.domaine = domaine;
    }

    public String getLibelle() {
        return libelle;
    }

    public DomaineSante getDomaine() {
        return domaine;
    }

    /**
     * Retourne tous les types de garanties appartenant à un domaine donné.
     */
    public static List<TypeGarantie> parDomaine(DomaineSante domaine) {
        return java.util.Arrays.stream(values())
                .filter(t -> t.domaine == domaine)
                .collect(Collectors.toList());
    }

    /**
     * Parse une chaîne libre (texte utilisateur ou IA) vers un type de garantie.
     * Retourne SOINS_GENERAUX par défaut si aucun type n'est reconnu.
     */
    public static TypeGarantie fromString(String value) {
        if (value == null || value.isBlank()) {
            return SOINS_GENERAUX;
        }

        String normalized = value.toUpperCase(Locale.ROOT).trim();

        for (TypeGarantie type : values()) {
            if (normalized.equals(type.name())) {
                return type;
            }
        }

        if (normalized.contains("HOSPITAL") || normalized.contains("HÔPITAL") || normalized.contains("HOPITAL")) {
            return HOSPITALISATION;
        }
        if (normalized.contains("CONSULT") || normalized.contains("MÉDECIN") || normalized.contains("MEDECIN")
                || normalized.contains("SPÉCIALISTE") || normalized.contains("SPECIALISTE")) {
            return CONSULTATION;
        }
        if (normalized.contains("DENT")) {
            return DENTAIRE;
        }
        if (normalized.contains("OPTIQU") || normalized.contains("LUNETT") || normalized.contains("VERRE")) {
            return OPTIQUE;
        }
        if (normalized.contains("MÉDICAMENT") || normalized.contains("MEDICAMENT") || normalized.contains("PHARMAC")) {
            return MEDICAMENTS;
        }
        if (normalized.contains("EXAMEN") || normalized.contains("ANALYSE") || normalized.contains("RADIO")) {
            return EXAMEN;
        }
        if (normalized.contains("INTERNATIONAL") || normalized.contains("MONDE")) {
            return INTERNATIONAL;
        }

        return SOINS_GENERAUX;
    }
}
