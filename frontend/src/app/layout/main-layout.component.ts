import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, NavigationEnd, RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { filter } from 'rxjs/operators';
import { Subscription } from 'rxjs';

import { SidebarComponent } from '../shared/components/sidebar.component';
import { BreadcrumbComponent } from '../shared/components/breadcrumb.component';
import { BreadcrumbService } from '../shared/services/breadcrumb.service';
import { ThemeService } from '../core/theme.service';
import { KeycloakService } from 'keycloak-angular';

@Component({
  selector: 'app-main-layout',
  templateUrl: './main-layout.component.html',
  styleUrls: ['./main-layout.component.css'],
  standalone: true,
  imports: [
    RouterOutlet,
    BreadcrumbComponent,
    SidebarComponent,
    CommonModule,
    FormsModule
  ]
})
export class MainLayoutComponent implements OnInit, OnDestroy {

  currentUser: any = null;

  isSidebarCollapsed = false;
  isMobileMenuOpen = false;
  showUserDropdown = false;
  searchOpen = false;
  searchQuery = '';

  private resizeHandler = () => this.checkMobileView();
  private routerSub?: Subscription;

  constructor(
    private router: Router,
    private breadcrumbService: BreadcrumbService,
    private themeService: ThemeService,
    private keycloakService: KeycloakService
  ) {}

  async ngOnInit(): Promise<void> {

  // Charger les informations utilisateur depuis Keycloak
  this.currentUser = await this.keycloakService.loadUserProfile();

  // Breadcrumbs
  this.breadcrumbService.updateBreadcrumbFromUrl();

  this.routerSub = this.router.events
    .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
    .subscribe(() => {
      this.breadcrumbService.updateBreadcrumbFromUrl();
    });

  // Responsive
  this.checkMobileView();
  window.addEventListener('resize', this.resizeHandler);
}

  ngOnDestroy(): void {
    this.routerSub?.unsubscribe();
    window.removeEventListener('resize', this.resizeHandler);
  }

  getUserName(): string {

  if (!this.currentUser) {
    return 'Utilisateur';
  }

  return (
    this.currentUser.username ||
    this.currentUser.firstName ||
    'Utilisateur'
  );
}

  getInitials(name?: string): string {

    const username = name || this.getUserName();

    if (!username) {
      return 'U';
    }

    return username
      .split(' ')
      .map((s: string) => s[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);
  }

  getCurrentPageTitle(): string {

    const url = this.router.url;

    if (url.includes('/dashboard')) return 'Dashboard';
    if (url.includes('/users')) return 'Utilisateurs';
    if (url.includes('/produits')) return 'Produits';
    if (url.includes('/packs')) return 'Packs';
    if (url.includes('/garanties')) return 'Garanties';
    if (url.includes('/chatbot')) return 'Assistant IA';

    return 'Dashboard';
  }

  getCurrentPageIcon(): string {

    const url = this.router.url;

    if (url.includes('/dashboard')) return 'bi-grid-1x2-fill';
    if (url.includes('/users')) return 'bi-people-fill';
    if (url.includes('/produits')) return 'bi-box-seam-fill';
    if (url.includes('/packs')) return 'bi-collection-fill';
    if (url.includes('/garanties')) return 'bi-shield-fill-check';
    if (url.includes('/chatbot')) return 'bi-robot';

    return 'bi-grid-1x2-fill';
  }

  onSidebarToggle(): void {
    this.isSidebarCollapsed = !this.isSidebarCollapsed;
  }

  toggleMobileSidebar(): void {
    this.isMobileMenuOpen = !this.isMobileMenuOpen;
  }

  closeMobileSidebar(): void {
    this.isMobileMenuOpen = false;
  }

  toggleSearch(): void {
    this.searchOpen = !this.searchOpen;
  }

  closeSearch(): void {

    setTimeout(() => {
      this.searchOpen = false;
      this.searchQuery = '';
    }, 150);
  }

  performSearch(): void {
    if (!this.searchQuery || this.searchQuery.trim().length < 2) {
      return;
    }

    const query = this.searchQuery.toLowerCase().trim();
    this.closeSearch();

    // Navigate to produits page with search query
    this.router.navigate(['/produits'], { queryParams: { search: query } });
  }

  toggleUserDropdown(): void {
    this.showUserDropdown = !this.showUserDropdown;
  }

  closeDropdown(): void {
    this.showUserDropdown = false;
  }

  checkMobileView(): void {

    if (window.innerWidth < 768) {
      this.isSidebarCollapsed = true;
    }
  }

  logout(): void {
    this.keycloakService.logout(window.location.origin);
  }
}