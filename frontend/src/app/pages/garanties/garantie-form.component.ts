import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { GestionProduitService, Garantie } from '../../services/gestion-produit.service';
import { DomaineMedical, Statut, TypeMontant, TypePlafond } from '../../models/entities.model';
import { ToastService } from '../../shared/services/toast.service';
import { BreadcrumbService } from '../../shared/services/breadcrumb.service';
import { NotificationService } from '../../services/notification.service';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { InputNumberModule } from 'primeng/inputnumber';
import { ToastModule } from 'primeng/toast';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-garantie-form',
  templateUrl: './garantie-form.component.html',
  styleUrls: ['./garantie-form.component.css'],
  standalone: true,
  imports: [
    CardModule,
    ButtonModule,
    InputTextModule,
    DropdownModule,
    InputTextareaModule,
    InputNumberModule,
    ToastModule,
    CommonModule,
    ReactiveFormsModule,
    RouterModule
  ]
})
export class GarantieFormComponent implements OnInit {
  garantieForm: FormGroup;
  loading = false;
  isEdit = false;
  garantieId?: string;

  domaineMedicalOptions = [
    { label: 'Hospitalisation', value: DomaineMedical.HOSPITALISATION },
    { label: 'Dentaire', value: DomaineMedical.DENTISTERIE_GENERALE },
    { label: 'Optique', value: DomaineMedical.OPHTALMOLOGIE },
    { label: 'Consultation', value: DomaineMedical.CONSULTATION_GENERALE }
  ];

  statutOptions = [
    { label: 'Actif', value: Statut.ACTIF },
    { label: 'Inactif', value: Statut.INACTIF }
  ];

  typeMontantOptions = [
    { label: 'Forfait', value: TypeMontant.FORFAIT },
    { label: 'Frais réels', value: TypeMontant.FRAIS_REELS },
    { label: 'Tarif conventionné', value: TypeMontant.TARIF_CONVENTIONNE }
  ];

  typePlafondOptions = [
    { label: 'Par acte', value: TypePlafond.PAR_ACTE },
    { label: 'Annuel', value: TypePlafond.ANNUEL },
    { label: 'Mensuel', value: TypePlafond.MENSUEL },
    { label: 'Par soins', value: TypePlafond.PAR_SOINS },
    { label: 'Global', value: TypePlafond.GLOBAL }
  ];

  constructor(
    private fb: FormBuilder,
    private garantieService: GestionProduitService,
    private route: ActivatedRoute,
    private router: Router,
    private toastService: ToastService,
    private breadcrumbService: BreadcrumbService,
    private notificationService: NotificationService
  ) {
    this.garantieForm = this.fb.group({
      nomGarantie: ['', [Validators.required]],
      description: ['', [Validators.required]],
      domaine: [null],
      statut: [Statut.ACTIF, [Validators.required]],
      tauxRemboursement: [0, [Validators.required, Validators.min(0), Validators.max(1)]],
      typeMontant: [null],
      typePlafond: [null],
      plafondAnnuel: [0, [Validators.min(0)]],
      plafondMensuel: [0, [Validators.min(0)]],
      plafondParActe: [0, [Validators.min(0)]],
      franchise: [0, [Validators.min(0)]],
      coutMoyenParSinistre: [0, [Validators.min(0)]],
      dureeMinContrat: [1, [Validators.min(1)]],
      dureeMaxContrat: [12, [Validators.min(1)]],
      resiliableAnnuellement: [true]
    });
    
    // Prevent initial disabled state
    this.garantieForm.markAsUntouched();
  }

  ngOnInit(): void {
    this.garantieId = this.route.snapshot.paramMap.get('idGarantie') || undefined;
    this.isEdit = !!this.garantieId;

    this.breadcrumbService.setBreadcrumb([
      { label: 'Garanties', url: '/garanties' },
      { label: this.isEdit ? 'Modifier garantie' : 'Nouvelle garantie', url: this.isEdit ? `/garanties/edit/${this.garantieId}` : '/garanties/add' }
    ]);

    if (this.isEdit && this.garantieId) {
      this.loading = true;
      this.garantieService.getGarantieById(this.garantieId).subscribe({
        next: (g) => {
          this.garantieForm.patchValue({
            nomGarantie: g.nomGarantie,
            description: g.description,
            domaine: g.domaine,
            statut: g.statut,
            tauxRemboursement: g.tauxRemboursement,
            typeMontant: g.typeMontant,
            typePlafond: g.typePlafond,
            plafondAnnuel: g.plafondAnnuel,
            plafondMensuel: g.plafondMensuel,
            plafondParActe: g.plafondParActe,
            franchise: g.franchise,
            coutMoyenParSinistre: g.coutMoyenParSinistre,
            dureeMinContrat: g.dureeMinContrat,
            dureeMaxContrat: g.dureeMaxContrat,
            resiliableAnnuellement: g.resiliableAnnuellement
          });
          this.loading = false;
        },
        error: () => {
          this.loading = false;
          this.toastService.showError('Erreur', 'Impossible de charger la garantie');
        }
      });
    }
  }

  onSubmit(): void {
    if (this.garantieForm.invalid) {
      Object.keys(this.garantieForm.controls).forEach(key => {
        this.garantieForm.get(key)?.markAsTouched();
      });
      return;
    }

    this.loading = true;
    const garantieData = this.garantieForm.value;
    const request$ = this.isEdit && this.garantieId
      ? this.garantieService.updateGarantie(this.garantieId, { ...garantieData, idGarantie: this.garantieId } as unknown as Garantie)
      : this.garantieService.createGarantie(garantieData);

    request$.subscribe({
      next: (response) => {
        this.loading = false;
        this.toastService.showSuccess(
          this.isEdit ? 'Garantie modifiée' : 'Garantie créée',
          this.isEdit ? 'La garantie a été modifiée avec succès' : 'La garantie a été créée avec succès'
        );
        
        // Add notification for new guarantee creation
        if (!this.isEdit) {
          const garantieId = (response as any).idGarantie || this.garantieForm.value.nomGarantie;
          this.notificationService.addGarantieCreatedNotification(
            this.garantieForm.value.nomGarantie,
            garantieId
          );
        }
        
        this.router.navigate(['/garanties']);
      },
      error: (error) => {
        this.loading = false;
        this.toastService.showError('Erreur', this.isEdit ? 'Impossible de modifier la garantie' : 'Impossible de créer la garantie');
        console.error('Error saving garantie:', error);
      }
    });
  }

  onCancel(): void {
    this.router.navigate(['/garanties']);
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.garantieForm.get(fieldName);
    return field ? field.invalid && field.touched : false;
  }

  getErrorMessage(fieldName: string): string {
    const field = this.garantieForm.get(fieldName);
    if (field?.errors) {
      if (field.errors['required']) return 'Ce champ est obligatoire';
      if (field.errors['min']) return `Valeur minimum: ${field.errors['min'].min}`;
      if (field.errors['max']) return `Valeur maximum: ${field.errors['max'].max}`;
    }
    return '';
  }
}
