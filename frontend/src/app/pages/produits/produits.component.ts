import { Component, OnInit } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { ConfirmationService } from 'primeng/api';
import { GestionProduitService, Produit, Pack } from '../../services/gestion-produit.service';
import { TypeProduit, getTypeProduitLabel, StatutWorkflow, getStatutWorkflowLabel, getStatutWorkflowBadgeVariant, isStatutWorkflowOutlined } from '../../models/entities.model';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { BadgeModule } from 'primeng/badge';
import { TooltipModule } from 'primeng/tooltip';
import { DropdownModule } from 'primeng/dropdown';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ToastService } from '../../shared/services/toast.service';
import { BreadcrumbService } from '../../shared/services/breadcrumb.service';
import { UiBadgeComponent } from '../../shared/components/ui-badge/ui-badge.component';

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
    DropdownModule,
    CommonModule,
    FormsModule,
    RouterModule,
    UiBadgeComponent
  ],
  providers: [ConfirmationService]
})
export class ProduitsComponent implements OnInit {
  produits: Produit[] = [];
  filteredProduits: Produit[] = [];
  loading = false;
  globalFilterValue = '';
  packsByProduit: Map<string, { packs: Pack[]; loading: boolean; show: boolean }> = new Map();

  // Filter properties
  typeOptions: { label: string; value: TypeProduit }[] = Object.values(TypeProduit).map(t => ({
    label: getTypeProduitLabel(t),
    value: t
  }));
  selectedType: TypeProduit | null = null;

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
        this.applyFilters();
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
    this.applyFilters();
  }

  // Filter methods
  applyFilters(): void {
    const searchLower = this.globalFilterValue.trim().toLowerCase();
    this.filteredProduits = this.produits.filter(p => {
      if (this.selectedType && p.typeProduit !== this.selectedType) {
        return false;
      }
      if (searchLower && !(p.nomProduit || '').toLowerCase().includes(searchLower)
          && !(p.description || '').toLowerCase().includes(searchLower)) {
        return false;
      }
      return true;
    });
  }

  // Relation Produit -> Packs (pattern identique à PacksComponent.loadGarantiesForPack)
  loadPacksForProduit(produitId: string): void {
    if (!produitId || this.packsByProduit.has(produitId)) {
      return;
    }

    this.packsByProduit.set(produitId, { packs: [], loading: true, show: false });

    this.produitService.getPacksByProduit(produitId).subscribe({
      next: (packs) => {
        this.packsByProduit.set(produitId, { packs, loading: false, show: false });
      },
      error: () => {
        this.packsByProduit.set(produitId, { packs: [], loading: false, show: false });
      }
    });
  }

  togglePacks(produit: Produit): void {
    const produitId = produit.idProduit;
    if (!produitId) return;

    if (!this.packsByProduit.has(produitId)) {
      this.loadPacksForProduit(produitId);
    }

    const current = this.packsByProduit.get(produitId);
    if (current) {
      current.show = !current.show;
      this.packsByProduit.set(produitId, current);
    }
  }

  getProduitPacks(produitId: string): { packs: Pack[]; loading: boolean; show: boolean } {
    return this.packsByProduit.get(produitId) || { packs: [], loading: false, show: false };
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

  resetFilters(): void {
    this.selectedType = null;
    this.globalFilterValue = '';
    this.applyFilters();
  }

  hasActiveFilters(): boolean {
    return !!this.selectedType || !!this.globalFilterValue;
  }

  clearTypeFilter(): void {
    this.selectedType = null;
    this.applyFilters();
  }

  clearSearchFilter(): void {
    this.globalFilterValue = '';
    this.applyFilters();
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
