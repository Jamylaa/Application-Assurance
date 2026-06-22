import { Component, EventEmitter, Output } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ThemeService } from '../../core/theme.service';
import { TranslationService } from '../../services/translation.service';
import { TranslatePipe } from '../../pipes/translate.pipe';

@Component({
  selector: 'app-sidebar',
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.css'],
  standalone: true,
  imports: [CommonModule, TranslatePipe]
})
export class SidebarComponent {
  @Output() sidebarToggle = new EventEmitter<void>();

  private _isCollapsed = false;

  get isCollapsed(): boolean {
    return this._isCollapsed;
  }

  set isCollapsed(value: boolean) {
    this._isCollapsed = value;
  }

  isDarkMode = false;
  currentUser: any = null;

  constructor(
    private router: Router,
    private themeService: ThemeService,
    private translationService: TranslationService
  ) {
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

  logout(): void {
    localStorage.clear();
    this.router.navigate(['/auth/login']);
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
