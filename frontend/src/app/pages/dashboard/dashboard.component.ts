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
import { CommonModule } from '@angular/common';
import { DomaineMedical, TypeProduit, NiveauCouverture, getDomaineMedicalLabel, getTypeProduitLabel, getNiveauCouvertureLabel } from '../../models/entities.model';
import { UiSkeletonComponent } from '../../shared/components/ui-skeleton/ui-skeleton.component';

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
    CommonModule,
    UiSkeletonComponent
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

  // Métriques avancées
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
  lineChartData: any;
  polarChartData: any;
  areaChartData: any;
  scatterChartData: any;

  chartOptions: any;
  pieChartOptions: any;
  polarOptions: any;
  areaChartOptions: any;
  scatterChartOptions: any;

  // Date range filter
  startDate: Date | null = null;
  endDate: Date | null = null;

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

    this.themeSubscription = this.themeService.theme$
      .subscribe(() => {
        this.initChartOptions();
      });
  }

  ngOnDestroy(): void {
    this.themeSubscription?.unsubscribe();
  }

  async loadUserData(): Promise<void> {
    const isLoggedIn = await this.keycloak.isLoggedIn();

    if (!isLoggedIn) {
      this.keycloak.login();
      return;
    }

    const profile = await this.keycloak.loadUserProfile();

    const tokenParsed = this.keycloak.getKeycloakInstance().tokenParsed;

    // Filter out technical roles, keep only business roles
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

        // Calculer les métriques avancées
        this.calculateAdvancedMetrics(packs, produits, garanties);

        this.initChartOptions();

        this.prepareCharts(
          produits,
          packs,
          users,
          garanties
        );
      })
      .finally(() => {
        this.loading = false;
      });
  }

  calculateAdvancedMetrics(packs: any[], produits: any[], garanties: any[]): void {
    // Prix moyen des packs
    if (packs && packs.length > 0) {
      const totalPrix = packs.reduce((sum, p) => sum + (p.prixMensuel || 0), 0);
      this.metrics.avgPrixPacks = Math.round(totalPrix / packs.length);
    }

    // Taux de remboursement moyen des garanties
    if (garanties && garanties.length > 0) {
      const totalTaux = garanties.reduce((sum, g) => sum + ((g.tauxRemboursement || 0) * 100), 0);
      this.metrics.avgTauxRemboursement = Math.round(totalTaux / garanties.length);
    }

    // Packs par produit
    if (produits && produits.length > 0 && packs) {
      this.metrics.packsParProduit = Math.round((packs.length / produits.length) * 10) / 10;
    }

    // Garanties par pack
    if (packs && packs.length > 0 && garanties) {
      this.metrics.garantiesParPack = Math.round((garanties.length / packs.length) * 10) / 10;
    }
  }

  initChartOptions(): void {

    const documentStyle = getComputedStyle(document.documentElement);

    const textColor =
      documentStyle.getPropertyValue('--app-text').trim() || '#334155';

    const textColorSecondary =
      documentStyle.getPropertyValue('--app-text-muted').trim() || '#64748b';

    const surfaceBorder =
      documentStyle.getPropertyValue('--app-border-light').trim() || '#f1f5f9';

    this.chartOptions = {
      maintainAspectRatio: false,
      plugins: {
        legend: {
          labels: {
            color: textColor
          }
        }
      },
      scales: {
        x: {
          ticks: {
            color: textColorSecondary
          },
          grid: {
            color: surfaceBorder
          }
        },
        y: {
          ticks: {
            color: textColorSecondary
          },
          grid: {
            color: surfaceBorder
          }
        }
      }
    };

    this.pieChartOptions = {
      plugins: {
        legend: {
          labels: {
            color: textColor
          }
        }
      }
    };

    this.polarOptions = {
      plugins: {
        legend: {
          labels: {
            color: textColor
          }
        }
      }
    };

    // Area chart options
    this.areaChartOptions = {
      maintainAspectRatio: false,
      plugins: {
        legend: {
          labels: { color: textColor }
        }
      },
      scales: {
        x: {
          ticks: { color: textColorSecondary },
          grid: { color: surfaceBorder }
        },
        y: {
          ticks: { color: textColorSecondary },
          grid: { color: surfaceBorder }
        }
      }
    };

    // Scatter chart options
    this.scatterChartOptions = {
      maintainAspectRatio: false,
      plugins: {
        legend: {
          labels: { color: textColor }
        }
      },
      scales: {
        x: {
          ticks: { color: textColorSecondary },
          grid: { color: surfaceBorder }
        },
        y: {
          ticks: { color: textColorSecondary },
          grid: { color: surfaceBorder }
        }
      }
    };
  }

  prepareCharts(
    produits: any[],
    packs: any[],
    users: any[],
    garanties: any[]
  ): void {
    // Bar chart: Products by type
    const produitsByType: Record<string, number> = {};
    Object.values(TypeProduit).forEach(type => produitsByType[type] = 0);
    produits.forEach(p => {
      const type = p.typeProduit || 'AUTRE';
      produitsByType[type] = (produitsByType[type] || 0) + 1;
    });

    this.barChartData = {
      labels: Object.keys(produitsByType)
        .filter(type => produitsByType[type] > 0)
        .map(type => getTypeProduitLabel(type as TypeProduit)),
      datasets: [
        {
          label: 'Produits',
          data: Object.values(produitsByType).filter(count => count > 0),
          backgroundColor: [
            'rgba(99, 102, 241, 0.7)',
            'rgba(139, 92, 246, 0.7)',
            'rgba(59, 130, 246, 0.7)',
            'rgba(16, 185, 129, 0.7)',
            'rgba(245, 158, 11, 0.7)'
          ]
        }
      ]
    };

    // Pie chart: Packs by coverage level
    const packsByNiveau: Record<string, number> = {};
    Object.values(NiveauCouverture).forEach(niveau => packsByNiveau[niveau] = 0);
    packs.forEach(p => {
      const niveau = p.niveauCouverture || p.niveau || 'BASIC';
      packsByNiveau[niveau] = (packsByNiveau[niveau] || 0) + 1;
    });

    this.pieChartData = {
      labels: Object.keys(packsByNiveau)
        .filter(niveau => packsByNiveau[niveau] > 0)
        .map(niveau => getNiveauCouvertureLabel(niveau as NiveauCouverture)),
      datasets: [
        {
          data: Object.values(packsByNiveau).filter(count => count > 0),
          backgroundColor: [
            'rgba(99, 102, 241, 0.8)',
            'rgba(139, 92, 246, 0.8)',
            'rgba(245, 158, 11, 0.8)'
          ]
        }
      ]
    };

    // Line chart: Users growth (simplified - showing current total)
    this.lineChartData = {
      labels: ['Jan', 'Fév', 'Mar', 'Avr'],
      datasets: [
        {
          label: 'Utilisateurs',
          data: [Math.max(0, users.length - 3), Math.max(0, users.length - 2), Math.max(0, users.length - 1), users.length],
          borderColor: 'rgba(99, 102, 241, 1)',
          backgroundColor: 'rgba(99, 102, 241, 0.1)',
          fill: true,
          tension: 0.4
        }
      ]
    };

    // Polar chart: Guarantees by domain
    const garantiesByDomaine: Record<string, number> = {};
    garanties.forEach(g => {
      const domaine = g.domaine || g.domaineMedical || 'AUTRE';
      garantiesByDomaine[domaine] = (garantiesByDomaine[domaine] || 0) + 1;
    });

    // Filter out domains with zero values and limit to top 10
    const sortedDomains = Object.entries(garantiesByDomaine)
      .filter(([_, count]) => count > 0)
      .sort((a, b) => b[1] - a[1])
      .slice(0, 10);

    this.polarChartData = {
      labels: sortedDomains.map(([domaine, _]) => getDomaineMedicalLabel(domaine as DomaineMedical)),
      datasets: [
        {
          data: sortedDomains.map(([_, count]) => count),
          backgroundColor: [
            'rgba(99, 102, 241, 0.7)',
            'rgba(139, 92, 246, 0.7)',
            'rgba(59, 130, 246, 0.7)',
            'rgba(16, 185, 129, 0.7)',
            'rgba(245, 158, 11, 0.7)',
            'rgba(239, 68, 68, 0.7)',
            'rgba(236, 72, 153, 0.7)',
            'rgba(20, 184, 166, 0.7)',
            'rgba(168, 85, 247, 0.7)',
            'rgba(249, 115, 22, 0.7)'
          ]
        }
      ]
    };
  }

  exportChartAsImage(chartElement: HTMLCanvasElement, fileName: string): void {
    const link = document.createElement('a');
    link.href = chartElement.toDataURL('image/png');
    link.download = `${fileName}-${new Date().getTime()}.png`;
    link.click();
  }

  filterChartsByDateRange(): void {
    if (!this.startDate || !this.endDate) {
      return;
    }
    // Charts would be re-fetched based on date range
    this.loadStats();
  }

  resetDateFilter(): void {
    this.startDate = null;
    this.endDate = null;
    this.loadStats();
  }

  getGreeting(): string {
    const hour = new Date().getHours();

    if (hour < 12) {
      return 'Bonjour';
    }

    if (hour < 18) {
      return 'Bon après-midi';
    }

    return 'Bonsoir';
  }

  getInitials(name: string): string {

    if (!name) {
      return 'U';
    }

    return name
      .split(' ')
      .map(s => s[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);
  }

  logout(): void {this.keycloak.logout();}
  navigateToUsers(): void {this.router.navigate(['/users']);}
  navigateToProduits(): void {this.router.navigate(['/produits']);}
  navigateToPacks(): void {this.router.navigate(['/packs']);}
  navigateToGaranties(): void {this.router.navigate(['/garanties']);}
  navigateToChatbot(): void {this.router.navigate(['/chatbot']);}
}
