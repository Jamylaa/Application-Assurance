import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  
  Garantie as GarantieModel,
  NiveauCouverture,
  Pack as PackModel,
  PackGarantie,
  Produit as ProduitModel,
  Statut,
  TypeClient,
  TypeGarantie,
  TypeProduit
} from '../models/entities.model';

export type Produit = ProduitModel;
export type Pack = PackModel;
export type Garantie = GarantieModel;

@Injectable({
  providedIn: 'root'
})
export class GestionProduitService {
  private readonly apiUrl = `${environment.apiProduit}`;
  private readonly packsUrl = `${this.apiUrl}/packs`;
  private readonly produitsUrl = `${this.apiUrl}/produits`;
  private readonly garantiesUrl = `${this.apiUrl}/garanties`;

  constructor(private http: HttpClient) {}


  // Gestion des produits
  getAllProduits(): Observable<Produit[]> {
    return this.http.get<Produit[]>(this.produitsUrl);
  }

  getProduitById(idProduit: string): Observable<Produit> {
    return this.http.get<Produit>(`${this.produitsUrl}/${idProduit}`);
  }

  getProduitsByType(typeProduit: TypeProduit | string): Observable<Produit[]> {
    return this.http.get<Produit[]>(`${this.produitsUrl}/type/${typeProduit}`);
  }

  getProduitsByStatut(statut: Statut | string): Observable<Produit[]> {
    return this.http.get<Produit[]>(`${this.produitsUrl}/statut/${statut}`);
  }

  searchProduits(nomProduit: string): Observable<Produit[]> {
    return this.http.get<Produit[]>(`${this.produitsUrl}/search?nom=${encodeURIComponent(nomProduit)}`);
  }

  createProduit(produit: Produit): Observable<Produit> {
    return this.http.post<Produit>(this.produitsUrl, produit);
  }

  updateProduit(idProduit: string, produit: Produit): Observable<Produit> {
    return this.http.put<Produit>(`${this.produitsUrl}/${idProduit}`, produit);
  }

  desactiverProduit(idProduit: string): Observable<Produit> {
    return this.http.patch<Produit>(`${this.produitsUrl}/${idProduit}/desactiver`, {});
  }

  deleteProduit(idProduit: string): Observable<void> {
    return this.http.delete<void>(`${this.produitsUrl}/${idProduit}`);
  }

  // Gestion des packs 
  getAllPacks(): Observable<Pack[]> {
    return this.http.get<Pack[]>(this.packsUrl);
  }

  getPackById(idPack: string): Observable<Pack> {
    return this.http.get<Pack>(`${this.packsUrl}/${idPack}`);
  }

  getPacksByProduit(produitId: string): Observable<Pack[]> {
    // Backend expose les deux routes : /produit/{produitId} (alias) et /by-produit/{produitId}
    return this.http.get<Pack[]>(`${this.packsUrl}/produit/${produitId}`);
  }

  getPacksByStatut(statut: Statut | string): Observable<Pack[]> {
    return this.http.get<Pack[]>(`${this.packsUrl}/statut/${statut}`);
  }

  getPacksByNiveau(niveauCouverture: NiveauCouverture | string): Observable<Pack[]> {
    return this.http.get<Pack[]>(`${this.packsUrl}/niveau/${niveauCouverture}`);
  }

  getPacksByTypeClient(typeClient: TypeClient | string): Observable<Pack[]> {
    return this.http.get<Pack[]>(`${this.packsUrl}/type-client/${typeClient}`);
  }

  searchPacks(nomPack: string): Observable<Pack[]> {
    return this.http.get<Pack[]>(`${this.packsUrl}/search?nomPack=${encodeURIComponent(nomPack)}`);
  }

  getPacksByPrixRange(prixMin: number, prixMax: number): Observable<Pack[]> {
    return this.http.get<Pack[]>(`${this.packsUrl}/prix-range?prixMin=${prixMin}&prixMax=${prixMax}`);
  }

  createPack(pack: Pack): Observable<Pack> {
    return this.http.post<Pack>(this.packsUrl, pack);
  }

  updatePack(idPack: string, pack: Pack): Observable<Pack> {
    return this.http.put<Pack>(`${this.packsUrl}/${idPack}`, pack);
  }

  desactiverPack(idPack: string): Observable<Pack> {
    return this.http.patch<Pack>(`${this.packsUrl}/${idPack}/desactiver`, {});
  }

  deletePack(idPack: string): Observable<void> {
    return this.http.delete<void>(`${this.packsUrl}/${idPack}`);
  }

  // Gestion des garanties 
  getAllGaranties(): Observable<Garantie[]> {
    return this.http.get<Garantie[]>(this.garantiesUrl);
  }

  getGarantieById(idGarantie: string): Observable<Garantie> {
    return this.http.get<Garantie>(`${this.garantiesUrl}/${idGarantie}`);
  }

