import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ConfirmationService } from 'primeng/api';
import { GestionProduitService, Garantie } from '../../services/gestion-produit.service';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { TooltipModule } from 'primeng/tooltip';
import { DropdownModule } from 'primeng/dropdown';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ToastService } from '../../shared/services/toast.service';
import { BreadcrumbService } from '../../shared/services/breadcrumb.service';
import { UiBadgeComponent } from '../../shared/components/ui-badge/ui-badge.component';

import { getDomaineMedicalLabel, DomaineMedical, StatutWorkflow, getStatutWorkflowLabel, getStatutWorkflowBadgeVariant, isStatutWorkflowOutlined } from '../../models/entities.model';
@Component({
  selector: 'app-garanties',
  templateUrl: './garanties.component.html',
  styleUrls: ['./garanties.component.css'],
  standalone: true,
  imports: [
    TableModule,
    ButtonModule,
    CardModule,
    TagModule,
    ToastModule,
    ConfirmDialogModule,
    TooltipModule,
    DropdownModule,
    CommonModule,
    FormsModule,
    RouterModule,
    UiBadgeComponent
  ],
  providers: [ConfirmationService]
})
export class GarantiesComponent implements OnInit {
  garanties: Garantie[] = [];
  filteredGaranties: Garantie[] = [];
  loading = true;

  // Filter properties
  domaineOptions: { label: string; value: DomaineMedical }[] = Object.values(DomaineMedical).map(d => ({
    label: getDomaineMedicalLabel(d),
    value: d
  }));
  selectedDomaine: DomaineMedical | null = null;
  globalFilterValue = '';

  readonly tableRows = 10;
  highlightedGarantieId: string | null = null;
  tableFirst = 0;

  constructor(
    private readonly garantieService: GestionProduitService,
    private toastService: ToastService,
    private breadcrumbService: BreadcrumbService,
    private confirmationService: ConfirmationService,
    private router: Router,
    private readonly route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.breadcrumbService.setGarantiesBreadcrumb();
    this.highlightedGarantieId = this.route.snapshot.queryParamMap.get('highlight');
    this.loadGaranties();
  }

  loadGaranties(): void {
    this.loading = true;
    this.garantieService.getAllGaranties().subscribe({
      next: (data) => {
        // Nettoyer les noms des garanties (supprimer "nommee" si présent)
        this.garanties = data.map(g => ({
          ...g,
          nomGarantie: this.cleanGarantieName(g.nomGarantie)
        }));
        this.applyFilters();
        this.loading = false;
        if (this.highlightedGarantieId) {
          this.revealHighlightedGarantie();
        }
      },
      error: (err) => {
        // Erreur chargement garanties handled
        this.loading = false;
        this.toastService.showLoadError('garanties');
      }
    });
  }

  // Navigue vers la page du tableau contenant la garantie ciblée (queryParam highlight),
  // puis scrolle jusqu'à sa ligne et lui applique une classe .highlighted — sans filtrer
  // la liste, la garantie reste visible dans son contexte complet.
  private revealHighlightedGarantie(): void {
    const index = this.filteredGaranties.findIndex(g => g.idGarantie === this.highlightedGarantieId);
    if (index === -1) {
      return;
    }
    this.tableFirst = Math.floor(index / this.tableRows) * this.tableRows;
    setTimeout(() => {
      const row = document.querySelector(`[data-garantie-id="${CSS.escape(this.highlightedGarantieId!)}"]`);
      row?.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }, 150);
  }

  private cleanGarantieName(nom: string): string {
    if (!nom) return nom;
    // Supprimer "nommee " au début du nom
    if (nom.toLowerCase().startsWith('nommee ')) {
      return nom.substring(7).trim();
    }
    return nom;
  }

  viewGarantie(garantie: Garantie): void {
    if (!garantie?.idGarantie) {
      this.toastService.showWarning('Aucune garantie sélectionnée', 'Veuillez sélectionner une garantie à afficher');
      return;
    }
    this.router.navigate(['/garanties', garantie.idGarantie]);
  }

  deleteGarantie(garantieId: string): void {
    this.confirmationService.confirm({
      message: 'Êtes-vous sûr de vouloir supprimer cette garantie ?',
      header: 'Confirmation de suppression',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.garantieService.deleteGarantie(garantieId).subscribe({
          next: () => {
            this.toastService.showDeleteSuccess('Garantie');
            this.loadGaranties();
          },
          error: (err) => {
            // Erreur suppression garantie handled
            this.toastService.showDeleteError('garantie');
          }
        });
      }
    });
  }

  getDomaineOrTypeLabel(garantie: Garantie): string {
    if (garantie.domaine) {
      return getDomaineMedicalLabel(garantie.domaine);
    }
    return '—';
  }

  getTypeMontantLabel(typeMontant: string): string {
    switch (typeMontant) {
      case 'FORFAIT': return 'Forfait';
      case 'TARIF_CONVENTIONNE': return 'Tarif conventionné';
      case 'FRAIS_REELS': return 'Frais réels';
      case 'FORFAITAIRE': return 'Forfaitaire';
      case 'POURCENTAGE': return '%';
      default: return typeMontant || '—';
    }
  }

  getTypePlafondLabel(typePlafond: string): string {
    switch (typePlafond) {
      case 'ANNUEL': return 'Annuel';
      case 'MENSUEL': return 'Mensuel';
      case 'PAR_ACTE': return 'Par acte';
      default: return '—';
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

  addGarantie(): void {
    try {
      this.router.navigate(['/garanties/add']);
    } catch (error) {
      this.toastService.showError('Erreur', 'Impossible d\'ajouter la garantie');
    }
  }

  editGarantie(garantie: Garantie): void {
    try {
      if (!garantie || !garantie.idGarantie) {
        this.toastService.showWarning('Aucune garantie sélectionnée', 'Veuillez sélectionner une garantie à modifier');
        return;
      }
      this.router.navigate(['/garanties/edit', garantie.idGarantie]);
    } catch (error) {
      this.toastService.showError('Erreur lors de la modification de la garantie', 'Veuillez réessayer plus tard');
    }
  }

  // Filter methods
  applyFilters(): void {
    const searchLower = this.globalFilterValue.trim().toLowerCase();
    this.filteredGaranties = this.garanties.filter(g => {
      if (this.selectedDomaine && g.domaine !== this.selectedDomaine) {
        return false;
      }
      if (searchLower && !(g.nomGarantie || '').toLowerCase().includes(searchLower)
          && !(g.description || '').toLowerCase().includes(searchLower)) {
        return false;
      }
      return true;
    });
  }

  resetFilters(): void {
    this.selectedDomaine = null;
    this.globalFilterValue = '';
    this.applyFilters();
  }

  hasActiveFilters(): boolean {
    return !!this.selectedDomaine || !!this.globalFilterValue;
  }

  clearDomaineFilter(): void {
    this.selectedDomaine = null;
    this.applyFilters();
  }

  clearSearchFilter(): void {
    this.globalFilterValue = '';
    this.applyFilters();
  }

  onGlobalFilter(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.globalFilterValue = target.value;
    this.applyFilters();
  }
}
