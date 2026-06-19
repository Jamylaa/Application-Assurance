package tn.vermeg.gestionproduit.dto.chatbot;

import tn.vermeg.gestionproduit.entities.Garantie;
import tn.vermeg.gestionproduit.entities.Pack;
import tn.vermeg.gestionproduit.entities.PackGarantie;
import tn.vermeg.gestionproduit.entities.Produit;

import java.util.ArrayList;
import java.util.List;
public class PackCreationResponseDTO {

    private boolean success;
    private String message;
    private PackDetails packDetails;
    private ProductDetails productDetails;
    private List<GuaranteeDetails> guarantees;
    private String hierarchyVisualization;
    private List<String> warnings;
    private List<String> errors;

    // Constructeurs
    public PackCreationResponseDTO() {
        this.success = false;
        this.warnings = new ArrayList<>();
        this.errors = new ArrayList<>();}

    public PackCreationResponseDTO(boolean success, String message) {
        this();
        this.success = success;
        this.message = message;}

    // Getters et Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public PackDetails getPackDetails() { return packDetails; }
    public void setPackDetails(PackDetails packDetails) { this.packDetails = packDetails; }

    public ProductDetails getProductDetails() { return productDetails; }
    public void setProductDetails(ProductDetails productDetails) { this.productDetails = productDetails; }

    public List<GuaranteeDetails> getGuarantees() { return guarantees; }
    public void setGuarantees(List<GuaranteeDetails> guarantees) { this.guarantees = guarantees; }

