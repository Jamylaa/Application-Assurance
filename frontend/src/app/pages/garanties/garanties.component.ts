import { Component, OnInit } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { ConfirmationService } from 'primeng/api';
import { GestionProduitService, Garantie } from '../../services/gestion-produit.service';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { TooltipModule } from 'primeng/tooltip';
import { CommonModule } from '@angular/common';
import { ToastService } from '../../shared/services/toast.service';
import { BreadcrumbService } from '../../shared/services/breadcrumb.service';
import { UiBadgeComponent } from '../../shared/components/ui-badge/ui-badge.component';

import { getDomaineMedicalLabel } from '../../models/entities.model';
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
    CommonModule,
    RouterModule,
    UiBadgeComponent
  ],
  providers: [ConfirmationService]
})
export class GarantiesComponent implements OnInit {
  garanties: Garantie[] = [];
  loading = true;

  constructor(
    private readonly garantieService: GestionProduitService,
    private toastService: ToastService,
    private breadcrumbService: BreadcrumbService,
    private confirmationService: ConfirmationService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.breadcrumbService.setGarantiesBreadcrumb();
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
        this.loading = false;
      },
      error: (err) => {
        // Erreur chargement garanties handled
        this.loading = false;
        this.toastService.showLoadError('garanties');
      }
    });
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

  getSeverity(statut: string): 'success' | 'info' | 'secondary' | 'contrast' | 'warning' | 'danger' {
    switch (statut) {
      case 'ACTIF': return 'success';
      case 'INACTIF': return 'danger';
      default: return 'info';
    }
  }
  getStatusClass(statut: string): string {
    switch (statut?.toUpperCase()) {
      case 'ACTIF': return 'active';
      case 'INACTIF': return 'inactive';
      default: return 'unknown';
    }
  }

  getStatusBadgeVariant(statut: string): 'primary' | 'secondary' | 'success' | 'warning' | 'error' | 'info' | 'neutral' {
    switch (statut?.toUpperCase()) {
      case 'ACTIF': return 'success';
      case 'INACTIF': return 'error';
      default: return 'neutral';
    }
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
}
