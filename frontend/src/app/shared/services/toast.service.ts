import { Injectable } from '@angular/core';
import { MessageService } from 'primeng/api';

export interface ToastOptions {
  summary?: string;
  detail?: string;
  life?: number;
  sticky?: boolean;
  closable?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class ToastService {
  constructor(private messageService: MessageService) {}

  showSuccess(detail: string, summary: string = 'Succès', options?: Partial<ToastOptions>): void {
    this.messageService.add({
      severity: 'success',
      summary,
      detail,
      life: 10000,
      ...options
    });
  }

  showError(detail: string, summary: string = 'Erreur', options?: Partial<ToastOptions>): void {
    this.messageService.add({
      severity: 'error',
      summary,
      detail,
      life: 10000,
      sticky: true,
      ...options
    });
  }

  showInfo(detail: string, summary: string = 'Information', options?: Partial<ToastOptions>): void {
    this.messageService.add({
      severity: 'info',
      summary,
      detail,
      life: 10000,
      ...options
    });
  }

  showWarning(detail: string, summary: string = 'Avertissement', options?: Partial<ToastOptions>): void {
    this.messageService.add({
      severity: 'warn',
      summary,
      detail,
      life: 10000,
      ...options
    });
  }

  // Messages prédéfinis pour les actions CRUD
  showCreateSuccess(entity: string): void {
    this.showSuccess(`${entity} créé avec succès`);
  }

  showUpdateSuccess(entity: string): void {
    this.showSuccess(`${entity} mis à jour avec succès`);
  }

  showDeleteSuccess(entity: string): void {
    this.showSuccess(`${entity} supprimé avec succès`);
  }

  showLoadError(entity: string): void {
    this.showError(`Erreur lors du chargement des ${entity}`);
  }

  showCreateError(entity: string): void {
    this.showError(`Erreur lors de la création du ${entity}`);
  }

  showUpdateError(entity: string): void {
    this.showError(`Erreur lors de la mise à jour du ${entity}`);
  }

  showDeleteError(entity: string): void {
    this.showError(`Erreur lors de la suppression du ${entity}`);
  }

  showNetworkError(): void {
    this.showError('Erreur de connexion', 'Veuillez vérifier votre connexion internet');
  }

  showUnauthorizedError(): void {
    this.showError('Accès non autorisé', 'Veuillez vous reconnecter');
  }

  showNotFoundError(entity: string): void {
    this.showError(`${entity} non trouvé`);
  }

  clearAll(): void {
    this.messageService.clear();
  }
}
