import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { GestionProduitService, Garantie } from '../../services/gestion-produit.service';
import { DomaineMedical, TypeMontant, TypePlafond, TypeFranchise, StatutWorkflow, getStatutWorkflowLabel } from '../../models/entities.model';
import { ToastService } from '../../shared/services/toast.service';
import { BreadcrumbService } from '../../shared/services/breadcrumb.service';
import { NotificationService } from '../../services/notification.service';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { InputNumberModule } from 'primeng/inputnumber';
import { ChipsModule } from 'primeng/chips';
import { InputSwitchModule } from 'primeng/inputswitch';
import { MultiSelectModule } from 'primeng/multiselect';
import { FieldsetModule } from 'primeng/fieldset';
import { ToastModule } from 'primeng/toast';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { KeyValueEditorComponent } from '../../shared/components/key-value-editor/key-value-editor.component';

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
    ChipsModule,
    InputSwitchModule,
    MultiSelectModule,
    FieldsetModule,
    ToastModule,
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    KeyValueEditorComponent
  ]
})
export class GarantieFormComponent implements OnInit {
  garantieForm: FormGroup;
  loading = false;
  isEdit = false;
  garantieId?: string;
  allGaranties: { label: string; value: string }[] = [];

  domaineMedicalOptions = [
    { label: 'Hospitalisation', value: DomaineMedical.HOSPITALISATION },
    { label: 'Dentaire', value: DomaineMedical.DENTISTERIE_GENERALE },
    { label: 'Optique', value: DomaineMedical.OPHTALMOLOGIE },
    { label: 'Consultation', value: DomaineMedical.CONSULTATION_GENERALE }
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

  statutWorkflowOptions = Object.values(StatutWorkflow).map(s => ({
    label: getStatutWorkflowLabel(s),
    value: s
  }));

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
      codeGarantie: ['', [Validators.required]],
      nomCourt: ['', [Validators.maxLength(30)]],
      description: ['', [Validators.required]],
      descriptionTechnique: [''],
      domaine: [null],
      garantieObligatoireParDefaut: [false],
      statutWorkflow: [StatutWorkflow.BROUILLON],
      evenementsCouvertsParDefaut: [[] as string[]],
      tauxRemboursement: [0, [Validators.required, Validators.min(0), Validators.max(100)]],
      typeMontant: [null],
      typePlafond: [null],
      plafondAnnuel: [0, [Validators.min(0)]],
      plafondMensuel: [0, [Validators.min(0)]],
      franchise: [0, [Validators.min(0)]],
      prerequisGarantieIds: [[] as string[]],
      parametresDynamiques: [{} as Record<string, number>],
      primePureBase: [0, [Validators.min(0)]],
      regleCalcul: this.fb.group({
        formule: [''],
        descriptionFormule: [''],
        parametresFormule: [[] as string[]],
        valeursDefaut: [{} as Record<string, number>],
        prioriteCalcul: [1, [Validators.min(1)]],
        appliquerPlafondApresCalcul: [true],
        deduireFranchiseAvantPlafond: [false],
        baseConventionnee: [false]
      })
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

    this.loadGaranties();

    if (this.isEdit && this.garantieId) {
      this.loading = true;
      this.garantieService.getGarantieById(this.garantieId).subscribe({
        next: (g) => {
          this.garantieForm.patchValue({
            nomGarantie: g.nomGarantie,
            codeGarantie: g.codeGarantie ?? '',
            nomCourt: g.nomCourt ?? '',
            description: g.description,
            descriptionTechnique: g.descriptionTechnique ?? '',
            domaine: g.domaine,
            garantieObligatoireParDefaut: g.garantieObligatoireParDefaut ?? false,
            statutWorkflow: g.statutWorkflow ?? StatutWorkflow.BROUILLON,
            evenementsCouvertsParDefaut: g.evenementsCouvertsParDefaut ?? [],
            tauxRemboursement: g.tauxRemboursementBase,
            typeMontant: g.typeRemboursement,
            typePlafond: g.plafond?.typePrincipal,
            plafondAnnuel: g.plafond?.plafondAnnuel,
            plafondMensuel: g.plafond?.plafondMensuel,
            franchise: g.franchise?.montantFixe,
            prerequisGarantieIds: g.prerequisGarantieIds ?? [],
            parametresDynamiques: g.parametresDynamiques ?? {},
            primePureBase: g.primePureBase ?? 0,
            regleCalcul: {
              formule: g.regleCalcul?.formule ?? '',
              descriptionFormule: g.regleCalcul?.descriptionFormule ?? '',
              parametresFormule: g.regleCalcul?.parametresFormule ?? [],
              valeursDefaut: g.regleCalcul?.valeursDefaut ?? {},
              prioriteCalcul: g.regleCalcul?.prioriteCalcul ?? 1,
              appliquerPlafondApresCalcul: g.regleCalcul?.appliquerPlafondApresCalcul ?? true,
              deduireFranchiseAvantPlafond: g.regleCalcul?.deduireFranchiseAvantPlafond ?? false,
              baseConventionnee: g.regleCalcul?.baseConventionnee ?? false
            }
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

  loadGaranties(): void {
    this.garantieService.getAllGaranties().subscribe({
      next: (data) => {
        this.allGaranties = data
          .filter(g => g.idGarantie !== this.garantieId)
          .map(g => ({ label: g.nomGarantie, value: g.idGarantie }));
      },
      error: (error) => {
        console.error('Error loading garanties:', error);
        this.toastService.showError('Erreur', 'Impossible de charger les garanties');
      }
    });
  }

  onSubmit(): void {
    if (this.garantieForm.invalid) {
      Object.keys(this.garantieForm.controls).forEach(key => {
        this.garantieForm.get(key)?.markAsTouched();
      });
      return;
    }

    this.loading = true;
    const formValue = this.garantieForm.value;
    const rc = formValue.regleCalcul;
    const regleCalculEmpty = !rc.formule && !rc.descriptionFormule
      && (!rc.parametresFormule || rc.parametresFormule.length === 0)
      && (!rc.valeursDefaut || Object.keys(rc.valeursDefaut).length === 0)
      && rc.prioriteCalcul === 1 && rc.appliquerPlafondApresCalcul === true
      && rc.deduireFranchiseAvantPlafond === false && rc.baseConventionnee === false;

    const garantieData: Partial<Garantie> = {
      nomGarantie: formValue.nomGarantie,
      codeGarantie: formValue.codeGarantie,
      nomCourt: formValue.nomCourt || undefined,
      description: formValue.description,
      descriptionTechnique: formValue.descriptionTechnique || undefined,
      domaine: formValue.domaine,
      garantieObligatoireParDefaut: formValue.garantieObligatoireParDefaut,
      statutWorkflow: formValue.statutWorkflow || undefined,
      evenementsCouvertsParDefaut: formValue.evenementsCouvertsParDefaut?.length ? formValue.evenementsCouvertsParDefaut : undefined,
      typeRemboursement: formValue.typeMontant,
      tauxRemboursementBase: formValue.tauxRemboursement,
      plafond: {
        typePrincipal: formValue.typePlafond,
        plafondAnnuel: formValue.plafondAnnuel || undefined,
        plafondMensuel: formValue.plafondMensuel || undefined
      },
      franchise: formValue.franchise
        ? { type: TypeFranchise.FIXE, montantFixe: formValue.franchise }
        : undefined,
      prerequisGarantieIds: formValue.prerequisGarantieIds?.length ? formValue.prerequisGarantieIds : undefined,
      parametresDynamiques: formValue.parametresDynamiques && Object.keys(formValue.parametresDynamiques).length > 0 ? formValue.parametresDynamiques : undefined,
      primePureBase: formValue.primePureBase || undefined,
      regleCalcul: regleCalculEmpty ? undefined : rc
    };
    const request$ = this.isEdit && this.garantieId
      ? this.garantieService.updateGarantie(this.garantieId, { ...garantieData, idGarantie: this.garantieId } as Garantie)
      : this.garantieService.createGarantie(garantieData as Garantie);

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
      if (field.errors['maxlength']) return `Maximum ${field.errors['maxlength'].requiredLength} caractères`;
    }
    return '';
  }
}
