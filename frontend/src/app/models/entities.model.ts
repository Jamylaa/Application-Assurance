
export enum TypeProduit {
  SANTE = 'SANTE',
  HABITATION = 'HABITATION',
  AUTO = 'AUTO',
  EPARGNE = 'EPARGNE',
  VIE = 'VIE'
}

export enum TypeClient {
  INDIVIDUEL = 'INDIVIDUEL',
  FAMILLE = 'FAMILLE',
  ENFANT = 'ENFANT',
  SENIOR = 'SENIOR',
  ENTREPRISE = 'ENTREPRISE',
  ETUDIANT = 'ETUDIANT'
}

export enum NiveauCouverture {
  BASIC = 'BASIC',
  PREMIUM = 'PREMIUM',
  GOLD = 'GOLD'
}

export enum StatutWorkflow {
  BROUILLON = 'BROUILLON',
  SOUMIS_VALIDATION = 'SOUMIS_VALIDATION',
  EN_COURS_VALIDATION = 'EN_COURS_VALIDATION',
  APPROUVE = 'APPROUVE',
  REJETE = 'REJETE',
  PUBLIE = 'PUBLIE',
  ARCHIVE = 'ARCHIVE',
  SUSPENDU = 'SUSPENDU'
}

export enum DomaineMedical {
  // Médecine générale
  CONSULTATION_GENERALE = 'CONSULTATION_GENERALE',
  MEDECINE_FAMILIALE = 'MEDECINE_FAMILIALE',
  MEDECINE_INTERNE = 'MEDECINE_INTERNE',
  MEDECINE_PREVENTIVE = 'MEDECINE_PREVENTIVE',

  // Spécialités médicales
  CARDIOLOGIE = 'CARDIOLOGIE',
  DERMATOLOGIE = 'DERMATOLOGIE',
  ENDOCRINOLOGIE = 'ENDOCRINOLOGIE',
  GASTRO_ENTEROLOGIE = 'GASTRO_ENTEROLOGIE',
  HEMATOLOGIE = 'HEMATOLOGIE',
  INFECTIOLOGIE = 'INFECTIOLOGIE',
  NEPHROLOGIE = 'NEPHROLOGIE',
  NEUROLOGIE = 'NEUROLOGIE',
  ONCOLOGIE = 'ONCOLOGIE',
  PNEUMOLOGIE = 'PNEUMOLOGIE',
  RHUMATOLOGIE = 'RHUMATOLOGIE',
  ALLERGOLOGIE = 'ALLERGOLOGIE',

  // Santé de la femme
  GYNECOLOGIE = 'GYNECOLOGIE',
  OBSTETRIQUE = 'OBSTETRIQUE',
  FERTILITE_PMA = 'FERTILITE_PMA',
  SUIVI_GROSSESSE = 'SUIVI_GROSSESSE',

  // Santé de l'enfant
  PEDIATRIE = 'PEDIATRIE',
  NEONATOLOGIE = 'NEONATOLOGIE',
  PEDOPSYCHIATRIE = 'PEDOPSYCHIATRIE',

  // Santé mentale
  PSYCHIATRIE = 'PSYCHIATRIE',
  PSYCHOLOGIE = 'PSYCHOLOGIE',
  PSYCHOTHERAPIE = 'PSYCHOTHERAPIE',
  ADDICTOLOGIE = 'ADDICTOLOGIE',

  // Chirurgie
  CHIRURGIE_GENERALE = 'CHIRURGIE_GENERALE',
  CHIRURGIE_ESTHETIQUE = 'CHIRURGIE_ESTHETIQUE',
  CHIRURGIE_ORTHOPEDIQUE = 'CHIRURGIE_ORTHOPEDIQUE',
  NEUROCHIRURGIE = 'NEUROCHIRURGIE',
  CHIRURGIE_CARDIAQUE = 'CHIRURGIE_CARDIAQUE',
  CHIRURGIE_DIGESTIVE = 'CHIRURGIE_DIGESTIVE',
  CHIRURGIE_UROLOGIQUE = 'CHIRURGIE_UROLOGIQUE',

  // Dentaire
  DENTISTERIE_GENERALE = 'DENTISTERIE_GENERALE',
  ORTHODONTIE = 'ORTHODONTIE',
  IMPLANTOLOGIE = 'IMPLANTOLOGIE',
  CHIRURGIE_DENTAIRE = 'CHIRURGIE_DENTAIRE',