  getGarantiesByType(typeGarantie: TypeGarantie | string): Observable<Garantie[]> {
    return this.http.get<Garantie[]>(`${this.garantiesUrl}/type/${typeGarantie}`);
  }

  getGarantiesByStatut(statut: Statut | string): Observable<Garantie[]> {
    return this.http.get<Garantie[]>(`${this.garantiesUrl}/statut/${statut}`);
  }

  searchGaranties(nomGarantie: string): Observable<Garantie[]> {
    return this.http.get<Garantie[]>(`${this.garantiesUrl}/search?nomGarantie=${encodeURIComponent(nomGarantie)}`);
  }

  getGarantiesByTauxMin(tauxMin: number): Observable<Garantie[]> {
    return this.http.get<Garantie[]>(`${this.garantiesUrl}/taux-min/${tauxMin}`);
  }

  getGarantiesByPlafondMin(plafondMin: number): Observable<Garantie[]> {
    return this.http.get<Garantie[]>(`${this.garantiesUrl}/plafond-min/${plafondMin}`);
  }

  createGarantie(garantie: Garantie): Observable<Garantie> {
    return this.http.post<Garantie>(this.garantiesUrl, garantie);
  }

  updateGarantie(idGarantie: string, garantie: Garantie): Observable<Garantie> {
    return this.http.put<Garantie>(`${this.garantiesUrl}/${idGarantie}`, garantie);
  }

  desactiverGarantie(idGarantie: string): Observable<Garantie> {
    return this.http.patch<Garantie>(`${this.garantiesUrl}/${idGarantie}/desactiver`, {});
  }

  deleteGarantie(idGarantie: string): Observable<void> {
    return this.http.delete<void>(`${this.garantiesUrl}/${idGarantie}`);
  }

  // Gestion de la configuration des packs (Pack-Garantie)
  
  // READ
  getGarantiesDuPack(packId: string): Observable<PackGarantie[]> {
    return this.http.get<PackGarantie[]>(`${this.packsUrl}/${packId}/garanties`);
  }

  getGarantiesInclusesDuPack(packId: string): Observable<PackGarantie[]> {
    return this.http.get<PackGarantie[]>(`${this.packsUrl}/${packId}/garanties/incluses`);
  }

  getGarantiesOptionnellesDuPack(packId: string): Observable<PackGarantie[]> {
    return this.http.get<PackGarantie[]>(`${this.packsUrl}/${packId}/garanties/optionnelles`);
  }

  getGarantiesDisponiblesPourPack(packId: string): Observable<Garantie[]> {
    return this.http.get<Garantie[]>(`${this.packsUrl}/${packId}/garanties-disponibles`);
  }

  calculerPrixTotalPack(packId: string): Observable<number> {
    return this.http.get<number>(`${this.packsUrl}/${packId}/prix-total`);
  }

  // CREATE
  ajouterGarantieAuPack(packId: string, garantieId: string, packGarantie: PackGarantie): Observable<PackGarantie> {
    return this.http.post<PackGarantie>(`${this.packsUrl}/${packId}/garanties/${garantieId}`, packGarantie);
  }

  // creerPackAvecGaranties(request: CreatePackWithGarantiesRequest): Observable<Pack> {
  //   return this.http.post<Pack>(`${this.apiUrl}/pack-configuration/avec-garanties-complet`, request);
  // }

  // UPDATE
  modifierGarantieDansPack(_packId: string, packGarantieId: string, details: PackGarantie): Observable<PackGarantie> {
    return this.http.put<PackGarantie>(`${this.packsUrl}/associations/${packGarantieId}`, details);
  }

  // DELETE
  supprimerGarantieDuPack(_packId: string, packGarantieId: string): Observable<void> {
    return this.http.delete<void>(`${this.packsUrl}/associations/${packGarantieId}`);
  }

  // Endpoints additionnels exposés par PackUnifiedController
  getAllPackGaranties(): Observable<PackGarantie[]> {
    return this.http.get<PackGarantie[]>(`${this.packsUrl}/associations`);
  }

  toggleGarantieActivation(packGarantieId: string, active: boolean): Observable<PackGarantie> {
    return this.http.patch<PackGarantie>(`${this.packsUrl}/associations/${packGarantieId}/activation?active=${active}`, {});
  }

  associatePackToProduit(packId: string, produitId: string): Observable<Pack> {
    return this.http.post<Pack>(`${this.packsUrl}/${packId}/associate-produit/${produitId}`, {});
  }

  dissociatePackFromProduit(packId: string): Observable<Pack> {
    return this.http.delete<Pack>(`${this.packsUrl}/${packId}/dissociate-produit`);
  }

  packsHealth(): Observable<Record<string, unknown>> {
    return this.http.get<Record<string, unknown>>(`${this.packsUrl}/health`);
  }

  packsStatistics(): Observable<Record<string, unknown>> {
    return this.http.get<Record<string, unknown>>(`${this.packsUrl}/statistics`);
  }
}
