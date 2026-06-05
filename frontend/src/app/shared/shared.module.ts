import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

// PrimeNG Components
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { MessageModule } from 'primeng/message';
import { ToastModule } from 'primeng/toast';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { AvatarModule } from 'primeng/avatar';
import { BadgeModule } from 'primeng/badge';
import { DividerModule } from 'primeng/divider';
import { TableModule } from 'primeng/table';
import { DialogModule } from 'primeng/dialog';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DropdownModule } from 'primeng/dropdown';
import { PaginatorModule } from 'primeng/paginator';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { ConfirmationService } from 'primeng/api';

@NgModule({
  declarations: [],
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    ReactiveFormsModule,
    
    // PrimeNG modules for rich components
    ButtonModule,
    CardModule,
    InputTextModule,
    PasswordModule,
    MessageModule,
    ToastModule,
    ProgressSpinnerModule,
    AvatarModule,
    BadgeModule,
    DividerModule,
    TableModule,
    DialogModule,
    ConfirmDialogModule,
    DropdownModule,
    PaginatorModule,
    TagModule,
    TooltipModule
  ],
  exports: [
    CommonModule,
    RouterModule,
    FormsModule,
    ReactiveFormsModule,
    
    // Export PrimeNG modules
    ButtonModule,
    CardModule,
    InputTextModule,
    PasswordModule,
    MessageModule,
    ToastModule,
    ProgressSpinnerModule,
    AvatarModule,
    BadgeModule,
    DividerModule,
    TableModule,
    DialogModule,
    ConfirmDialogModule,
    DropdownModule,
    PaginatorModule,
    TagModule,
    TooltipModule
  ],
  providers: [
    ConfirmationService
  ]
})
export class SharedModule { }
