package tn.vermeg.gestionproduit.dto;

import java.util.List;
import java.util.Map;

public class ClientProfileDTO {
    
    // === INFORMATIONS PERSONNELLES ===
    private String idClient;
    private String prenom;
    private String nom;
    private String email;
    private String telephone;
    
    // Détails démographiques
    private String sexe;  // M, F, Autre
    private Integer age;
    private String profession;
    private String situationFamiliale; // Célibataire, Marié, Divorcé, Veuf
    
    // === INFORMATIONS SUR LES BÉNÉFICIAIRES ===
    private Integer nombreBeneficiaires;
    private List<BeneficiaireDTO> beneficiaires;
    
    // === SANTÉ ET BIEN-ÊTRE ===
    private List<String> maladiesChroniques;  // Ex: diabète, hypertension, asthme
    private String etatSante; // Bon, Moyen, Mauvais
    private boolean fumeur;
    private List<String> allergie;
    
    // === PRÉFÉRENCES D'ASSURANCE ===
    private Double budgetMensuelMax;
    private Double budgetAnnuelMax;
    private String niveauCouverturePreferee; // BASIC, STANDARD, PREMIUM, GOLD
    
    // Garanties souhaitées
    private List<String> garantiesChoisies; // Ex: HOSPITALISATION, DENTAIRE, OPTIQUE
    private List<String> garantiesExclues;
    
    // Plafonds
    private Map<String, Double> plafonds; // clé: type, valeur: montant
    
    // === HISTORIQUE ===
    private String historiquePrecedentClient; // Descriptif de son parcours
    private boolean clientExistant;
    private String ancienneteAnnees; // Combien d'années client
    
    // === PRÉFÉRENCES ADDITIONNELLES ===
    private String typeProduitPreferee; // SANTE, AUTO, HABITATION, VIE, EPARGNE, PREVOYANCE
    private String couvertureGeographique; // NATIONAL, UE, INTERNATIONAL, MAGHREB
    private boolean resiliableAnnuellement;
    
    // === CONTEXTE ===
    private String objetRapport; // Raison de la recommandation
    private String remarquesSpeciales;
    
    // Constructeurs
    public ClientProfileDTO() {}
    
    public ClientProfileDTO(String idClient, String prenom, String nom, 
                           Integer age, String sexe, String profession) {
        this.idClient = idClient;
        this.prenom = prenom;
        this.nom = nom;
        this.age = age;
        this.sexe = sexe;
        this.profession = profession;
    }
    
    // Getters & Setters
    public String getIdClient() { return idClient; }
    public void setIdClient(String idClient) { this.idClient = idClient; }
    
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    
    public String getSexe() { return sexe; }
    public void setSexe(String sexe) { this.sexe = sexe; }
    
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    
    public String getProfession() { return profession; }
    public void setProfession(String profession) { this.profession = profession; }
    
    public String getSituationFamiliale() { return situationFamiliale; }
    public void setSituationFamiliale(String situationFamiliale) { this.situationFamiliale = situationFamiliale; }
    
    public Integer getNombreBeneficiaires() { return nombreBeneficiaires; }
    public void setNombreBeneficiaires(Integer nombreBeneficiaires) { this.nombreBeneficiaires = nombreBeneficiaires; }
    
    public List<BeneficiaireDTO> getBeneficiaires() { return beneficiaires; }
    public void setBeneficiaires(List<BeneficiaireDTO> beneficiaires) { this.beneficiaires = beneficiaires; }
    
    public List<String> getMaladiesChroniques() { return maladiesChroniques; }
    public void setMaladiesChroniques(List<String> maladiesChroniques) { this.maladiesChroniques = maladiesChroniques; }
    
    public String getEtatSante() { return etatSante; }
    public void setEtatSante(String etatSante) { this.etatSante = etatSante; }
    
    public boolean isFumeur() { return fumeur; }
    public void setFumeur(boolean fumeur) { this.fumeur = fumeur; }
    
    public List<String> getAllergie() { return allergie; }
    public void setAllergie(List<String> allergie) { this.allergie = allergie; }
    
    public Double getBudgetMensuelMax() { return budgetMensuelMax; }
    public void setBudgetMensuelMax(Double budgetMensuelMax) { this.budgetMensuelMax = budgetMensuelMax; }
    
    public Double getBudgetAnnuelMax() { return budgetAnnuelMax; }
    public void setBudgetAnnuelMax(Double budgetAnnuelMax) { this.budgetAnnuelMax = budgetAnnuelMax; }
    
    public String getNiveauCouverturePreferee() { return niveauCouverturePreferee; }
    public void setNiveauCouverturePreferee(String niveauCouverturePreferee) { this.niveauCouverturePreferee = niveauCouverturePreferee; }
    
    public List<String> getGarantiesChoisies() { return garantiesChoisies; }
    public void setGarantiesChoisies(List<String> garantiesChoisies) { this.garantiesChoisies = garantiesChoisies; }
    
    public List<String> getGarantiesExclues() { return garantiesExclues; }
    public void setGarantiesExclues(List<String> garantiesExclues) { this.garantiesExclues = garantiesExclues; }
    
    public Map<String, Double> getPlafonds() { return plafonds; }
    public void setPlafonds(Map<String, Double> plafonds) { this.plafonds = plafonds; }
    
    public String getHistoriquePrecedentClient() { return historiquePrecedentClient; }
    public void setHistoriquePrecedentClient(String historiquePrecedentClient) { this.historiquePrecedentClient = historiquePrecedentClient; }
    
    public boolean isClientExistant() { return clientExistant; }
    public void setClientExistant(boolean clientExistant) { this.clientExistant = clientExistant; }
    
    public String getAncienneteAnnees() { return ancienneteAnnees; }
    public void setAncienneteAnnees(String ancienneteAnnees) { this.ancienneteAnnees = ancienneteAnnees; }
    
    public String getTypeProduitPreferee() { return typeProduitPreferee; }
    public void setTypeProduitPreferee(String typeProduitPreferee) { this.typeProduitPreferee = typeProduitPreferee; }
    
    public String getCouvertureGeographique() { return couvertureGeographique; }
    public void setCouvertureGeographique(String couvertureGeographique) { this.couvertureGeographique = couvertureGeographique; }
    
    public boolean isResiliableAnnuellement() { return resiliableAnnuellement; }
    public void setResiliableAnnuellement(boolean resiliableAnnuellement) { this.resiliableAnnuellement = resiliableAnnuellement; }
    
    public String getObjetRapport() { return objetRapport; }
    public void setObjetRapport(String objetRapport) { this.objetRapport = objetRapport; }
    
    public String getRemarquesSpeciales() { return remarquesSpeciales; }
    public void setRemarquesSpeciales(String remarquesSpeciales) { this.remarquesSpeciales = remarquesSpeciales; }
    
    @Override
    public String toString() {
        return String.format("ClientProfile{nom='%s %s', age=%d, profession='%s', beneficiaires=%d, budget=%.2f}",
            prenom, nom, age, profession, nombreBeneficiaires != null ? nombreBeneficiaires : 0,
            budgetMensuelMax != null ? budgetMensuelMax : 0);
    }
}