    public String getHierarchyVisualization() { return hierarchyVisualization; }
    public void setHierarchyVisualization(String hierarchyVisualization) { this.hierarchyVisualization = hierarchyVisualization; }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }

    // Méthodes utilitaires
    public void addWarning(String warning) {
        this.warnings.add(warning);
    }
    public void addError(String error) {
        this.errors.add(error);
        this.success = false;
    }

    public static class PackDetails {
        private String idPack;
        private String nomPack;
        private String description;
        private Integer ageMinimum;
        private Integer ageMaximum;
        private Double prixMensuel;
        private Integer dureeMinContrat;
        private Integer dureeMaxContrat;
        private String niveauCouverture;
        private String couvertureGeographique;
        private String statut;
        private List<String> typeClients;
        private Integer ancienneteContratMois;

        public static PackDetails fromEntity(Pack pack) {
            PackDetails details = new PackDetails();
            details.setIdPack(pack.getIdPack());
            details.setNomPack(pack.getNomPack());
            details.setDescription(pack.getDescription());
            details.setAgeMinimum(pack.getAgeMinimum());
            details.setAgeMaximum(pack.getAgeMaximum());
            details.setPrixMensuel(pack.getPrixMensuel());
            details.setDureeMinContrat(pack.getDureeMinContrat());
            details.setDureeMaxContrat(pack.getDureeMaxContrat());
            details.setNiveauCouverture(pack.getNiveauCouverture() != null ? pack.getNiveauCouverture().name() : null);
            details.setCouvertureGeographique(pack.getCouvertureGeographique() != null ? pack.getCouvertureGeographique().name() : null);
            details.setStatut(pack.getStatut() != null ? pack.getStatut().name() : null);
            if (pack.getTypeClients() != null) {
                details.setTypeClients(pack.getTypeClients().stream()
                    .map(tc -> tc.name())
                    .toList());}
            details.setAncienneteContratMois(pack.getAncienneteContratMois());
            return details;
        }

        // Getters et Setters
        public String getIdPack() { return idPack; }
        public void setIdPack(String idPack) { this.idPack = idPack; }
        public String getNomPack() { return nomPack; }
        public void setNomPack(String nomPack) { this.nomPack = nomPack; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Integer getAgeMinimum() { return ageMinimum; }
        public void setAgeMinimum(Integer ageMinimum) { this.ageMinimum = ageMinimum; }
        public Integer getAgeMaximum() { return ageMaximum; }
        public void setAgeMaximum(Integer ageMaximum) { this.ageMaximum = ageMaximum; }
        public Double getPrixMensuel() { return prixMensuel; }
        public void setPrixMensuel(Double prixMensuel) { this.prixMensuel = prixMensuel; }
        public Integer getDureeMinContrat() { return dureeMinContrat; }
        public void setDureeMinContrat(Integer dureeMinContrat) { this.dureeMinContrat = dureeMinContrat; }
        public Integer getDureeMaxContrat() { return dureeMaxContrat; }
        public void setDureeMaxContrat(Integer dureeMaxContrat) { this.dureeMaxContrat = dureeMaxContrat; }
        public String getNiveauCouverture() { return niveauCouverture; }
        public void setNiveauCouverture(String niveauCouverture) { this.niveauCouverture = niveauCouverture; }
        public String getCouvertureGeographique() { return couvertureGeographique; }
        public void setCouvertureGeographique(String couvertureGeographique) { this.couvertureGeographique = couvertureGeographique; }
        public String getStatut() { return statut; }
        public void setStatut(String statut) { this.statut = statut; }
        public List<String> getTypeClients() { return typeClients; }
        public void setTypeClients(List<String> typeClients) { this.typeClients = typeClients; }
        public Integer getAncienneteContratMois() { return ancienneteContratMois; }
        public void setAncienneteContratMois(Integer ancienneteContratMois) { this.ancienneteContratMois = ancienneteContratMois; }
    }
    public static class ProductDetails {
        private String idProduit;
        private String nomProduit;
        private String description;
        private String typeProduit;
        private String statut;

        public static ProductDetails fromEntity(Produit produit) {
            ProductDetails details = new ProductDetails();
            details.setIdProduit(produit.getIdProduit());
            details.setNomProduit(produit.getNomProduit());
            details.setDescription(produit.getDescription());
            details.setTypeProduit(produit.getTypeProduit() != null ? produit.getTypeProduit().name() : null);
            details.setStatut(produit.getStatut() != null ? produit.getStatut().name() : null);
            return details;
        }

        // Getters et Setters
        public String getIdProduit() { return idProduit; }
        public void setIdProduit(String idProduit) { this.idProduit = idProduit; }
        public String getNomProduit() { return nomProduit; }
        public void setNomProduit(String nomProduit) { this.nomProduit = nomProduit; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getTypeProduit() { return typeProduit; }
        public void setTypeProduit(String typeProduit) { this.typeProduit = typeProduit; }
        public String getStatut() { return statut; }
        public void setStatut(String statut) { this.statut = statut; }
    }

    public static class GuaranteeDetails {
        private String idGarantie;
        private String nomGarantie;
        private String description;
        private String domaine;
        private Double tauxRemboursement;
        private Double plafond;
        private Double franchise;
        private String typeMontant;
        private boolean optionnelle;
        private Double supplementPrix;
        private Integer delaiCarence;
        private Integer priorite;

        public static GuaranteeDetails fromEntities(Garantie garantie, PackGarantie packGarantie) {
            GuaranteeDetails details = new GuaranteeDetails();
            details.setIdGarantie(garantie.getIdGarantie());
            details.setNomGarantie(garantie.getNomGarantie());
            details.setDescription(garantie.getDescription());
            details.setDomaine(garantie.getDomaine() != null ? garantie.getDomaine().name() : null);
            
            // Utiliser les valeurs de l'association si disponibles, sinon celles de la garantie
            details.setTauxRemboursement(packGarantie.getTauxRemboursement() > 0 ? 
                packGarantie.getTauxRemboursement() : garantie.getTauxRemboursement());
            details.setPlafond(packGarantie.getPlafond() > 0 ? 
                packGarantie.getPlafond() : garantie.getPlafondAnnuel());
            details.setFranchise(packGarantie.getFranchise() != 0 ? 
                packGarantie.getFranchise() : garantie.getFranchise());
            details.setTypeMontant(packGarantie.getTypeMontant() != null ? 
                packGarantie.getTypeMontant().name() : 
                (garantie.getTypeMontant() != null ? garantie.getTypeMontant().name() : null));
            details.setOptionnelle(packGarantie.isOptionnelle());
            details.setSupplementPrix(packGarantie.getSupplementPrix());
            details.setDelaiCarence(packGarantie.getDelaiCarence());
            details.setPriorite(packGarantie.getPriorite());
            
            return details;
        }

        // Getters et Setters
        public String getIdGarantie() { return idGarantie; }
        public void setIdGarantie(String idGarantie) { this.idGarantie = idGarantie; }
        public String getNomGarantie() { return nomGarantie; }
        public void setNomGarantie(String nomGarantie) { this.nomGarantie = nomGarantie; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getDomaine() { return domaine; }
        public void setDomaine(String domaine) { this.domaine = domaine; }
        public Double getTauxRemboursement() { return tauxRemboursement; }
        public void setTauxRemboursement(Double tauxRemboursement) { this.tauxRemboursement = tauxRemboursement; }
        public Double getPlafond() { return plafond; }
        public void setPlafond(Double plafond) { this.plafond = plafond; }
        public Double getFranchise() { return franchise; }
        public void setFranchise(Double franchise) { this.franchise = franchise; }
        public String getTypeMontant() { return typeMontant; }
        public void setTypeMontant(String typeMontant) { this.typeMontant = typeMontant; }
        public boolean isOptionnelle() { return optionnelle; }
        public void setOptionnelle(boolean optionnelle) { this.optionnelle = optionnelle; }
        public Double getSupplementPrix() { return supplementPrix; }
        public void setSupplementPrix(Double supplementPrix) { this.supplementPrix = supplementPrix; }
        public Integer getDelaiCarence() { return delaiCarence; }
        public void setDelaiCarence(Integer delaiCarence) { this.delaiCarence = delaiCarence; }
        public Integer getPriorite() { return priorite; }
        public void setPriorite(Integer priorite) { this.priorite = priorite; }
    }

    public void buildHierarchyVisualization() {
        if (productDetails == null || packDetails == null) {
            this.hierarchyVisualization = "Informations incomplètes pour générer la hiérarchie";
            return;}

        StringBuilder sb = new StringBuilder();
        sb.append("┌── Produit: ").append(productDetails.getNomProduit())
          .append(" (").append(productDetails.getTypeProduit()).append(")\n");
        sb.append("│   └── Pack: ").append(packDetails.getNomPack())
          .append(" (").append(packDetails.getNiveauCouverture()).append(")\n");

        if (guarantees != null && !guarantees.isEmpty()) {
            for (int i = 0; i < guarantees.size(); i++) {
                GuaranteeDetails g = guarantees.get(i);
                String prefix = (i == guarantees.size() - 1) ? "│       └──" : "│       ├──";
                sb.append(prefix).append(" Garantie: ").append(g.getNomGarantie());
                if (g.getDomaine() != null) {
                    sb.append(" [").append(g.getDomaine()).append("]");
                }
                if (g.getTauxRemboursement() != null) {
                    sb.append(" (").append((g.getTauxRemboursement() * 100)).append("%)");
                }
                sb.append("\n");}
        } else {sb.append("│  └── (Aucune garantie associée)\n");}
        this.hierarchyVisualization = sb.toString();
    }
}
