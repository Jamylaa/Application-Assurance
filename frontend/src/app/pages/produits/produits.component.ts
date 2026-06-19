import { Component, OnInit } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { ConfirmationService } from 'primeng/api';
import { GestionProduitService, Produit } from '../../services/gestion-produit.service';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { BadgeModule } from 'primeng/badge';
import { TooltipModule } from 'primeng/tooltip';
import { CommonModule } from '@angular/common';
import { ToastService } from '../../shared/services/toast.service';
import { BreadcrumbService } from '../../shared/services/breadcrumb.service';

@Component({
  selector: 'app-produits',
  templateUrl: './produits.component.html',
  styleUrls: ['./produits.component.css'],
  standalone: true,
  imports: [
    TableModule,
    ButtonModule,
    CardModule,
    InputTextModule,
    ToastModule,
    ConfirmDialogModule,
    BadgeModule,
    TooltipModule,
    CommonModule,
    RouterModule
  ],
  providers: [ConfirmationService]
})
export class ProduitsComponent implements OnInit {
  produits: Produit[] = [];
  loading = false;
  globalFilterValue = '';

  constructor(
    private produitService: GestionProduitService,
    private toastService: ToastService,
    private breadcrumbService: BreadcrumbService,
    private confirmationService: ConfirmationService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.breadcrumbService.setProduitsBreadcrumb();
    this.loadProduits();
  }

  loadProduits(): void {
    this.loading = true;
    this.produitService.getAllProduits().subscribe({
      next: (data) => {
        this.produits = data;
        this.loading = false;
      },
      error: (error) => {
        this.loading = false;
        // Error loading produits handled
        this.toastService.showLoadError('produits');
      }
    });
  }

  onGlobalFilter(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.globalFilterValue = target.value;
  }

  refreshProduits(): void {
    this.loadProduits();
  }

  addProduit(): void {
    try {
      this.router.navigate(['/produits/add']);
    } catch (error) {
      this.toastService.showError('Erreur lors de l\'ajout du produit', 'Veuillez réessayer plus tard');
    }
  }

  getStatusClass(statut: string): string {
    switch (statut?.toUpperCase()) {
      case 'ACTIF': return 'active';
      case 'INACTIF': return 'inactive';
      default: return 'unknown';
    }
  }

  editProduit(produit: Produit): void {
    try {
      if (!produit || !produit.idProduit) {
        this.toastService.showWarning('Aucun produit sélectionné', 'Veuillez sélectionner un produit à modifier');
        return;
      }
      this.router.navigate(['/produits/edit', produit.idProduit]);
    } catch (error) {
      this.toastService.showError('Erreur lors de la modification du produit', 'Veuillez réessayer plus tard');
    }
  }

  viewProduit(produit: Produit): void {
    if (!produit?.idProduit) {
      this.toastService.showWarning('Aucun produit sélectionné', 'Veuillez sélectionner un produit à afficher');
      return;
    }
    this.router.navigate(['/produits', produit.idProduit]);
  }

  deleteProduit(produit: Produit): void {
    if (!produit || !produit.idProduit) {
      this.toastService.showWarning('Aucun produit sélectionné', 'Veuillez sélectionner un produit à supprimer');
      return;
    }

    this.confirmationService.confirm({
      message: 'Êtes-vous sûr de vouloir supprimer ce produit ?',
      header: 'Confirmation de suppression',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.produitService.deleteProduit(produit.idProduit!).subscribe({
          next: () => {
            this.toastService.showDeleteSuccess('Produit');
            this.loadProduits();
          },
          error: (err) => {
            // Erreur suppression produit handled
            this.toastService.showDeleteError('produit');
          }
        });
      }
    });
  }
}
