import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface Notification {
  id: string;
  type: 'produit' | 'pack' | 'garantie' | 'info' | 'success' | 'warning' | 'error';
  title: string;
  message: string;
  timestamp: Date;
  read: boolean;
  entityId?: string;
  entityName?: string;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private notifications: Notification[] = [];
  private notificationsSubject = new BehaviorSubject<Notification[]>([]);
  private unreadCountSubject = new BehaviorSubject<number>(0);

  notifications$: Observable<Notification[]> = this.notificationsSubject.asObservable();
  unreadCount$: Observable<number> = this.unreadCountSubject.asObservable();

  constructor() {
    this.loadNotifications();
  }

  private loadNotifications(): void {
    const saved = localStorage.getItem('notifications');
    if (saved) {
      try {
        this.notifications = JSON.parse(saved);
        this.notificationsSubject.next(this.notifications);
        this.updateUnreadCount();
      } catch (e) {
        console.error('Error loading notifications:', e);
      }
    }
  }

  private saveNotifications(): void {
    localStorage.setItem('notifications', JSON.stringify(this.notifications));
  }

  private updateUnreadCount(): void {
    const unreadCount = this.notifications.filter(n => !n.read).length;
    this.unreadCountSubject.next(unreadCount);
  }

  addNotification(notification: Omit<Notification, 'id' | 'timestamp' | 'read'>): void {
    const newNotification: Notification = {
      ...notification,
      id: this.generateId(),
      timestamp: new Date(),
      read: false
    };

    this.notifications.unshift(newNotification);

    // Keep only last 50 notifications
    if (this.notifications.length > 50) {
      this.notifications = this.notifications.slice(0, 50);
    }

    this.saveNotifications();
    this.notificationsSubject.next(this.notifications);
    this.updateUnreadCount();
  }

  addProduitCreatedNotification(nomProduit: string, idProduit: string): void {
    this.addNotification({
      type: 'produit',
      title: 'Nouveau produit créé',
      message: `Le produit "${nomProduit}" a été créé avec succès.`,
      entityId: idProduit,
      entityName: nomProduit
    });
  }

  addPackCreatedNotification(nomPack: string, idPack: string): void {
    this.addNotification({
      type: 'pack',
      title: 'Nouveau pack créé',
      message: `Le pack "${nomPack}" a été créé avec succès.`,
      entityId: idPack,
      entityName: nomPack
    });
  }

  addGarantieCreatedNotification(nomGarantie: string, idGarantie: string): void {
    this.addNotification({
      type: 'garantie',
      title: 'Nouvelle garantie créée',
      message: `La garantie "${nomGarantie}" a été créée avec succès.`,
      entityId: idGarantie,
      entityName: nomGarantie
    });
  }

  markAsRead(notificationId: string): void {
    const notification = this.notifications.find(n => n.id === notificationId);
    if (notification) {
      notification.read = true;
      this.saveNotifications();
      this.notificationsSubject.next(this.notifications);
      this.updateUnreadCount();
    }
  }

  markAllAsRead(): void {
    this.notifications.forEach(n => n.read = true);
    this.saveNotifications();
    this.notificationsSubject.next(this.notifications);
    this.updateUnreadCount();
  }

  deleteNotification(notificationId: string): void {
    this.notifications = this.notifications.filter(n => n.id !== notificationId);
    this.saveNotifications();
    this.notificationsSubject.next(this.notifications);
    this.updateUnreadCount();
  }

  clearAll(): void {
    this.notifications = [];
    this.saveNotifications();
    this.notificationsSubject.next(this.notifications);
    this.updateUnreadCount();
  }

  getNotifications(): Notification[] {
    return this.notifications;
  }

  getUnreadCount(): number {
    return this.notifications.filter(n => !n.read).length;
  }

  private generateId(): string {
    return `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  }
}
