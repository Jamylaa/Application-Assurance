import { Component, OnInit } from '@angular/core';
import { ConfirmationService } from 'primeng/api';
import { GestionUserService, User } from '../../services/gestion-user.service';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { BadgeModule } from 'primeng/badge';
import { TooltipModule } from 'primeng/tooltip';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { ToastService } from '../../shared/services/toast.service';
import { BreadcrumbService } from '../../shared/services/breadcrumb.service';

@Component({
  selector: 'app-users',
  templateUrl: './users.component.html',
  styleUrls: ['./users.component.css'],
  standalone: true,
  imports: [
    TableModule,
    ButtonModule,
    CardModule,
    InputTextModule,
    ToastModule,
    ConfirmDialogModule,
    BadgeModule,
    TooltipModule,
    CommonModule,
    FormsModule,
    ReactiveFormsModule
  ],
  providers: [ConfirmationService]
})
export class UsersComponent implements OnInit {
  users: User[] = [];
  loading = false;
  globalFilterValue = '';

  constructor(
    private userService: GestionUserService,
    private confirmationService: ConfirmationService,
    private toastService: ToastService,
    private breadcrumbService: BreadcrumbService
  ) {}

  ngOnInit(): void {
    this.breadcrumbService.setUsersBreadcrumb();
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading = true;
    this.userService.getAllUsers().subscribe({
      next: (data) => {
        this.users = data;
        this.loading = false;
      },
      error: (error) => {
        this.loading = false;
        console.error('Error loading users:', error);
        this.toastService.showLoadError('utilisateurs');
      }
    });
  }

  deleteUser(user: User): void {
    if (!user.idUser) return;

    this.confirmationService.confirm({
      message: `Etes-vous sur de vouloir supprimer l'utilisateur ${user.username}?`,
      header: 'Confirmation de suppression',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.userService.deleteUser(user.idUser!).subscribe({
          next: () => {
            this.users = this.users.filter(u => u.idUser !== user.idUser);
            this.toastService.showDeleteSuccess('Utilisateur');
          },
          error: (error) => {
            console.error('Error deleting user:', error);
            this.toastService.showDeleteError('utilisateur');
          }
        });
      }
    });
  }

  onGlobalFilter(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.globalFilterValue = target.value;
  }

  refreshUsers(): void {
    this.loadUsers();
  }

  getInitials(name: string): string {
    if (!name) return 'U';
    return name.split(' ').map((s: string) => s[0]).join('').toUpperCase().slice(0, 2);
  }

  addUser(): void {
    try {
      this.toastService.showSuccess('Formulaire d\'ajout d\'utilisateur', 'Redirection vers le formulaire de création');
      // TODO: Implémenter la navigation vers le formulaire d'ajout
      // this.router.navigate(['/users/add']);
    } catch (error) {
      this.toastService.showError('Erreur lors de l\'ajout de l\'utilisateur', 'Veuillez réessayer plus tard');
    }
  }
}
