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
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toastService.showError('Impossible de charger la garantie');
      }
    });
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

  formatDateTime(value?: string): string {
    if (!value) return '—';
    try {
      return new Date(value).toLocaleString('fr-TN');
    } catch (e) {
      return value;
    }
  }
}
