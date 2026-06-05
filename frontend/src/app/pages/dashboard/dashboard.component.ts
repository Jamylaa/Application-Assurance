import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom, Subscription } from 'rxjs';

import { GestionProduitService } from '../../services/gestion-produit.service';
import { GestionUserService } from '../../services/gestion-user.service';
import { ThemeService } from '../../core/theme.service';
import { KeycloakService } from 'keycloak-angular';
import { ToastService } from '../../core/toast.service';

import { ToastModule } from 'primeng/toast';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { ChartModule } from 'primeng/chart';
import { CommonModule } from '@angular/common';

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
    CommonModule
  ]
})
export class DashboardComponent implements OnInit, OnDestroy {
  currentUser: any = null;
  today = new Date();
  loading = true;
  private themeSubscription?: Subscription;

  stats = {
    totalUsers: 0,
    totalProduits: 0,
    totalPacks: 0,
    totalGaranties: 0
  };

  recentActivities: Array<{
    icon: string;
    type: string;
    message: string;
    time: string;
  }> = [];

  barChartData: any;
  pieChartData: any;
  lineChartData: any;
  polarChartData: any;

  chartOptions: any;
  pieChartOptions: any;
  polarOptions: any;

  private readonly keycloakService = inject(KeycloakService);

  constructor(
    private readonly router: Router,
    private readonly userService: GestionUserService,
    private readonly produitService: GestionProduitService,
    private readonly themeService: ThemeService,
    private readonly toastService: ToastService
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
    const kc = this.keycloakService.getKeycloakInstance();
    if (!kc.authenticated) {
      await this.keycloakService.login();
      return;
    }

    const profile = await kc.loadUserProfile();
    const tokenParsed: any = kc.tokenParsed;
    this.currentUser = {
      username: profile.username,
      email: profile.email,
      firstName: profile.firstName,
      lastName: profile.lastName,
      roles: tokenParsed?.realm_access?.roles || []
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

        this.generateRecentActivities(users, produits, packs, garanties);
        this.initChartOptions();

        this.prepareCharts(
          produits,
          packs,
          users,
          garanties
        );

        this.toastService.success('Dashboard chargé avec succès');
      })
      .catch((error) => {
        this.toastService.error('Erreur lors du chargement des données');
      })
      .finally(() => {
        this.loading = false;
      });
  }

  generateRecentActivities(users: any[], produits: any[], packs: any[], garanties: any[]): void {
    const activities = [];

    if (produits.length > 0) {
      activities.push({
        icon: 'bi-box-seam-fill',
        type: 'success',
        message: `${produits.length} produits disponibles`,
        time: 'À l\'instant'
      });
    }

    if (packs.length > 0) {
      activities.push({
        icon: 'bi-collection-fill',
        type: 'info',
        message: `${packs.length} packs configurés`,
        time: 'À l\'instant'
      });
    }

    if (garanties.length > 0) {
      activities.push({
        icon: 'bi-shield-fill-check',
        type: 'primary',
        message: `${garanties.length} garanties actives`,
        time: 'À l\'instant'
      });
    }

    if (users.length > 0) {
      activities.push({
        icon: 'bi-people-fill',
        type: 'warning',
        message: `${users.length} utilisateurs enregistrés`,
        time: 'À l\'instant'
      });
    }

    this.recentActivities = activities.slice(0, 5);
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
  }

  prepareCharts(
    produits: any[],
    packs: any[],
    users: any[],
    garanties: any[]
  ): void {

    this.barChartData = {
      labels: ['Auto', 'Santé', 'Habitation'],
      datasets: [
        {
          label: 'Produits',
          data: [
            produits.length,
            packs.length,
            garanties.length
          ]
        }
      ]
    };

    this.pieChartData = {
      labels: ['Premium', 'Standard', 'Basic'],
      datasets: [
        {
          data: [40, 35, 25]
        }
      ]
    };

    this.lineChartData = {
      labels: ['Jan', 'Fév', 'Mar', 'Avr'],
      datasets: [
        {
          label: 'Utilisateurs',
          data: [5, 10, 20, users.length]
        }
      ]
    };

    this.polarChartData = {
      labels: ['Vol', 'Incendie', 'Santé'],
      datasets: [
        {
          data: [11, 16, 7]
        }
      ]
    };
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

  logout(): void {
    this.keycloakService.logout(window.location.origin);
  }
  navigateToUsers(): void {
    this.router.navigate(['/users']);
  }
  navigateToProduits(): void {
    this.router.navigate(['/produits']);
  }
  navigateToPacks(): void {
    this.router.navigate(['/packs']);
  }
  navigateToGaranties(): void {
    this.router.navigate(['/garanties']);
  }
  navigateToChatbot(): void {
    this.router.navigate(['/chatbot']);
  }
}