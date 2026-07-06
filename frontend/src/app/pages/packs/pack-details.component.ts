import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { GestionProduitService, Pack } from '../../services/gestion-produit.service';
import {
  PackGarantie,
  StatutWorkflow,
  getStatutWorkflowLabel,
  getStatutWorkflowBadgeVariant,
  isStatutWorkflowOutlined,
  formatDate
} from '../../models/entities.model';
import { ToastService } from '../../shared/services/toast.service';
import { BreadcrumbService } from '../../shared/services/breadcrumb.service';
import { UiBadgeComponent } from '../../shared/components/ui-badge/ui-badge.component';

@Component({
  selector: 'app-pack-details',
  standalone: true,
  imports: [CommonModule, RouterModule, CardModule, ButtonModule, ProgressSpinnerModule, TagModule, ToastModule, UiBadgeComponent],
  templateUrl: './pack-details.component.html',
  styleUrls: ['./pack-details.component.css']
})
export class PackDetailsComponent implements OnInit {
  loading = true;
  pack?: Pack;
  packId?: string;
  garantiesCount: number = 0;
  packRating: number = 0;
  packGaranties: PackGarantie[] = [];
  loadingGaranties: boolean = false;
  packNamesById: Map<string, string> = new Map();

  Math = Math;

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
        this.calculateMetrics();
        this.loading = false;
        // Charger les garanties associées
        this.loadPackGaranties(idPack);
      },
      error: () => {
        this.loading = false;
        this.toastService.showError('Impossible de charger le pack');
      }
    });

    // Résolution des références croisées (optionsPackIds, packsCompatibles, packsIncompatibles) en noms lisibles
    this.packService.getAllPacks().subscribe({
      next: (packs) => {
        this.packNamesById = new Map(packs.map(p => [p.idPack, p.nomPack]));
      },
      error: () => {
        this.packNamesById = new Map();
      }
    });
  }

  resolvePackNames(ids?: string[]): string[] {
    if (!ids || ids.length === 0) return [];
    return ids.map(id => this.packNamesById.get(id) || id);
  }

  private loadPackGaranties(idPack: string): void {
    this.loadingGaranties = true;
    this.packService.getPackGaranties(idPack).subscribe({
      next: (garanties) => {
        this.packGaranties = garanties;
        this.garantiesCount = garanties.length;
        this.loadingGaranties = false;
      },
      error: () => {
        this.loadingGaranties = false;
        // Ne pas afficher d'erreur, les garanties peuvent ne pas être associées
      }
    });
  }

  private calculateMetrics(): void {
    if (!this.pack) return;

    // Le nombre de garanties est maintenant chargé depuis le backend
    // this.garantiesCount est mis à jour dans loadPackGaranties

    // Calculer une note basée sur le prix et le niveau de couverture
    const prixMensuel = this.pack.prixMensuel ?? 0;
    const prixScore = prixMensuel <= 50 ? 100 : (prixMensuel <= 100 ? 80 : 60);
    const niveauCouverture = (this.pack.niveauCouverture || '').toString().toUpperCase();
    const niveauScore = niveauCouverture === 'PREMIUM' ? 100 :
                       niveauCouverture === 'GOLD' ? 90 :
                       niveauCouverture === 'STANDARD' ? 70 : 50;
    this.packRating = Math.round((prixScore + niveauScore) / 2);
  }

  back(): void {
    this.router.navigate(['/packs']);
  }

  edit(): void {
    if (!this.packId) return;
    this.router.navigate(['/packs/edit', this.packId]);
  }

  getNiveauClass(niveau?: string): string {
    switch (niveau?.toUpperCase()) {
      case 'PREMIUM': return 'premium';
      case 'GOLD': return 'gold';
      case 'SILVER': return 'silver';
      case 'STANDARD': return 'standard';
      case 'BASIC': return 'basic';
      default: return 'unknown';
    }
  }

  getCoverageScore(): number {
    if (!this.pack) return 0;

    // Score niveau (43%)
    const niveauMap: Record<string, number> = { GOLD: 100, PREMIUM: 75, BASIC: 50 };
    const niveauScore = niveauMap[(this.pack.niveauCouverture ?? '').toUpperCase()] ?? 40;

    // Score garanties (57%) — plafonné à 10 garanties = 100%
    const garantiesScore = Math.min(this.garantiesCount * 10, 100);

    return Math.round(niveauScore * 0.43 + garantiesScore * 0.57);
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

  formatDateValue(date?: string): string {
    return date ? formatDate(date) : '—';
  }

  getPricingCategory(): string {
    if (!this.pack) return '—';
    const prix = this.pack.prixMensuel ?? 0;
    if (prix <= 50) return 'Économique';
    if (prix <= 100) return 'Standard';
    if (prix <= 200) return 'Premium';
    return 'Luxueux';
  }
}
