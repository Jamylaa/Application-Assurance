import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { GestionProduitService, Produit } from '../../services/gestion-produit.service';
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
  productRating: number = 0;

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
        this.calculateMetrics();
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toastService.showError('Impossible de charger le produit');
      }
    });
  }

  private calculateMetrics(): void {
    if (!this.produit) return;

    // Calculer le nombre de packs (simulé pour l'instant)
    this.packsCount = this.produit.idProduit ? Math.floor(Math.random() * 8) + 2 : 0;

    // Calculer une note basée sur le type de produit
    const typeScore = this.produit.typeProduit === 'SANTE' ? 100 :
                     this.produit.typeProduit === 'VIE' ? 90 :
                     this.produit.typeProduit === 'HABITATION' ? 85 : 70;
    this.productRating = typeScore;
  }

  back(): void {
    this.router.navigate(['/produits']);
  }

  edit(): void {
    if (!this.produitId) return;
    this.router.navigate(['/produits/edit', this.produitId]);
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
