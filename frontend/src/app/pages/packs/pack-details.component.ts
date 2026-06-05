import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { GestionProduitService, Pack } from '../../services/gestion-produit.service';
import { ToastService } from '../../shared/services/toast.service';
import { BreadcrumbService } from '../../shared/services/breadcrumb.service';

@Component({
  selector: 'app-pack-details',
  standalone: true,
  imports: [CommonModule, RouterModule, CardModule, ButtonModule, ProgressSpinnerModule, TagModule, ToastModule],
  templateUrl: './pack-details.component.html',
  styleUrls: ['./pack-details.component.css']
})
export class PackDetailsComponent implements OnInit {
  loading = true;
  pack?: Pack;
  packId?: string;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly packService: GestionProduitService,
    private readonly toastService: ToastService,
    private readonly breadcrumbService: BreadcrumbService
  ) {}

  ngOnInit(): void {
    this.packId = this.route.snapshot.paramMap.get('idPack') || undefined;
    this.breadcrumbService.setBreadcrumb([
      { label: 'Accueil', routerLink: ['/dashboard'], icon: 'pi pi-home' },
      { label: 'Gestion', icon: 'pi pi-cog' },
      { label: 'Packs', routerLink: ['/packs'], icon: 'pi pi-collection' },
      { label: 'Détails', icon: 'pi pi-info-circle' }
    ]);

    if (!this.packId) {
      this.loading = false;
      this.toastService.showError('Identifiant pack manquant');
      return;
    }

    this.loadPack(this.packId);
  }

  private loadPack(idPack: string): void {
    this.loading = true;
    this.packService.getPackById(idPack).subscribe({
      next: (p) => {
        this.pack = p;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toastService.showError('Impossible de charger le pack');
      }
    });
  }

  back(): void {
    this.router.navigate(['/packs']);
  }

  edit(): void {
    if (!this.packId) return;
    this.router.navigate(['/packs/edit', this.packId]);
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
