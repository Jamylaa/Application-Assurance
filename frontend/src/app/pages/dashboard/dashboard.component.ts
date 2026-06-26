import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom, Subscription } from 'rxjs';
import { GestionProduitService } from '../../services/gestion-produit.service';
import { GestionUserService } from '../../services/gestion-user.service';
import { ThemeService } from '../../core/theme.service';
import { KeycloakService } from 'keycloak-angular';
import { ToastModule } from 'primeng/toast';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { ChartModule } from 'primeng/chart';
import { ProgressBarModule } from 'primeng/progressbar';
import { TagModule } from 'primeng/tag';
import { SkeletonModule } from 'primeng/skeleton';
import { CommonModule } from '@angular/common';
import { DomaineMedical, TypeProduit, NiveauCouverture, getDomaineMedicalLabel, getTypeProduitLabel, getNiveauCouvertureLabel } from '../../models/entities.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css'],
  imports: [
    ToastModule,
    ProgressSpinnerModule,
    ButtonModule,
    CardModule,
    ChartModule,
    ProgressBarModule,
    TagModule,
    SkeletonModule,
    CommonModule
  ]
})
export class DashboardComponent implements OnInit, OnDestroy {

  currentUser: any = null;
  today = new Date();
  Math = Math;
  loading = true;

  private themeSubscription?: Subscription;

  stats = {
    totalUsers: 0,
    totalProduits: 0,
    totalPacks: 0,
    totalGaranties: 0
  };

  metrics = {
    avgPrixPacks: 0,
    avgTauxRemboursement: 0,
    packsParProduit: 0,
    garantiesParPack: 0,
    croissanceProduits: 12.5,
    croissancePacks: 8.3,
    croissanceGaranties: 15.2,
    tauxConversion: 67.8,
    satisfactionClient: 4.7
  };

  barChartData: any;
  pieChartData: any;
  horizontalBarChartData: any;

  chartOptions: any;
  pieChartOptions: any;
  horizontalBarOptions: any;

  constructor(
    private readonly router: Router,
    private readonly userService: GestionUserService,
    private readonly produitService: GestionProduitService,
    private readonly themeService: ThemeService,
    private readonly keycloak: KeycloakService
  ) {}

  async ngOnInit(): Promise<void> {
    await this.loadUserData();
    this.loadStats();
    this.themeSubscription = this.themeService.theme$.subscribe(() => {
      this.initChartOptions();
    });
  }

  ngOnDestroy(): void {
    this.themeSubscription?.unsubscribe();
  }

  get todayFr(): string {
    const d = new Date();
    const days = ['Dimanche', 'Lundi', 'Mardi', 'Mercredi', 'Jeudi', 'Vendredi', 'Samedi'];
    const months = ['janvier', 'février', 'mars', 'avril', 'mai', 'juin',
                    'juillet', 'août', 'septembre', 'octobre', 'novembre', 'décembre'];
    return `${days[d.getDay()]} ${d.getDate()} ${months[d.getMonth()]} ${d.getFullYear()}`;
  }

  async loadUserData(): Promise<void> {
    const isLoggedIn = await this.keycloak.isLoggedIn();
    if (!isLoggedIn) {
      this.keycloak.login();
      return;
    }
    const profile = await this.keycloak.loadUserProfile();
    const tokenParsed = this.keycloak.getKeycloakInstance().tokenParsed;
    const allRoles = (tokenParsed as any)?.realm_access?.roles || [];
    const technicalRoles = ['default-roles-vermeg-realm', 'offline_access', 'uma_authorization', 'USER'];
    const businessRoles = allRoles.filter((role: string) => !technicalRoles.includes(role));
    this.currentUser = {
      username: profile.username || '',
      email: profile.email || '',
      firstName: profile.firstName || '',
      lastName: profile.lastName || '',
      roles: businessRoles
    };
  }

  loadStats(): void {
    this.loading = true;
    Promise.all([
      firstValueFrom(this.userService.getAllUsers()).catch(() => []),
      firstValueFrom(this.produitService.getAllProduits()).catch(() => []),
      firstValueFrom(this.produitService.getAllPacks()).catch(() => []),
      firstValueFrom(this.produitService.getAllGaranties()).catch(() => [])
    ])
      .then(([users, produits, packs, garanties]) => {
        this.stats = {
          totalUsers: users?.length || 0,
          totalProduits: produits?.length || 0,
          totalPacks: packs?.length || 0,
          totalGaranties: garanties?.length || 0
        };
        this.calculateAdvancedMetrics(packs, produits, garanties);
        this.initChartOptions();
        this.prepareCharts(produits, packs, users, garanties);
      })
      .finally(() => { this.loading = false; });
  }

  calculateAdvancedMetrics(packs: any[], produits: any[], garanties: any[]): void {
    if (packs?.length > 0) {
      const totalPrix = packs.reduce((sum, p) => sum + (p.prixMensuel || 0), 0);
      this.metrics.avgPrixPacks = Math.round(totalPrix / packs.length);
    }
    if (garanties?.length > 0) {
      const totalTaux = garanties.reduce((sum, g) => sum + ((g.tauxRemboursement || 0) * 100), 0);
      this.metrics.avgTauxRemboursement = Math.round(totalTaux / garanties.length);
    }
    if (produits?.length > 0 && packs) {
      this.metrics.packsParProduit = Math.round((packs.length / produits.length) * 10) / 10;
    }
    if (packs?.length > 0 && garanties) {
      this.metrics.garantiesParPack = Math.round((garanties.length / packs.length) * 10) / 10;
    }
  }

