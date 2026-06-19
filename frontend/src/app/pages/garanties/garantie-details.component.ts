import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { GestionProduitService, Garantie } from '../../services/gestion-produit.service';
import { ToastService } from '../../shared/services/toast.service';
import { BreadcrumbService } from '../../shared/services/breadcrumb.service';
import { getDomaineMedicalLabel } from '../../models/entities.model';

@Component({
  selector: 'app-garantie-details',
  standalone: true,
  imports: [CommonModule, RouterModule, CardModule, ButtonModule, ProgressSpinnerModule, TagModule, ToastModule],
  templateUrl: './garantie-details.component.html',
  styleUrls: ['./garantie-details.component.css']
})
export class GarantieDetailsComponent implements OnInit {
  loading = true;
  garantie?: Garantie;
  garantieId?: string;
  packsCount: number = 0;
  garantieRating: number = 0;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly garantieService: GestionProduitService,
    private readonly toastService: ToastService,
    private readonly breadcrumbService: BreadcrumbService
  ) {}

  ngOnInit(): void {
    this.garantieId = this.route.snapshot.paramMap.get('idGarantie') || undefined;
    this.breadcrumbService.setBreadcrumb([
      { label: 'Accueil', routerLink: ['/dashboard'], icon: 'pi pi-home' },
      { label: 'Gestion', icon: 'pi pi-cog' },
      { label: 'Garanties', routerLink: ['/garanties'], icon: 'pi pi-shield' },
      { label: 'Détails', icon: 'pi pi-info-circle' }
    ]);

    if (!this.garantieId) {
      this.loading = false;
      this.toastService.showError('Identifiant garantie manquant');
      return;
    }

    this.loadGarantie(this.garantieId);
  }

  private loadGarantie(idGarantie: string): void {
    this.loading = true;
    this.garantieService.getGarantieById(idGarantie).subscribe({
      next: (g) => {
        this.garantie = g;
        this.calculateMetrics();
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toastService.showError('Impossible de charger la garantie');
      }
    });
  }

  private calculateMetrics(): void {
    if (!this.garantie) return;
    
    // Calculer le nombre de packs (simulé pour l'instant)
    this.packsCount = this.garantie.idGarantie ? Math.floor(Math.random() * 6) + 1 : 0;
    
    // Calculer une note basée sur le taux de remboursement et les plafonds
    const tauxScore = (this.garantie.tauxRemboursement ?? 0) * 100;
    const plafondScore = (this.garantie.plafondAnnuel ?? 0) >= 10000 ? 100 : 
                       (this.garantie.plafondAnnuel ?? 0) >= 5000 ? 80 : 60;
    this.garantieRating = Math.round((tauxScore + plafondScore) / 2);
  }

  back(): void {
    this.router.navigate(['/garanties']);
  }

  edit(): void {
    if (!this.garantieId) return;
    this.router.navigate(['/garanties/edit', this.garantieId]);
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

  getDomaineOrTypeLabel(g: Garantie): string {
    if (g.domaine) {
      return getDomaineMedicalLabel(g.domaine);
    }
    return '—';
  }

  protected readonly getDomaineMedicalLabel = getDomaineMedicalLabel;

  getTauxCategory(): string {
    if (!this.garantie) return '—';
    const taux = (this.garantie.tauxRemboursement ?? 0) * 100;
    if (taux >= 90) return 'Excellent';
    if (taux >= 70) return 'Très bon';
    if (taux >= 50) return 'Bon';
    return 'Standard';
  }

  getPlafondCategory(): string {
    if (!this.garantie) return '—';
    const plafond = this.garantie.plafondAnnuel ?? 0;
    if (plafond >= 20000) return 'Élevé';
    if (plafond >= 10000) return 'Moyen';
    return 'Bas';
  }
}
