import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { GestionProduitService, Pack } from '../../services/gestion-produit.service';
import { NiveauCouverture, StatutWorkflow, getStatutWorkflowLabel, Garantie, PackGarantie } from '../../models/entities.model';
import { GarantieSelectorComponent } from '../../shared/components/garantie-selector/garantie-selector.component';
import { ToastService } from '../../shared/services/toast.service';
import { BreadcrumbService } from '../../shared/services/breadcrumb.service';
import { NotificationService } from '../../services/notification.service';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { InputNumberModule } from 'primeng/inputnumber';
import { CalendarModule } from 'primeng/calendar';
import { ColorPickerModule } from 'primeng/colorpicker';
import { InputSwitchModule } from 'primeng/inputswitch';
import { MultiSelectModule } from 'primeng/multiselect';
import { ToastModule } from 'primeng/toast';
import { CardModule } from 'primeng/card';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-pack-form',
  templateUrl: './pack-form.component.html',
  styleUrls: ['./pack-form.component.css'],
  standalone: true,
  imports: [
    CardModule,
    ButtonModule,
    InputTextModule,
    DropdownModule,
    InputTextareaModule,
    InputNumberModule,
    CalendarModule,
    ColorPickerModule,
    InputSwitchModule,
    MultiSelectModule,
    ToastModule,
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    RouterModule,
    GarantieSelectorComponent
  ]
})
export class PackFormComponent implements OnInit {
  packForm: FormGroup;
  loading = false;
  produits: any[] = [];
  allPacks: { label: string; value: string }[] = [];
  isEdit = false;
  packId?: string;
  selectedGaranties: Garantie[] = [];

  niveauCouvertureOptions = [
    { label: 'Basic', value: NiveauCouverture.BASIC },
    { label: 'Premium', value: NiveauCouverture.PREMIUM },
    { label: 'Gold', value: NiveauCouverture.GOLD }
  ];

  statutWorkflowOptions = Object.values(StatutWorkflow).map(s => ({
    label: getStatutWorkflowLabel(s),
    value: s
  }));

  constructor(
    private fb: FormBuilder,
    private packService: GestionProduitService,
    private produitService: GestionProduitService,
    private route: ActivatedRoute,
    private router: Router,
    private toastService: ToastService,
    private breadcrumbService: BreadcrumbService,
    private notificationService: NotificationService
  ) {
    this.packForm = this.fb.group({
      nomPack: ['', [Validators.required]],
      codePack: ['', [Validators.required]],
      nomCommercial: [''],
      description: ['', [Validators.required]],
      descriptionCourte: ['', [Validators.maxLength(280)]],
      produitId: [null, [Validators.required]],
      prixMensuel: [0, [Validators.required, Validators.min(0)]],
      prixAnnuel: [0, [Validators.min(0)]],
      tauxRemiseAnnuelle: [0, [Validators.min(0)]],
      devisePrix: ['TND'],
      niveauCouverture: [null, [Validators.required]],
      statutWorkflow: [StatutWorkflow.BROUILLON],
      packRecommande: [false],
      colorTheme: ['#0f4c81'],
      optionsDisponibles: [false],
      optionsPackIds: [[] as string[]],
      packsCompatibles: [[] as string[]],
      packsIncompatibles: [[] as string[]],
      dateEffet: [null as Date | null],
      dateExpiration: [null as Date | null]
    });
    
    // Prevent initial disabled state
    this.packForm.markAsUntouched();
  }

  ngOnInit(): void {
    this.packId = this.route.snapshot.paramMap.get('idPack') || undefined;
    this.isEdit = !!this.packId;

    this.breadcrumbService.setBreadcrumb([
      { label: 'Packs', url: '/packs' },
      { label: this.isEdit ? 'Modifier pack' : 'Nouveau pack', url: this.isEdit ? `/packs/edit/${this.packId}` : '/packs/add' }
    ]);

    this.loadProduits();
    this.loadPacks();

    if (this.isEdit && this.packId) {
      this.loadPack(this.packId);
    }
  }

  private loadPack(idPack: string): void {
    this.loading = true;
    this.packService.getPackById(idPack).subscribe({
      next: (pack) => {
        this.packForm.patchValue({
          nomPack: pack.nomPack,
          codePack: pack.codePack ?? '',
          nomCommercial: pack.nomCommercial ?? '',
          description: pack.description,
          descriptionCourte: pack.descriptionCourte ?? '',
          produitId: pack.produitId,
          prixMensuel: pack.prixMensuel ?? 0,
          prixAnnuel: pack.prixAnnuel ?? 0,
          tauxRemiseAnnuelle: pack.tauxRemiseAnnuelle ?? 0,
          devisePrix: pack.devisePrix ?? 'TND',
          niveauCouverture: pack.niveauCouverture ?? null,
          statutWorkflow: pack.statutWorkflow ?? StatutWorkflow.BROUILLON,
          packRecommande: pack.packRecommande ?? false,
          colorTheme: pack.colorTheme ?? '#0f4c81',
          optionsDisponibles: pack.optionsDisponibles ?? false,
          optionsPackIds: pack.optionsPackIds ?? [],
          packsCompatibles: pack.packsCompatibles ?? [],
          packsIncompatibles: pack.packsIncompatibles ?? [],
          dateEffet: pack.dateEffet ? new Date(pack.dateEffet) : null,
          dateExpiration: pack.dateExpiration ? new Date(pack.dateExpiration) : null
        });
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toastService.showError('Erreur', 'Impossible de charger le pack');
      }
    });
  }

