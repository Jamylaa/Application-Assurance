import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { GestionProduitService, Produit, Pack } from '../../services/gestion-produit.service';
import { ToastService } from '../../shared/services/toast.service';
import { BreadcrumbService } from '../../shared/services/breadcrumb.service';
import { UiBadgeComponent } from '../../shared/components/ui-badge/ui-badge.component';
import {
  StatutWorkflow,
  CouvertureGeographique,
  getStatutWorkflowLabel,
  getStatutWorkflowBadgeVariant,
  isStatutWorkflowOutlined,
  getCouvertureGeographiqueLabel,
  formatDate
} from '../../models/entities.model';

@Component({
  selector: 'app-produit-details',
  standalone: true,
  imports: [CommonModule, RouterModule, ProgressSpinnerModule, CardModule, ButtonModule, ToastModule, UiBadgeComponent],
  templateUrl: './produit-details.component.html',
  styleUrls: ['./produit-details.component.css']
})
export class ProduitDetailsComponent implements OnInit {
  loading = true;
  produit?: Produit;
  produitId?: string;
  packsCount: number = 0;
  packs: Pack[] = [];
  loadingPacks = false;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly produitService: GestionProduitService,
    private readonly toastService: ToastService,
    private readonly breadcrumbService: BreadcrumbService
  ) {}

  ngOnInit(): void {
    this.produitId = this.route.snapshot.paramMap.get('idProduit') || undefined;
    this.breadcrumbService.setBreadcrumb([
      { label: 'Accueil', routerLink: ['/dashboard'], icon: 'pi pi-home' },
      { label: 'Gestion', icon: 'pi pi-cog' },
      { label: 'Produits', routerLink: ['/produits'], icon: 'pi pi-box' },
      { label: 'Détails', icon: 'pi pi-info-circle' }
    ]);

    if (!this.produitId) {
      this.loading = false;
      this.toastService.showError('Identifiant produit manquant');
      return;
    }

    this.loadProduit(this.produitId);
  }

  private loadProduit(idProduit: string): void {
    this.loading = true;
    this.produitService.getProduitById(idProduit).subscribe({
      next: (p) => {
        this.produit = p;
        this.loading = false;
        this.loadPacks(idProduit);
      },
      error: () => {
        this.loading = false;
        this.toastService.showError('Impossible de charger le produit');
      }
    });
  }

  private loadPacks(idProduit: string): void {
    this.loadingPacks = true;
    this.produitService.getPacksByProduit(idProduit).subscribe({
      next: (packs) => {
        this.packs = packs;
        this.packsCount = packs.length;
        this.loadingPacks = false;
      },
      error: () => {
        this.packs = [];
        this.packsCount = 0;
        this.loadingPacks = false;
      }
    });
  }

  goToPack(idPack: string): void {
    this.router.navigate(['/packs', idPack]);
  }

  back(): void {
    this.router.navigate(['/produits']);
  }

  edit(): void {
    if (!this.produitId) return;
    this.router.navigate(['/produits/edit', this.produitId]);
  }

  publier(): void {
    if (!this.produitId) return;
    this.produitService.publierProduit(this.produitId).subscribe({
      next: (p) => {
        this.produit = p;
        this.toastService.showSuccess('Produit publié avec succès');
      },
      error: () => {
        this.toastService.showError('Impossible de publier ce produit (statut actuel incompatible)');
      }
    });
  }

  canPublier(): boolean {
    return !!this.produit && this.produit.statutWorkflow !== StatutWorkflow.PUBLIE;
  }

  getTypeClass(type?: string): string {
    switch (type?.toUpperCase()) {
      case 'SANTE': return 'sante';
      case 'VIE': return 'vie';
      case 'HABITATION': return 'habitation';
      case 'AUTO': return 'auto';
      case 'EPARGNE': return 'epargne';
      default: return 'unknown';
    }
  }

  getStatutLabel(statut?: StatutWorkflow): string {
    return statut ? getStatutWorkflowLabel(statut) : '—';
  }

  getStatutBadgeVariant(statut?: StatutWorkflow): 'primary' | 'secondary' | 'success' | 'warning' | 'error' | 'info' | 'neutral' {
    return statut ? getStatutWorkflowBadgeVariant(statut) : 'neutral';
  }

  isStatutOutlined(statut?: StatutWorkflow): boolean {
    return statut ? isStatutWorkflowOutlined(statut) : false;
  }

  getCouvertureLabel(couverture?: CouvertureGeographique): string {
    return couverture ? getCouvertureGeographiqueLabel(couverture) : '—';
  }

  formatDateValue(date?: string): string {
    return date ? formatDate(date) : '—';
  }

  getProductCategory(): string {
    if (!this.produit) return '—';
    switch (this.produit.typeProduit?.toUpperCase()) {
      case 'SANTE': return 'Assurance Santé';
      case 'VIE': return 'Assurance Vie';
      case 'HABITATION': return 'Assurance Habitation';
      case 'AUTO': return 'Assurance Automobile';
      case 'EPARGNE': return 'Épargne & Placement';
      default: return 'Autre';
    }
  }
}