  // Vision et ORL
  OPHTALMOLOGIE = 'OPHTALMOLOGIE',
  ORL = 'ORL',
  AUDIOLOGIE = 'AUDIOLOGIE',

  // Rééducation et thérapies
  KINESITHERAPIE = 'KINESITHERAPIE',
  PHYSIOTHERAPIE = 'PHYSIOTHERAPIE',
  ERGOTHERAPIE = 'ERGOTHERAPIE',
  ORTHOPHONIE = 'ORTHOPHONIE',
  CHIROPRATIE = 'CHIROPRATIE',
  OSTEOPATHIE = 'OSTEOPATHIE',

  // Examens et diagnostic
  RADIOLOGIE = 'RADIOLOGIE',
  IMAGERIE_MEDICALE = 'IMAGERIE_MEDICALE',
  ANALYSES_BIOLOGIQUES = 'ANALYSES_BIOLOGIQUES',
  MEDECINE_NUCLEAIRE = 'MEDECINE_NUCLEAIRE',

  // Médecines spécialisées
  MEDECINE_SPORT = 'MEDECINE_SPORT',
  MEDECINE_TRAVAIL = 'MEDECINE_TRAVAIL',
  GERIATRIE = 'GERIATRIE',
  NUTRITION_DIETETIQUE = 'NUTRITION_DIETETIQUE',
  MEDECINE_ESTHETIQUE = 'MEDECINE_ESTHETIQUE',
  MEDECINE_ALTERNATIVE = 'MEDECINE_ALTERNATIVE',

  // Urgences et soins critiques
  URGENCES_MEDICALES = 'URGENCES_MEDICALES',
  REANIMATION = 'REANIMATION',
  SOINS_INTENSIFS = 'SOINS_INTENSIFS',

  // Téléconsultation
  CONSULTATION_VIDEO = 'CONSULTATION_VIDEO',
  SUIVI_DISTANCE = 'SUIVI_DISTANCE',
  DEUXIEME_AVIS_MEDICAL = 'DEUXIEME_AVIS_MEDICAL',

  // Domaines existants conservés pour compatibilité
  HOSPITALISATION = 'HOSPITALISATION',
  DENTAIRE = 'DENTAIRE',
  OPTIQUE = 'OPTIQUE',
  PHARMACIE = 'PHARMACIE',
  MATERNITE = 'MATERNITE',
  PREVENTION = 'PREVENTION',
  SOINS_DENTAIRES = 'SOINS_DENTAIRES',
  SOINS_OPTIQUES = 'SOINS_OPTIQUES',
  HOSPITALISATION_CHIRURGICALE = 'HOSPITALISATION_CHIRURGICALE',
  HOSPITALISATION_MEDICALE = 'HOSPITALISATION_MEDICALE',
  MEDECINE_GENERALE = 'MEDECINE_GENERALE',
  SPECIALITES_MEDICALES = 'SPECIALITES_MEDICALES',
  PARAMEDICAL = 'PARAMEDICAL',
  ANALYSES_MEDICALES = 'ANALYSES_MEDICALES',
  REPATRIEMENT = 'REPATRIEMENT',
  DECES = 'DECES',
  INVALIDITE = 'INVALIDITE',

  // Autre pour les nouveaux types non classés
  AUTRE = 'AUTRE'
}

export enum TypeMontant {
  FORFAIT = 'FORFAIT',
  FRAIS_REELS = 'FRAIS_REELS',
  TARIF_CONVENTIONNE = 'TARIF_CONVENTIONNE'
}

export enum TypePlafond {
  PAR_ACTE = 'PAR_ACTE',
  ANNUEL = 'ANNUEL',
  MENSUEL = 'MENSUEL',
  PAR_SOINS = 'PAR_SOINS',
  GLOBAL = 'GLOBAL'
}

export enum CouvertureGeographique {
  LOCAL = 'LOCAL',
  NATIONAL = 'NATIONAL',
  INTERNATIONAL = 'INTERNATIONAL',
  UE = 'UE',
  MAGHREB = 'MAGHREB'
}

export enum TypeFranchise {
  AUCUNE = 'AUCUNE',
  FIXE = 'FIXE',
  POURCENTAGE = 'POURCENTAGE',
  RELATIVE = 'RELATIVE',
  ABSOLUE = 'ABSOLUE'
}

