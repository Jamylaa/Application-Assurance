import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { filter } from 'rxjs/operators';
import { Subscription } from 'rxjs';
import { SidebarComponent } from '../shared/components/sidebar.component';
import { BreadcrumbComponent } from '../shared/components/breadcrumb.component';
import { BreadcrumbService } from '../shared/services/breadcrumb.service';
import { ThemeService } from '../core/theme.service';
import { NotificationService, Notification } from '../services/notification.service';

@Component({
  selector: 'app-main-layout',
  templateUrl: './main-layout.component.html',
  styleUrls: ['./main-layout.component.css'],
  standalone: true,
  imports: [RouterOutlet, BreadcrumbComponent, SidebarComponent, CommonModule, FormsModule],
  providers: []
})
export class MainLayoutComponent implements OnInit, OnDestroy {
  currentUser: any = null;
  isSidebarCollapsed = false;
  isMobileMenuOpen = false;
  showUserDropdown = false;
  searchOpen = false;
  searchQuery = '';
  showNotificationDropdown = false;
  notifications: Notification[] = [];
  unreadCount = 0;

  private resizeHandler = () => this.checkMobileView();
  private routerSub?: Subscription;
  private notificationSub?: Subscription;

  constructor(
    private router: Router,
    private breadcrumbService: BreadcrumbService,
    private themeService: ThemeService,
    private notificationService: NotificationService
  ) {
    this.loadUserData();
  }

  ngOnInit(): void {
    this.breadcrumbService.updateBreadcrumbFromUrl();
    this.routerSub = this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe(() => this.breadcrumbService.updateBreadcrumbFromUrl());
    this.checkMobileView();
    window.addEventListener('resize', this.resizeHandler);
    
    // Initialize notifications
    this.notifications = this.notificationService.getNotifications();
    this.unreadCount = this.notificationService.getUnreadCount();
    
    this.notificationSub = this.notificationService.notifications$.subscribe(
      notifications => {
        this.notifications = notifications;
      }
    );
    
    this.notificationService.unreadCount$.subscribe(
      count => {
        this.unreadCount = count;
      }
    );
  }

  ngOnDestroy(): void {
    this.routerSub?.unsubscribe();
    this.notificationSub?.unsubscribe();
    window.removeEventListener('resize', this.resizeHandler);
  }

  loadUserData(): void {
    const userData = localStorage.getItem('currentUser');
    if (userData) {
      this.currentUser = JSON.parse(userData);
    }
  }

  getInitials(name: string): string {
    if (!name) return 'U';
    return name
      .split(' ')
      .map((s) => s[0])
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
    //if (url.includes('/users')) return 'bi-people-fill';
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
    if (!this.searchQuery || this.searchQuery.trim().length === 0) {
      return;
    }

    const query = this.searchQuery.trim().toLowerCase();
    
    // Determine which page to navigate to based on current page
    const currentUrl = this.router.url;
    
    // Navigate to appropriate page with search parameter
    if (currentUrl.includes('/dashboard')) {
      // On dashboard, search in products by default
      this.router.navigate(['/produits'], { queryParams: { search: query } });
    } else if (currentUrl.includes('/produits')) {
      this.router.navigate(['/produits'], { queryParams: { search: query } });
    } else if (currentUrl.includes('/packs')) {
      this.router.navigate(['/packs'], { queryParams: { search: query } });
    } else if (currentUrl.includes('/garanties')) {
      this.router.navigate(['/garanties'], { queryParams: { search: query } });
    } else {
      // Default to products search
      this.router.navigate(['/produits'], { queryParams: { search: query } });
    }

    this.closeSearch();
  }

  toggleUserDropdown(): void {
    this.showUserDropdown = !this.showUserDropdown;
    this.showNotificationDropdown = false;
  }

  toggleNotificationDropdown(): void {
    this.showNotificationDropdown = !this.showNotificationDropdown;
    this.showUserDropdown = false;
  }

  closeDropdown(): void {
    this.showUserDropdown = false;
    this.showNotificationDropdown = false;
  }

  markNotificationAsRead(notificationId: string): void {
    this.notificationService.markAsRead(notificationId);
  }

  markAllNotificationsAsRead(): void {
    this.notificationService.markAllAsRead();
  }

  deleteNotification(notificationId: string): void {
    this.notificationService.deleteNotification(notificationId);
  }

  clearAllNotifications(): void {
    this.notificationService.clearAll();
  }

  getNotificationIcon(type: string): string {
    switch (type) {
      case 'produit': return 'bi-box-seam-fill';
      case 'pack': return 'bi-collection-fill';
      case 'garantie': return 'bi-shield-fill-check';
      case 'success': return 'bi-check-circle-fill';
      case 'warning': return 'bi-exclamation-triangle-fill';
      case 'error': return 'bi-x-circle-fill';
      default: return 'bi-info-circle-fill';
    }
  }

  getNotificationColor(type: string): string {
    switch (type) {
      case 'produit': return '#6366F1';
      case 'pack': return '#8B5CF6';
      case 'garantie': return '#10B981';
      case 'success': return '#10B981';
      case 'warning': return '#F59E0B';
      case 'error': return '#EF4444';
      default: return '#64748B';
    }
  }

  formatNotificationTime(timestamp: Date): string {
    const now = new Date();
    const notificationTime = new Date(timestamp);
    const diffMs = now.getTime() - notificationTime.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return 'À l\'instant';
    if (diffMins < 60) return `Il y a ${diffMins} min`;
    if (diffHours < 24) return `Il y a ${diffHours} h`;
    if (diffDays < 7) return `Il y a ${diffDays} j`;
    return notificationTime.toLocaleDateString('fr-FR');
  }

  checkMobileView(): void {
    if (window.innerWidth < 768) {
      this.isSidebarCollapsed = true;
    }
  }

  logout(): void {
    localStorage.clear();
    this.router.navigate(['/auth/login']);
  }
}
