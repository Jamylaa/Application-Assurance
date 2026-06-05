import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { GestionProduitService, Produit } from '../../services/gestion-produit.service';
import { ToastService } from '../../shared/services/toast.service';
import { BreadcrumbService } from '../../shared/services/breadcrumb.service';

@Component({
  selector: 'app-produit-details',
  standalone: true,
  imports: [CommonModule, RouterModule, ProgressSpinnerModule, CardModule, ButtonModule, TagModule, ToastModule],
  templateUrl: './produit-details.component.html',
  styleUrls: ['./produit-details.component.css']
})
export class ProduitDetailsComponent implements OnInit {
  loading = true;
  produit?: Produit;
  produitId?: string;

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
      },
      error: () => {
        this.loading = false;
        this.toastService.showError('Impossible de charger le produit');
      }
    });
  }

  back(): void {
    this.router.navigate(['/produits']);
  }

  edit(): void {
    if (!this.produitId) return;
    this.router.navigate(['/produits/edit', this.produitId]);
  }

  getStatusSeverity(statut?: string): 'success' | 'danger' | 'info' | 'secondary' {
    switch ((statut || '').toUpperCase()) {
      case 'ACTIF':
        return 'success';
      case 'INACTIF':
        return 'danger';
      default:
        return 'info';
    }
  }

  formatDateTime(value?: string): string {
    if (!value) return '—';
    try {
      return new Date(value).toLocaleString('fr-TN');
    } catch (e) {
      return value;
    }
  }
}
