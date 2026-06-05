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

  setProduitAddBreadcrumb(): void {
    this.setBreadcrumb([
      { label: 'Accueil', routerLink: ['/dashboard'], icon: 'pi pi-home' },
      { label: 'Gestion', icon: 'pi pi-cog' },
      { label: 'Produits', routerLink: ['/produits'], icon: 'pi pi-box' },
      { label: 'Ajouter', icon: 'pi pi-plus' }
    ]);
  }

  setProduitEditBreadcrumb(id: string): void {
    this.setBreadcrumb([
      { label: 'Accueil', routerLink: ['/dashboard'], icon: 'pi pi-home' },
      { label: 'Gestion', icon: 'pi pi-cog' },
      { label: 'Produits', routerLink: ['/produits'], icon: 'pi pi-box' },
      { label: 'Modifier', icon: 'pi pi-pencil' }
    ]);
  }

  setProduitDetailsBreadcrumb(id: string): void {
    this.setBreadcrumb([
      { label: 'Accueil', routerLink: ['/dashboard'], icon: 'pi pi-home' },
      { label: 'Gestion', icon: 'pi pi-cog' },
      { label: 'Produits', routerLink: ['/produits'], icon: 'pi pi-box' },
      { label: 'Détails', icon: 'pi pi-eye' }
    ]);
  }

  setPacksBreadcrumb(): void {
    this.setBreadcrumb([
      { label: 'Accueil', routerLink: ['/dashboard'], icon: 'pi pi-home' },
      { label: 'Gestion', icon: 'pi pi-cog' },
      { label: 'Packs', routerLink: ['/packs'], icon: 'pi pi-collection' }
    ]);
  }

  setPackAddBreadcrumb(): void {
    this.setBreadcrumb([
      { label: 'Accueil', routerLink: ['/dashboard'], icon: 'pi pi-home' },
      { label: 'Gestion', icon: 'pi pi-cog' },
      { label: 'Packs', routerLink: ['/packs'], icon: 'pi pi-collection' },
      { label: 'Ajouter', icon: 'pi pi-plus' }
    ]);
  }

  setPackEditBreadcrumb(id: string): void {
    this.setBreadcrumb([
      { label: 'Accueil', routerLink: ['/dashboard'], icon: 'pi pi-home' },
      { label: 'Gestion', icon: 'pi pi-cog' },
      { label: 'Packs', routerLink: ['/packs'], icon: 'pi pi-collection' },
      { label: 'Modifier', icon: 'pi pi-pencil' }
    ]);
  }

  setPackDetailsBreadcrumb(id: string): void {
    this.setBreadcrumb([
      { label: 'Accueil', routerLink: ['/dashboard'], icon: 'pi pi-home' },
      { label: 'Gestion', icon: 'pi pi-cog' },
      { label: 'Packs', routerLink: ['/packs'], icon: 'pi pi-collection' },
      { label: 'Détails', icon: 'pi pi-eye' }
    ]);
  }

  setGarantiesBreadcrumb(): void {
    this.setBreadcrumb([
      { label: 'Accueil', routerLink: ['/dashboard'], icon: 'pi pi-home' },
      { label: 'Gestion', icon: 'pi pi-cog' },
      { label: 'Garanties', routerLink: ['/garanties'], icon: 'pi pi-shield' }
    ]);
  }

  setGarantieAddBreadcrumb(): void {
    this.setBreadcrumb([
      { label: 'Accueil', routerLink: ['/dashboard'], icon: 'pi pi-home' },
      { label: 'Gestion', icon: 'pi pi-cog' },
      { label: 'Garanties', routerLink: ['/garanties'], icon: 'pi pi-shield' },
      { label: 'Ajouter', icon: 'pi pi-plus' }
    ]);
  }

  setGarantieEditBreadcrumb(id: string): void {
    this.setBreadcrumb([
      { label: 'Accueil', routerLink: ['/dashboard'], icon: 'pi pi-home' },
      { label: 'Gestion', icon: 'pi pi-cog' },
      { label: 'Garanties', routerLink: ['/garanties'], icon: 'pi pi-shield' },
      { label: 'Modifier', icon: 'pi pi-pencil' }
    ]);
  }

  setGarantieDetailsBreadcrumb(id: string): void {
    this.setBreadcrumb([
      { label: 'Accueil', routerLink: ['/dashboard'], icon: 'pi pi-home' },
      { label: 'Gestion', icon: 'pi pi-cog' },
      { label: 'Garanties', routerLink: ['/garanties'], icon: 'pi pi-shield' },
      { label: 'Détails', icon: 'pi pi-eye' }
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
    } else if (url.includes('/produits/add')) {
      this.setProduitAddBreadcrumb();
    } else if (url.includes('/produits/edit/')) {
      this.setProduitEditBreadcrumb('');
    } else if (url.includes('/produits/') && !url.includes('/produits/add') && !url.includes('/produits/edit')) {
      this.setProduitDetailsBreadcrumb('');
    } else if (url.includes('/produits')) {
      this.setProduitsBreadcrumb();
    } else if (url.includes('/packs/add')) {
      this.setPackAddBreadcrumb();
    } else if (url.includes('/packs/edit/')) {
      this.setPackEditBreadcrumb('');
    } else if (url.includes('/packs/') && !url.includes('/packs/add') && !url.includes('/packs/edit')) {
      this.setPackDetailsBreadcrumb('');
    } else if (url.includes('/packs')) {
      this.setPacksBreadcrumb();
    } else if (url.includes('/garanties/add')) {
      this.setGarantieAddBreadcrumb();
    } else if (url.includes('/garanties/edit/')) {
      this.setGarantieEditBreadcrumb('');
    } else if (url.includes('/garanties/') && !url.includes('/garanties/add') && !url.includes('/garanties/edit')) {
      this.setGarantieDetailsBreadcrumb('');
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
