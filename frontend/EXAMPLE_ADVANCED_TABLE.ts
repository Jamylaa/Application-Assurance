/**
 * EXAMPLE: Enhanced Data Page Component
 *
 * Shows how to implement advanced features:
 * - Column visibility toggle
 * - Export (CSV/PDF)
 * - Inline editing
 * - Batch operations
 * - Advanced filtering
 *
 * Apply these patterns to:
 * - src/app/pages/produits/produits.component.ts
 * - src/app/pages/packs/packs.component.ts
 * - src/app/pages/garanties/garanties.component.ts
 */

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown';
import { ConfirmationService } from 'primeng/api';

import { UiModalComponent } from '../../shared/components/ui-modal/ui-modal.component';
import { UiInputComponent } from '../../shared/components/ui-input/ui-input.component';
import { UiTextareaComponent } from '../../shared/components/ui-textarea/ui-textarea.component';
import { UiSkeletonComponent } from '../../shared/components/ui-skeleton/ui-skeleton.component';
import { GestionProduitService } from '../../services/gestion-produit.service';

interface Produit {
  id: string;
  nom: string;
  typeProduit: string;
  description: string;
  statut: string;
  createdAt: Date;
  updatedAt: Date;
}

interface ColumnConfig {
  field: string;
  header: string;
  width: string;
  sortable: boolean;
}

@Component({
  selector: 'app-produits-advanced',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TableModule,
    ButtonModule,
    DropdownModule,
    UiModalComponent,
    UiInputComponent,
    UiTextareaComponent,
    UiSkeletonComponent
  ],
  templateUrl: './produits.component.html',
  styleUrls: ['./produits.component.scss']
})
export class ProduitsAdvancedComponent implements OnInit {
  // Data
  produits: Produit[] = [];
  filteredProduits: Produit[] = [];

  // UI State
  loading = false;
  submitting = false;
  showCreateModal = false;
  showDetailsModal = false;

  // Selection & Editing
  selectedRows: Produit[] = [];
  editingRows: Set<Produit> = new Set();
  selectedProduit: Produit | null = null;
  editingProduit: Produit | null = null;

  // Filters
  selectedType: string | null = null;
  selectedStatus: string | null = null;
  globalFilterValue = '';

  // Column Configuration
  allColumns: ColumnConfig[] = [
    { field: 'nom', header: 'Nom', width: '250px', sortable: true },
    { field: 'typeProduit', header: 'Type', width: '150px', sortable: true },
    { field: 'description', header: 'Description', width: '300px', sortable: false },
    { field: 'statut', header: 'Statut', width: '120px', sortable: true },
    { field: 'createdAt', header: 'Créé le', width: '150px', sortable: true },
    { field: 'updatedAt', header: 'Modifié le', width: '150px', sortable: true }
  ];

  visibleColumns: ColumnConfig[] = [];
  columnOptions = this.allColumns.map(col => ({ label: col.header, field: col.field }));

  // Options for dropdowns
  typeOptions = [
    { label: 'Santé', value: 'SANTE' },
    { label: 'Auto', value: 'AUTO' },
    { label: 'Habitation', value: 'HABITATION' },
    { label: 'Voyage', value: 'VOYAGE' }
  ];

  statusOptions = [
    { label: 'Actif', value: 'ACTIF' },
    { label: 'Inactif', value: 'INACTIF' }
  ];

  // Form
  form: FormGroup;

  constructor(
    private fb: FormBuilder,
    private produitService: GestionProduitService,
    private confirmationService: ConfirmationService
  ) {
    this.form = this.fb.group({
      nom: ['', [Validators.required, Validators.minLength(3)]],
      typeProduit: ['', Validators.required],
      description: ['', [Validators.required, Validators.maxLength(500)]],
      statut: ['ACTIF', Validators.required]
    });

    // Initialize column preferences from localStorage
    this.loadColumnPreferences();
  }

  ngOnInit(): void {
    this.loadProduits();
  }

