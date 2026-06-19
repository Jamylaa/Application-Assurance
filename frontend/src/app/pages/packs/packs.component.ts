import { Component, OnInit } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { ConfirmationService } from 'primeng/api';
import { GestionProduitService, Pack } from '../../services/gestion-produit.service';
import { PackGarantie } from '../../models/entities.model';
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

@Component({
  selector: 'app-packs',
  templateUrl: './packs.component.html',
  styleUrls: ['./packs.component.css'],
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
export class PacksComponent implements OnInit {
  packs: Pack[] = [];
  loading = true;
  packsWithGaranties: Map<string, { garanties: PackGarantie[]; loading: boolean; show: boolean }> = new Map();

  constructor(
    private readonly packService: GestionProduitService,
    private toastService: ToastService,
    private breadcrumbService: BreadcrumbService,
    private confirmationService: ConfirmationService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.breadcrumbService.setPacksBreadcrumb();
    this.loadPacks();
  }

  loadPacks(): void {
    this.loading = true;
    this.packService.getAllPacks().subscribe({
      next: (data) => {
        this.packs = data;
        this.loading = false;
      },
      error: (err) => {
        // Erreur chargement packs handled
        this.loading = false;
        this.toastService.showLoadError('packs');
      }
    });
  }

  viewPack(packId: string): void {
    if (!packId) {
      this.toastService.showWarning('Aucun pack sélectionné', 'Veuillez sélectionner un pack à afficher');
      return;
    }
    this.router.navigate(['/packs', packId]);
  }

  deletePack(idPack: string): void {
    this.confirmationService.confirm({
      message: 'Êtes-vous sûr de vouloir supprimer ce pack ?',
      header: 'Confirmation de suppression',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.packService.deletePack(idPack).subscribe({
          next: () => {
            this.toastService.showDeleteSuccess('Pack');
            this.loadPacks();
          },
          error: (err) => {
            // Erreur suppression pack handled
            this.toastService.showDeleteError('pack');
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

  getNiveauSeverity(niveau: string): 'success' | 'info' | 'secondary' | 'contrast' | 'warning' | 'danger' {
    switch (niveau?.toLowerCase()) {
      case 'premium': case 'gold': case 'platinum': return 'success';
      case 'standard': case 'silver': return 'info';
      case 'basic': case 'bronze': return 'warning';
      default: return 'secondary';
    }
  }

  getStatusClass(statut: string): string {
    switch (statut?.toUpperCase()) {
      case 'ACTIF': return 'active';
      case 'INACTIF': return 'inactive';
      default: return 'unknown';
    }
  }

  getNiveauClass(niveau: string): string {
    switch (niveau?.toLowerCase()) {
      case 'premium': case 'gold': case 'platinum': return 'gold';
      case 'standard': case 'silver': return 'silver';
      case 'basic': case 'bronze': return 'bronze';
      default: return 'default';
    }
  }

  getStatusBadgeVariant(statut: string): 'primary' | 'secondary' | 'success' | 'warning' | 'error' | 'info' | 'neutral' {
    switch (statut?.toUpperCase()) {
      case 'ACTIF': return 'success';
      case 'INACTIF': return 'error';
      default: return 'neutral';
    }
  }

  getNiveauBadgeVariant(niveau: string): 'primary' | 'secondary' | 'success' | 'warning' | 'error' | 'info' | 'neutral' {
    switch (niveau?.toLowerCase()) {
      case 'premium': case 'gold': case 'platinum': return 'success';
      case 'standard': case 'silver': return 'info';
      case 'basic': case 'bronze': return 'warning';
      default: return 'neutral';
    }
  }

  addPack(): void {
    try {
      this.router.navigate(['/packs/add']);
    } catch (error) {
      this.toastService.showError('Erreur lors de l\'ajout du pack', 'Veuillez réessayer plus tard');
    }
  }

  editPack(packId: string): void {
    try {
      if (!packId) {
        this.toastService.showWarning('Aucun pack sélectionné', 'Veuillez sélectionner un pack à modifier');
        return;
      }
      this.router.navigate(['/packs/edit', packId]);
    } catch (error) {
      this.toastService.showError('Erreur lors de la modification du pack', 'Veuillez réessayer plus tard');
    }
  }

  loadGarantiesForPack(packId: string): void {
    if (!packId || this.packsWithGaranties.has(packId)) {
      return;
    }

    this.packsWithGaranties.set(packId, { garanties: [], loading: true, show: false });

    this.packService.getGarantiesDuPack(packId).subscribe({
      next: (garanties) => {
        const current = this.packsWithGaranties.get(packId);
        if (current) {
          current.loading = false;
          current.garanties = garanties;
          this.packsWithGaranties.set(packId, current);
        }
      },
      error: () => {
        const current = this.packsWithGaranties.get(packId);
        if (current) {
          current.loading = false;
          this.packsWithGaranties.set(packId, current);
        }
      }
    });
  }

  toggleGaranties(pack: Pack): void {
    const packId = pack.idPack;
    if (!packId) return;

    if (!this.packsWithGaranties.has(packId)) {
      this.loadGarantiesForPack(packId);
    }

    const current = this.packsWithGaranties.get(packId);
    if (current) {
      current.show = !current.show;
      this.packsWithGaranties.set(packId, current);
    }
  }

  getPackGaranties(packId: string): { garanties: PackGarantie[]; loading: boolean; show: boolean } {
    return this.packsWithGaranties.get(packId) || { garanties: [], loading: false, show: false };
  }
}

