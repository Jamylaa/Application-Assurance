
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

export enum TypeGarantie {
  HOSPITALISATION = 'HOSPITALISATION',
  DENTAIRE = 'DENTAIRE',
  OPTIQUE = 'OPTIQUE',
  CONSULTATION = 'CONSULTATION'
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
  packs?: Pack[];
  customFields?: Record<string, unknown>;
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
  niveauCouverture: NiveauCouverture;
  statut: Statut;
  customFields?: Record<string, unknown>;
  dateCreation: string;
  dateModification: string;
}

export interface Garantie {
  idGarantie: string;
  nomGarantie: string;
  description: string;
  statut: Statut;
  type: string;
  tauxRemboursement: number;
  typeMontant: TypeMontant;
  typePlafond: TypePlafond;
  plafondAnnuel: number;
  plafondMensuel: number;
  plafondParActe: number;
  franchise: number;
  coutMoyenParSinistre: number;
  dureeMinContrat: number;
  dureeMaxContrat: number;
  resiliableAnnuellement: boolean;
  creePar: string;
  customFields?: Record<string, unknown>;
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
  typeMontant: TypeMontant;
  delaiCarence: number;
  priorite: number;
  actif: boolean;
  customFields?: Record<string, unknown>;
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
  type?: string;
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