  /**
   * Load products from service
   */
  loadProduits(): void {
    this.loading = true;
    this.produitService.getAllProduits().subscribe({
      next: (data) => {
        this.produits = data;
        this.applyFilters();
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  /**
   * Apply all active filters
   */
  applyFilters(): void {
    this.filteredProduits = this.produits.filter(p => {
      const typeMatch = !this.selectedType || p.typeProduit === this.selectedType;
      const statusMatch = !this.selectedStatus || p.statut === this.selectedStatus;
      const searchMatch = !this.globalFilterValue ||
        p.nom.toLowerCase().includes(this.globalFilterValue.toLowerCase()) ||
        p.description.toLowerCase().includes(this.globalFilterValue.toLowerCase());

      return typeMatch && statusMatch && searchMatch;
    });
  }

  /**
   * Global filter on input
   */
  onGlobalFilter(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.globalFilterValue = value;
    this.applyFilters();
  }

  hasActiveFilters(): boolean {
    return !!(this.selectedType || this.selectedStatus || this.globalFilterValue);
  }

  /**
   * Clear individual filters
   */
  clearTypeFilter(): void {
    this.selectedType = null;
    this.applyFilters();
  }

  clearStatusFilter(): void {
    this.selectedStatus = null;
    this.applyFilters();
  }

  clearSearchFilter(): void {
    this.globalFilterValue = '';
    this.applyFilters();
  }

  resetFilters(): void {
    this.selectedType = null;
    this.selectedStatus = null;
    this.globalFilterValue = '';
    this.applyFilters();
  }

  /**
   * Column Visibility Management
   */
  loadColumnPreferences(): void {
    const saved = localStorage.getItem('produitColumnPrefs');
    if (saved) {
      const fields = JSON.parse(saved) as string[];
      this.visibleColumns = this.allColumns.filter(col => fields.includes(col.field));
    } else {
      this.visibleColumns = this.allColumns;
    }
  }

  saveColumnPreferences(): void {
    const fields = this.visibleColumns.map(col => col.field);
    localStorage.setItem('produitColumnPrefs', JSON.stringify(fields));
  }

  /**
   * Get column value with formatting
   */
  getColumnValue(row: any, field: string): string {
    const value = row[field];
    if (field === 'createdAt' || field === 'updatedAt') {
      return new Date(value).toLocaleDateString();
    }
    return value;
  }

  /**
   * Inline Editing
   */
  isEditingRow(row: Produit): boolean {
    return this.editingRows.has(row);
  }

  startEdit(row: Produit): void {
    // Save original data for cancel
    this.editingRows.add(row);
  }

  saveEdit(row: Produit): void {
    this.submitting = true;
    this.produitService.update(row).subscribe({
      next: () => {
        this.editingRows.delete(row);
        this.submitting = false;
        // Show success message
      },
      error: () => {
        this.submitting = false;
      }
    });
  }

  cancelEdit(row: Produit): void {
    this.editingRows.delete(row);
    this.loadProduits();
  }

  /**
   * Modal Forms
   */
  openCreateModal(): void {
    this.editingProduit = null;
    this.form.reset();
    this.showCreateModal = true;
  }

  saveProduit(): void {
    if (!this.form.valid) return;

    this.submitting = true;
    const data = this.form.value;

    const request = this.editingProduit
      ? this.produitService.update({ ...data, id: this.editingProduit.id })
      : this.produitService.create(data);

    request.subscribe({
      next: () => {
        this.showCreateModal = false;
        this.loadProduits();
        this.submitting = false;
      },
      error: () => {
        this.submitting = false;
      }
    });
  }

  /**
   * Details & Delete
   */
  viewDetails(row: Produit): void {
    this.selectedProduit = row;
    this.showDetailsModal = true;
  }

  deleteProduit(row: Produit): void {
    this.confirmationService.confirm({
      message: `Êtes-vous sûr de vouloir supprimer "${row.nom}"?`,
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.produitService.delete(row.id).subscribe({
          next: () => {
            this.loadProduits();
          }
        });
      }
    });
  }

  /**
   * Export Functionality
   */
  exportToCSV(): void {
    const headers = this.visibleColumns.map(col => col.header).join(',');
    const rows = this.filteredProduits.map(p =>
      this.visibleColumns.map(col => p[col.field as keyof Produit]).join(',')
    ).join('\n');

    const csv = headers + '\n' + rows;
    this.downloadFile(csv, 'produits.csv', 'text/csv');
  }

  exportToPDF(): void {
    // Implement PDF export using a library like jspdf
    console.log('PDF export would be implemented with jspdf library');
  }

  private downloadFile(content: string, fileName: string, mimeType: string): void {
    const blob = new Blob([content], { type: mimeType });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = fileName;
    link.click();
  }

  /**
   * Batch Operations
   */
  bulkDelete(): void {
    this.confirmationService.confirm({
      message: `Supprimer ${this.selectedRows.length} produits?`,
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        const ids = this.selectedRows.map(r => r.id);
        this.produitService.bulkDelete(ids).subscribe({
          next: () => {
            this.selectedRows = [];
            this.loadProduits();
          }
        });
      }
    });
  }

  bulkExport(): void {
    const csv = this.selectedRows.map(r =>
      `${r.nom},${r.typeProduit},${r.statut}`
    ).join('\n');
    this.downloadFile(csv, 'produits-export.csv', 'text/csv');
  }

  /**
   * Form Error Handling
   */
  getFieldError(fieldName: string): string | null {
    const control = this.form.get(fieldName);
    if (!control || !control.errors || !control.touched) {
      return null;
    }

    if (control.hasError('required')) {
      return `${fieldName} est obligatoire`;
    }
    if (control.hasError('minlength')) {
      return `${fieldName} doit contenir au moins ${control.getError('minlength').requiredLength} caractères`;
    }
    if (control.hasError('maxlength')) {
      return `${fieldName} ne peut pas dépasser ${control.getError('maxlength').requiredLength} caractères`;
    }
    return null;
  }

  /**
   * Status Styling
   */
  getStatusSeverity(status: string): 'success' | 'danger' | 'warning' | 'info' {
    switch (status) {
      case 'ACTIF':
        return 'success';
      case 'INACTIF':
        return 'danger';
      default:
        return 'info';
    }
  }

  /**
   * Utility Methods
   */
  refreshProduits(): void {
    this.loadProduits();
  }
}