export enum TypeRemboursement {
  FRAIS_REELS = 'FRAIS_REELS',
  FORFAIT = 'FORFAIT',
  TARIF_CONVENTIONNE = 'TARIF_CONVENTIONNE',
  CAPITAL_DECES = 'CAPITAL_DECES',
  INDEMNITE_JOURNALIERE = 'INDEMNITE_JOURNALIERE',
  RENTE_VIAGERE = 'RENTE_VIAGERE',
  RENTE_EDUCATION = 'RENTE_EDUCATION',
  VALEUR_A_NEUF = 'VALEUR_A_NEUF',
  VALEUR_VENALE = 'VALEUR_VENALE'
}

// === ENTITÉS PRINCIPALES ===

// DTOs simplifiés pour éviter les relations circulaires
export interface GarantieSimple {
  idGarantie: string;
  nomGarantie: string;
  domaine?: DomaineMedical;
}

export interface PackSimple {
  idPack: string;
  nomPack: string;
  description: string;
  prixMensuel: number;
  niveauCouverture?: NiveauCouverture;
}

export interface Produit {
  idProduit: string;
  codeProduit?: string;
  nomProduit: string;
  nomCommercial?: string;
  description: string;
  typeProduit: TypeProduit;
  statutWorkflow?: StatutWorkflow;
  prixBase?: number;
  devisePrix?: string;
  couvertureGeographique?: CouvertureGeographique;
  territoiresExclus?: string[];
  version?: string;
  dateEffet?: string;
  dateExpiration?: string;
  dateCreation: string;
  dateModification: string;
  packs?: PackSimple[]; // Liste des packs associés
}

// Value Objects (remplacent les anciens champs plats — cf. Phase 1 backend)
export interface PlafondGarantie {
  plafondParActe?: number;
  plafondMensuel?: number;
  plafondAnnuel?: number;
  plafondGlobal?: number;
  plafondParSoins?: number;
  typePrincipal?: TypePlafond;
  description?: string;
  devise?: string;
}

export interface FranchiseGarantie {
  type?: TypeFranchise;
  montantFixe?: number;
  pourcentage?: number;
  montantMinimum?: number;
  montantMaximum?: number;
  description?: string;
  devise?: string;
}

export interface RegleCalcul {
  formule?: string;
  descriptionFormule?: string;
  parametresFormule?: string[];
  valeursDefaut?: Record<string, number>;
  prioriteCalcul?: number;
  appliquerPlafondApresCalcul?: boolean;
  deduireFranchiseAvantPlafond?: boolean;
  baseConventionnee?: boolean;
}

export interface Pack {
  idPack: string;
  codePack?: string;
  nomPack: string;
  nomCommercial?: string;
  description: string;
  descriptionCourte?: string;
  produitId: string;
  prixMensuel: number;
  prixAnnuel?: number;
  tauxRemiseAnnuelle?: number;
  devisePrix?: string;
  versionPack?: string;
  niveauCouverture?: NiveauCouverture;
  statutWorkflow?: StatutWorkflow;
  packRecommande?: boolean;
  colorTheme?: string;
  optionsDisponibles?: boolean;
  optionsPackIds?: string[];
  packsCompatibles?: string[];
  packsIncompatibles?: string[];
  dateEffet?: string;
  dateExpiration?: string;
  dateCreation: string;
  dateModification: string;
  garanties?: PackGarantie[]; // Liste des garanties associées
}

export interface Garantie {
  idGarantie: string;
  codeGarantie?: string;
  nomGarantie: string;
  nomCourt?: string;
  description: string;
  descriptionTechnique?: string;
  domaine?: DomaineMedical;
  garantieObligatoireParDefaut?: boolean;
  statutWorkflow?: StatutWorkflow;
  evenementsCouvertsParDefaut?: string[];
  typeRemboursement?: TypeRemboursement;
  tauxRemboursementBase?: number;
  tauxRemboursementMinimum?: number;
  tauxRemboursementMaximum?: number;
  plafond?: PlafondGarantie;
  franchise?: FranchiseGarantie;
  regleCalcul?: RegleCalcul;
  prerequisGarantieIds?: string[];
  parametresDynamiques?: Record<string, number>;
  primePureBase?: number;
  creePar?: string;
  dateCreation: string;
  dateModification: string;
  dateDesactivation?: string;
}

export interface PackGarantie {
  idPackGarantie: string;
  packId: string;
  garantieId: string;
  nomGarantie: string;
  codeGarantie?: string;
  tauxRemboursementSpecifique?: number;
  plafondSpecifique?: PlafondGarantie;
  franchiseSpecifique?: FranchiseGarantie;
  typeMontant?: TypeMontant;
  actif: boolean;
  dateActivation: string;
  dateDesactivation?: string;
  optionnelle: boolean;
  supplementPrix: number;
}

