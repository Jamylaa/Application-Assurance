import { Component, EventEmitter, Output } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ThemeService } from '../../core/theme.service';
import { KeycloakService } from 'keycloak-angular';

@Component({
  selector: 'app-sidebar',
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.css'],
  standalone: true,
  imports: [CommonModule]
})
export class SidebarComponent {
  @Output() sidebarToggle = new EventEmitter<void>();

  isCollapsed = false;
  isDarkMode = false;
  currentUser: any = null;

  constructor(private router: Router, private themeService: ThemeService, private keycloak: KeycloakService) {
    this.loadUserData();
    this.themeService.theme$.subscribe(theme => {
      this.isDarkMode = theme === 'dark';
    });
  }

  loadUserData(): void {
    const userData = localStorage.getItem('currentUser');
    if (userData) {
      this.currentUser = JSON.parse(userData);
    }
  }

  toggleSidebar(): void {
    this.isCollapsed = !this.isCollapsed;
    this.sidebarToggle.emit();
  }

  toggleTheme(): void {
    this.themeService.toggle();
  }

  navigateTo(route: string): void {
    this.router.navigate([route]);
  }

  navigateToDashboard(): void {
    this.router.navigate(['/dashboard']);
  }

  async logout(): Promise<void> {
    localStorage.clear();
    await this.keycloak.logout();
  }

  isActiveRoute(route: string): boolean {
    return this.router.url.startsWith(route);
  }

  getUserInitials(): string {
    if (!this.currentUser?.username) return 'U';
    return this.currentUser.username
      .split(' ')
      .map((s: string) => s[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);
  }

  getUserRole(): string {
    return this.currentUser?.role || 'Utilisateur';
  }
}
