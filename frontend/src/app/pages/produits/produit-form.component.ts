import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { GestionProduitService, Produit } from '../../services/gestion-produit.service';
import { TypeProduit, Statut } from '../../models/entities.model';
import { ToastService } from '../../shared/services/toast.service';
import { BreadcrumbService } from '../../shared/services/breadcrumb.service';
import { NotificationService } from '../../services/notification.service';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { ToastModule } from 'primeng/toast';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-produit-form',
  templateUrl: './produit-form.component.html',
  styleUrls: ['./produit-form.component.css'],
  standalone: true,
  imports: [
    CardModule,
    ButtonModule,
    InputTextModule,
    DropdownModule,
    InputTextareaModule,
    ToastModule,
    CommonModule,
    ReactiveFormsModule,
    RouterModule
  ]
})
export class ProduitFormComponent implements OnInit {
  produitForm: FormGroup;
  loading = false;
  isEdit = false;
  produitId?: string;

  typeProduitOptions = [
    { label: 'Santé', value: TypeProduit.SANTE },
    { label: 'Habitation', value: TypeProduit.HABITATION },
    { label: 'Auto', value: TypeProduit.AUTO },
    { label: 'Épargne', value: TypeProduit.EPARGNE },
    { label: 'Vie', value: TypeProduit.VIE }
  ];

  statutOptions = [
    { label: 'Actif', value: Statut.ACTIF },
    { label: 'Inactif', value: Statut.INACTIF }
  ];

  constructor(
    private fb: FormBuilder,
    private produitService: GestionProduitService,
    private route: ActivatedRoute,
    private router: Router,
    private toastService: ToastService,
    private breadcrumbService: BreadcrumbService,
    private notificationService: NotificationService
  ) {
    this.produitForm = this.fb.group({
      nomProduit: ['', [Validators.required]],
      description: ['', [Validators.required]],
      typeProduit: [null, [Validators.required]],
      statut: [Statut.ACTIF, [Validators.required]]
    });
    
    // Prevent initial disabled state
    this.produitForm.markAsUntouched();
  }

  ngOnInit(): void {
    this.produitId = this.route.snapshot.paramMap.get('idProduit') || undefined;
    this.isEdit = !!this.produitId;

    this.breadcrumbService.setBreadcrumb([
      { label: 'Produits', url: '/produits' },
      {
        label: this.isEdit ? 'Modifier produit' : 'Nouveau produit',
        url: this.isEdit ? `/produits/edit/${this.produitId}` : '/produits/add'
      }
    ]);

    if (this.isEdit && this.produitId) {
      this.loading = true;
      this.produitService.getProduitById(this.produitId).subscribe({
        next: (produit) => {
          this.produitForm.patchValue({
            nomProduit: produit.nomProduit,
            description: produit.description,
            typeProduit: produit.typeProduit,
            statut: produit.statut
          });
          this.loading = false;
        },
        error: () => {
          this.loading = false;
          this.toastService.showError('Erreur', 'Impossible de charger le produit');
        }
      });
    }
  }

  onSubmit(): void {
    if (this.produitForm.invalid) {
      Object.keys(this.produitForm.controls).forEach(key => {
        this.produitForm.get(key)?.markAsTouched();
      });
      return;
    }

    this.loading = true;
    const produitData = this.produitForm.value;
    const request$ = this.isEdit && this.produitId
      ? this.produitService.updateProduit(this.produitId, { ...produitData, idProduit: this.produitId } as unknown as Produit)
      : this.produitService.createProduit(produitData);

    request$.subscribe({
      next: (response) => {
        this.loading = false;
        this.toastService.showSuccess(
          this.isEdit ? 'Produit modifié' : 'Produit créé',
          this.isEdit ? 'Le produit a été modifié avec succès' : 'Le produit a été créé avec succès'
        );
        
        // Add notification for new product creation
        if (!this.isEdit && response) {
          const produitId = (response as any).idProduit || this.produitForm.value.nomProduit;
          this.notificationService.addProduitCreatedNotification(
            this.produitForm.value.nomProduit,
            produitId
          );
        }
        
        this.router.navigate(['/produits']);
      },
      error: (error) => {
        this.loading = false;
        this.toastService.showError('Erreur', this.isEdit ? 'Impossible de modifier le produit' : 'Impossible de créer le produit');
        console.error('Error saving produit:', error);
      }
    });
  }

  onCancel(): void {
    this.router.navigate(['/produits']);
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.produitForm.get(fieldName);
    return field ? field.invalid && field.touched : false;
  }

  getErrorMessage(fieldName: string): string {
    const field = this.produitForm.get(fieldName);
    if (field?.errors) {
      if (field.errors['required']) return 'Ce champ est obligatoire';
      if (field.errors['minlength']) return 'Minimum 3 caractères requis';
    }
    return '';
  }
}
