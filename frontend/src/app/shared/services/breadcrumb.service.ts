import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject } from 'rxjs';

export interface BreadcrumbItem {
  label: string;
  routerLink?: string[];
  url?: string; // Maintained for backward compatibility
  icon?: string;
}

@Injectable({
  providedIn: 'root'
})
export class BreadcrumbService {
  private breadcrumbSubject = new BehaviorSubject<BreadcrumbItem[]>([]);
  public breadcrumb$ = this.breadcrumbSubject.asObservable();

  constructor(private router: Router) {}

  setBreadcrumb(items: BreadcrumbItem[]): void {
    this.breadcrumbSubject.next(items);
  }

  // Méthodes prédéfinies pour les pages communes
  setHomeBreadcrumb(): void {
    this.setBreadcrumb([
      { label: 'Accueil', routerLink: ['/dashboard'] }
    ]);
  }

  setUsersBreadcrumb(): void {
    this.setBreadcrumb([
      { label: 'Accueil', routerLink: ['/dashboard'], icon: 'pi pi-home' },
      { label: 'Gestion', icon: 'pi pi-cog' },
      { label: 'Utilisateurs', routerLink: ['/users'], icon: 'pi pi-users' }
    ]);
  }

  setProduitsBreadcrumb(): void {
    this.setBreadcrumb([
      { label: 'Accueil', routerLink: ['/dashboard'], icon: 'pi pi-home' },
      { label: 'Gestion', icon: 'pi pi-cog' },
      { label: 'Produits', routerLink: ['/produits'], icon: 'pi pi-box' }
    ]);
  }

  setPacksBreadcrumb(): void {
    this.setBreadcrumb([
      { label: 'Accueil', routerLink: ['/dashboard'], icon: 'pi pi-home' },
      { label: 'Gestion', icon: 'pi pi-cog' },
      { label: 'Packs', routerLink: ['/packs'], icon: 'pi pi-collection' }
    ]);
  }

  setGarantiesBreadcrumb(): void {
    this.setBreadcrumb([
      { label: 'Accueil', routerLink: ['/dashboard'], icon: 'pi pi-home' },
      { label: 'Gestion', icon: 'pi pi-cog' },
      { label: 'Garanties', routerLink: ['/garanties'], icon: 'pi pi-shield' }
    ]);
  }

  setDashboardBreadcrumb(): void {
    this.setBreadcrumb([
      { label: 'Dashboard', routerLink: ['/dashboard'], icon: 'pi pi-home' }
    ]);
  }

  setAuthBreadcrumb(): void {
    this.setBreadcrumb([
      { label: 'Authentification', icon: 'pi pi-lock' }
    ]);
  }

  setChatbotBreadcrumb(): void {
    this.setBreadcrumb([
      { label: 'Accueil', routerLink: ['/dashboard'], icon: 'pi pi-home' },
      { label: 'Assistant IA', routerLink: ['/chatbot'], icon: 'pi pi-robot' }
    ]);
  }

  // Méthode générique basée sur l'URL actuelle
  updateBreadcrumbFromUrl(): void {
    const url = this.router.url;
    
    if (url.includes('/users')) {
      this.setUsersBreadcrumb();
    } else if (url.includes('/produits')) {
      this.setProduitsBreadcrumb();
    } else if (url.includes('/packs')) {
      this.setPacksBreadcrumb();
    } else if (url.includes('/garanties')) {
      this.setGarantiesBreadcrumb();
    } else if (url.includes('/chatbot')) {
      this.setChatbotBreadcrumb();
    } else if (url.includes('/dashboard')) {
      this.setDashboardBreadcrumb();
    } else if (url.includes('/auth')) {
      this.setAuthBreadcrumb();
    } else {
      this.setHomeBreadcrumb();
    }
  }

  // Nettoyer le breadcrumb
  clear(): void {
    this.breadcrumbSubject.next([]);
  }
}