  initChartOptions(): void {
    const s = getComputedStyle(document.documentElement);
    const textColor = s.getPropertyValue('--color-text-primary').trim() || '#334155';
    const textSub = s.getPropertyValue('--color-text-tertiary').trim() || '#64748b';
    const border = s.getPropertyValue('--color-border-light').trim() || '#e2e8f0';

    this.chartOptions = {
      maintainAspectRatio: false,
      plugins: {
        legend: { labels: { color: textColor, font: { size: 12 } } }
      },
      scales: {
        x: { ticks: { color: textSub }, grid: { color: border } },
        y: { ticks: { color: textSub }, grid: { color: border } }
      }
    };

    this.pieChartOptions = {
      plugins: {
        legend: { labels: { color: textColor, font: { size: 12 } } }
      }
    };

    this.horizontalBarOptions = {
      indexAxis: 'y',
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: { label: (ctx: any) => `${ctx.parsed.x} garanties` }
        }
      },
      scales: {
        x: { beginAtZero: true, ticks: { color: textSub }, grid: { color: border } },
        y: { ticks: { color: textColor, font: { size: 12 } }, grid: { display: false } }
      }
    };
  }

  prepareCharts(produits: any[], packs: any[], users: any[], garanties: any[]): void {
    // Bar chart — produits par type
    const produitsByType: Record<string, number> = {};
    Object.values(TypeProduit).forEach(t => { produitsByType[t] = 0; });
    produits.forEach(p => {
      const t = p.typeProduit || 'AUTRE';
      produitsByType[t] = (produitsByType[t] || 0) + 1;
    });
    const activeTypes = Object.entries(produitsByType).filter(([, c]) => c > 0);
    this.barChartData = {
      labels: activeTypes.map(([t]) => getTypeProduitLabel(t as TypeProduit)),
      datasets: [{
        label: 'Produits',
        data: activeTypes.map(([, c]) => c),
        backgroundColor: ['rgba(15,76,129,0.8)', 'rgba(13,115,119,0.8)', 'rgba(96,165,250,0.8)',
                          'rgba(21,101,52,0.8)', 'rgba(180,83,9,0.8)'],
        borderRadius: 6
      }]
    };

    // Doughnut — packs par niveau
    const packsByNiveau: Record<string, number> = {};
    Object.values(NiveauCouverture).forEach(n => { packsByNiveau[n] = 0; });
    packs.forEach(p => {
      const n = p.niveauCouverture || 'BASIC';
      packsByNiveau[n] = (packsByNiveau[n] || 0) + 1;
    });
    const activeNiveaux = Object.entries(packsByNiveau).filter(([, c]) => c > 0);
    this.pieChartData = {
      labels: activeNiveaux.map(([n]) => getNiveauCouvertureLabel(n as NiveauCouverture)),
      datasets: [{
        data: activeNiveaux.map(([, c]) => c),
        backgroundColor: ['rgba(15,76,129,0.85)', 'rgba(13,115,119,0.85)', 'rgba(180,83,9,0.85)']
      }]
    };

    // Horizontal bar — garanties par domaine
    const byDomaine: Record<string, number> = {};
    garanties.forEach(g => {
      const d = g.domaine || g.domaineMedical || 'AUTRE';
      byDomaine[d] = (byDomaine[d] || 0) + 1;
    });
    const sorted = Object.entries(byDomaine)
      .filter(([, c]) => c > 0)
      .sort((a, b) => b[1] - a[1])
      .slice(0, 8);
    const palette = ['rgba(15,76,129,0.85)', 'rgba(13,115,119,0.85)', 'rgba(96,165,250,0.85)',
                     'rgba(6,45,82,0.85)', 'rgba(14,160,160,0.85)', 'rgba(185,28,28,0.85)',
                     'rgba(139,92,246,0.85)', 'rgba(251,191,36,0.85)'];
    this.horizontalBarChartData = {
      labels: sorted.map(([d]) => getDomaineMedicalLabel(d as DomaineMedical)),
      datasets: [{
        label: 'Garanties',
        data: sorted.map(([, c]) => c),
        backgroundColor: sorted.map((_, i) => palette[i % palette.length]),
        borderRadius: 4,
        barThickness: 28
      }]
    };
  }

  getGreeting(): string {
    const h = new Date().getHours();
    if (h < 12) return 'Bonjour';
    if (h < 18) return 'Bon après-midi';
    return 'Bonsoir';
  }

  getInitials(name: string): string {
    if (!name) return 'U';
    return name.split(' ').map(s => s[0]).join('').toUpperCase().slice(0, 2);
  }

  logout(): void { this.keycloak.logout(); }
  navigateToUsers(): void { this.router.navigate(['/users']); }
  navigateToProduits(): void { this.router.navigate(['/produits']); }
  navigateToPacks(): void { this.router.navigate(['/packs']); }
  navigateToGaranties(): void { this.router.navigate(['/garanties']); }
  navigateToChatbot(): void { this.router.navigate(['/chatbot']); }
}