  loadPacks(): void {
    this.packService.getAllPacks().subscribe({
      next: (data) => {
        this.allPacks = data
          .filter(p => p.idPack !== this.packId)
          .map(p => ({ label: p.nomPack, value: p.idPack }));
      },
      error: (error) => {
        console.error('Error loading packs:', error);
        this.toastService.showError('Erreur', 'Impossible de charger les packs');
      }
    });
  }

  loadProduits(): void {
    this.produitService.getAllProduits().subscribe({
      next: (data) => {
        this.produits = data.map(p => ({
          label: p.nomProduit,
          value: p.idProduit
        }));
      },
      error: (error) => {
        console.error('Error loading produits:', error);
        this.toastService.showError('Erreur', 'Impossible de charger les produits');
      }
    });
  }

  onSubmit(): void {
    if (this.packForm.invalid) {
      Object.keys(this.packForm.controls).forEach(key => {
        this.packForm.get(key)?.markAsTouched();
      });
      return;
    }

    this.loading = true;
    const formValue = this.packForm.value;
    const packData: Partial<Pack> = {
      nomPack: formValue.nomPack,
      codePack: formValue.codePack,
      nomCommercial: formValue.nomCommercial || undefined,
      description: formValue.description,
      descriptionCourte: formValue.descriptionCourte || undefined,
      produitId: formValue.produitId,
      prixMensuel: formValue.prixMensuel,
      prixAnnuel: formValue.prixAnnuel || undefined,
      tauxRemiseAnnuelle: formValue.tauxRemiseAnnuelle || undefined,
      devisePrix: formValue.devisePrix || undefined,
      niveauCouverture: formValue.niveauCouverture,
      statutWorkflow: formValue.statutWorkflow || undefined,
      packRecommande: formValue.packRecommande,
      colorTheme: formValue.colorTheme || undefined,
      optionsDisponibles: formValue.optionsDisponibles,
      optionsPackIds: formValue.optionsPackIds?.length ? formValue.optionsPackIds : undefined,
      packsCompatibles: formValue.packsCompatibles?.length ? formValue.packsCompatibles : undefined,
      packsIncompatibles: formValue.packsIncompatibles?.length ? formValue.packsIncompatibles : undefined,
      dateEffet: formValue.dateEffet ? (formValue.dateEffet as Date).toISOString() : undefined,
      dateExpiration: formValue.dateExpiration ? (formValue.dateExpiration as Date).toISOString() : undefined
    };
    const request$ = this.isEdit && this.packId
      ? this.packService.updatePack(this.packId, { ...packData, idPack: this.packId } as Pack)
      : this.packService.createPack(packData as Pack);

    request$.subscribe({
      next: (response) => {
        this.loading = false;
        this.toastService.showSuccess(
          this.isEdit ? 'Pack modifié' : 'Pack créé',
          this.isEdit ? 'Le pack a été modifié avec succès' : 'Le pack a été créé avec succès'
        );

        const packId = (response as any).idPack || this.packId;
        if (packId) {
          this.associerGarantiesSelectionnees(packId);
        }

        // Add notification for new pack creation
        if (!this.isEdit) {
          this.notificationService.addPackCreatedNotification(
            this.packForm.value.nomPack,
            packId || this.packForm.value.nomPack
          );
        }

        this.router.navigate(['/packs']);
      },
      error: (error) => {
        this.loading = false;
        this.toastService.showError('Erreur', this.isEdit ? 'Impossible de modifier le pack' : 'Impossible de créer le pack');
        console.error('Error saving pack:', error);
      }
    });
  }

  onGarantiesChange(garanties: Garantie[]): void {
    this.selectedGaranties = garanties;
  }

  /** Associe les garanties sélectionnées au pack nouvellement créé/modifié. Chaque échec
   * individuel est signalé sans bloquer les autres associations (le pack existe déjà). */
  private associerGarantiesSelectionnees(idPack: string): void {
    if (!this.selectedGaranties.length) {
      return;
    }
    this.selectedGaranties.forEach(garantie => {
      this.packService.ajouterGarantieAuPack(idPack, garantie.idGarantie, {} as PackGarantie).subscribe({
        error: () => {
          this.toastService.showError(
            'Association impossible',
            `La garantie "${garantie.nomGarantie}" n'a pas pu être associée au pack`
          );
        }
      });
    });
  }

  onCancel(): void {
    this.router.navigate(['/packs']);
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.packForm.get(fieldName);
    return field ? field.invalid && field.touched : false;
  }

  getErrorMessage(fieldName: string): string {
    const field = this.packForm.get(fieldName);
    if (field?.errors) {
      if (field.errors['required']) return 'Ce champ est obligatoire';
      if (field.errors['min']) return `Valeur minimum: ${field.errors['min'].min}`;
      if (field.errors['maxlength']) return `Maximum ${field.errors['maxlength'].requiredLength} caractères`;
    }
    return '';
  }
}