// Vues enrichies (endpoints /detail) — idPacks/idGaranties/domainesMedicaux calculés côté serveur
export interface ProduitDetail {
  produit: Produit;
  idPacks: string[];
}

export interface PackDetail {
  pack: Pack;
  idGaranties: string[];
  domainesMedicaux: DomaineMedical[];
}

//  INTERFACES API
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data?: T;
  error?: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}
//  FILTRES
export interface ProduitFilter {
  typeProduit?: TypeProduit;
  actif?: boolean;
  categorie?: string;
  prixMin?: number;
  prixMax?: number;
  ageMin?: number;
  ageMax?: number;
  searchTerm?: string;
}

export interface PackFilter {
  produitId?: string;
  niveauCouverture?: NiveauCouverture;
  actif?: boolean;
  prixMin?: number;
  prixMax?: number;
  dureeMin?: number;
  dureeMax?: number;
  searchTerm?: string;
}

export interface GarantieFilter {
  domaine?: DomaineMedical;
  searchTerm?: string;
}


export function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('fr-TN', {
    style: 'currency',
    currency: 'TND'
  }).format(amount);
}

export function formatDate(date: string): string {
  return new Date(date).toLocaleDateString('fr-TN');
}

export function formatDateTime(date: string): string {
  return new Date(date).toLocaleString('fr-TN');
}

export function getDomaineMedicalLabel(domaine: DomaineMedical): string {
  const labels: Record<DomaineMedical, string> = {
    CONSULTATION_GENERALE: 'Consultation générale',
    MEDECINE_FAMILIALE: 'Médecine familiale',
    MEDECINE_INTERNE: 'Médecine interne',
    MEDECINE_PREVENTIVE: 'Médecine préventive',
    CARDIOLOGIE: 'Cardiologie',
    DERMATOLOGIE: 'Dermatologie',
    ENDOCRINOLOGIE: 'Endocrinologie',
    GASTRO_ENTEROLOGIE: 'Gastro-entérologie',
    HEMATOLOGIE: 'Hématologie',
    INFECTIOLOGIE: 'Infectiologie',
    NEPHROLOGIE: 'Néphrologie',
    NEUROLOGIE: 'Neurologie',
    ONCOLOGIE: 'Oncologie',
    PNEUMOLOGIE: 'Pneumologie',
    RHUMATOLOGIE: 'Rhumatologie',
    ALLERGOLOGIE: 'Allergologie',
    GYNECOLOGIE: 'Gynécologie',
    OBSTETRIQUE: 'Obstétrique',
    FERTILITE_PMA: 'Fertilité/PMA',
    SUIVI_GROSSESSE: 'Suivi grossesse',
    PEDIATRIE: 'Pédiatrie',
    NEONATOLOGIE: 'Néonatologie',
    PEDOPSYCHIATRIE: 'Pédopsychiatrie',
    PSYCHIATRIE: 'Psychiatrie',
    PSYCHOLOGIE: 'Psychologie',
    PSYCHOTHERAPIE: 'Psychothérapie',
    ADDICTOLOGIE: 'Addictologie',
    CHIRURGIE_GENERALE: 'Chirurgie générale',
    CHIRURGIE_ESTHETIQUE: 'Chirurgie esthétique',
    CHIRURGIE_ORTHOPEDIQUE: 'Chirurgie orthopédique',
    NEUROCHIRURGIE: 'Neurochirurgie',
    CHIRURGIE_CARDIAQUE: 'Chirurgie cardiaque',
    CHIRURGIE_DIGESTIVE: 'Chirurgie digestive',
    CHIRURGIE_UROLOGIQUE: 'Chirurgie urologique',
    DENTISTERIE_GENERALE: 'Dentisterie générale',
    ORTHODONTIE: 'Orthodontie',
    IMPLANTOLOGIE: 'Implantologie',
    CHIRURGIE_DENTAIRE: 'Chirurgie dentaire',
    OPHTALMOLOGIE: 'Ophtalmologie',
    ORL: 'ORL',
    AUDIOLOGIE: 'Audiologie',
    KINESITHERAPIE: 'Kinésithérapie',
    PHYSIOTHERAPIE: 'Physiothérapie',
    ERGOTHERAPIE: 'Ergothérapie',
    ORTHOPHONIE: 'Orthophonie',
    CHIROPRATIE: 'Chiropratie',
    OSTEOPATHIE: 'Ostéopathie',
    RADIOLOGIE: 'Radiologie',
    IMAGERIE_MEDICALE: 'Imagerie médicale',
    ANALYSES_BIOLOGIQUES: 'Analyses biologiques',
    MEDECINE_NUCLEAIRE: 'Médecine nucléaire',
    MEDECINE_SPORT: 'Médecine du sport',
    MEDECINE_TRAVAIL: 'Médecine du travail',
    GERIATRIE: 'Gériatrie',
    NUTRITION_DIETETIQUE: 'Nutrition/Diététique',
    MEDECINE_ESTHETIQUE: 'Médecine esthétique',
    MEDECINE_ALTERNATIVE: 'Médecine alternative',
    URGENCES_MEDICALES: 'Urgences médicales',
    REANIMATION: 'Réanimation',
    SOINS_INTENSIFS: 'Soins intensifs',
    CONSULTATION_VIDEO: 'Consultation vidéo',
    SUIVI_DISTANCE: 'Suivi à distance',
    DEUXIEME_AVIS_MEDICAL: 'Deuxième avis médical',
    HOSPITALISATION: 'Hospitalisation',
    DENTAIRE: 'Dentaire',
    OPTIQUE: 'Optique',
    PHARMACIE: 'Pharmacie',
    MATERNITE: 'Maternité',
    PREVENTION: 'Prévention',
    SOINS_DENTAIRES: 'Soins dentaires',
    SOINS_OPTIQUES: 'Soins optiques',
    HOSPITALISATION_CHIRURGICALE: 'Hospitalisation chirurgicale',
    HOSPITALISATION_MEDICALE: 'Hospitalisation médicale',
    MEDECINE_GENERALE: 'Médecine générale',
    SPECIALITES_MEDICALES: 'Spécialités médicales',
    PARAMEDICAL: 'Paramédical',
    ANALYSES_MEDICALES: 'Analyses médicales',
    REPATRIEMENT: 'Rapatriement',
    DECES: 'Décès',
    INVALIDITE: 'Invalidité',
    AUTRE: 'Autre'
  };
  return labels[domaine] || domaine;
}

