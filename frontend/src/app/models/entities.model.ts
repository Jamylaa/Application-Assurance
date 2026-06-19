
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

export enum Statut {
  ACTIF = 'ACTIF',
  INACTIF = 'INACTIF',
  EN_ATTENTE = 'EN_ATTENTE',
  SUSPENDU = 'SUSPENDU',
  EXPIRE = 'EXPIRE',
  RESILIE = 'RESILIE'
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

// === ENTITÉS PRINCIPALES ===

export interface Produit {
  idProduit: string;
  nomProduit: string;
  description: string;
  typeProduit: TypeProduit;
  statut: Statut;
  dateCreation: string;
  dateModification: string;
}

export interface Pack {
  idPack: string;
  nomPack: string;
  description: string;
  produitId: string;
  nomProduit: string;
  ageMinimum?: number;
  ageMaximum?: number;
  typeClients: TypeClient[];
  ancienneteContratMois: number;
  couvertureGeographique: CouvertureGeographique;
  prixMensuel: number;
  dureeMinContrat: number;
  dureeMaxContrat: number;
  niveauCouverture?: NiveauCouverture;
  statut: Statut;
  domainesMedicaux?: string[];
  dateCreation: string;
  dateModification: string;
}

export interface Garantie {
  idGarantie: string;
  nomGarantie: string;
  description: string;
  statut: Statut;
  domaine?: DomaineMedical;
  tauxRemboursement?: number;
  typeMontant?: TypeMontant;
  typePlafond?: TypePlafond;
  plafondAnnuel?: number;
  plafondMensuel?: number;
  plafondParActe?: number;
  franchise?: number;
  coutMoyenParSinistre?: number;
  dureeMinContrat?: number;
  dureeMaxContrat?: number;
  resiliableAnnuellement?: boolean;
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
  tauxRemboursement: number;
  plafond: number;
  franchise: number;
  typeMontant?: TypeMontant;
  delaiCarence: number;
  priorite: number;
  actif: boolean;
  dateActivation: string;
  dateDesactivation?: string;
  optionnelle: boolean;
  supplementPrix: number;
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
  statut?: Statut;
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