export function getTypeProduitLabel(type: TypeProduit): string {
  const labels: Record<TypeProduit, string> = {
    SANTE: 'Santé',
    HABITATION: 'Habitation',
    AUTO: 'Auto',
    EPARGNE: 'Épargne',
    VIE: 'Vie'
  };
  return labels[type] || type;
}

export function getNiveauCouvertureLabel(niveau: NiveauCouverture): string {
  const labels: Record<NiveauCouverture, string> = {
    BASIC: 'Basic',
    PREMIUM: 'Premium',
    GOLD: 'Gold'
  };
  return labels[niveau] || niveau;
}

export function getStatutWorkflowLabel(statut: StatutWorkflow): string {
  const labels: Record<StatutWorkflow, string> = {
    BROUILLON: 'Brouillon',
    SOUMIS_VALIDATION: 'Soumis pour validation',
    EN_COURS_VALIDATION: 'En cours de validation',
    APPROUVE: 'Approuvé',
    REJETE: 'Rejeté',
    PUBLIE: 'Publié',
    ARCHIVE: 'Archivé',
    SUSPENDU: 'Suspendu'
  };
  return labels[statut] || statut;
}

export function getStatutWorkflowBadgeVariant(statut: StatutWorkflow): 'primary' | 'secondary' | 'success' | 'warning' | 'error' | 'info' | 'neutral' {
  const variants: Record<StatutWorkflow, 'primary' | 'secondary' | 'success' | 'warning' | 'error' | 'info' | 'neutral'> = {
    BROUILLON: 'neutral',
    SOUMIS_VALIDATION: 'info',
    EN_COURS_VALIDATION: 'info',
    APPROUVE: 'secondary',
    REJETE: 'error',
    PUBLIE: 'success',
    ARCHIVE: 'neutral',
    SUSPENDU: 'warning'
  };
  return variants[statut] || 'neutral';
}

export function isStatutWorkflowOutlined(statut: StatutWorkflow): boolean {
  return statut === StatutWorkflow.ARCHIVE;
}

export function getCouvertureGeographiqueLabel(couverture: CouvertureGeographique): string {
  const labels: Record<CouvertureGeographique, string> = {
    LOCAL: 'Local',
    NATIONAL: 'National',
    INTERNATIONAL: 'International',
    UE: 'Union européenne',
    MAGHREB: 'Maghreb'
  };
  return labels[couverture] || couverture;
}