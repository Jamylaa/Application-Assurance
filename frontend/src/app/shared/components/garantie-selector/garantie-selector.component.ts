import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { AutoCompleteModule, AutoCompleteCompleteEvent } from 'primeng/autocomplete';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { InputNumberModule } from 'primeng/inputnumber';
import { ButtonModule } from 'primeng/button';

import { GestionProduitService, Garantie } from '../../../services/gestion-produit.service';
import { DomaineMedical, StatutWorkflow, getDomaineMedicalLabel } from '../../../models/entities.model';
import { ToastService } from '../../services/toast.service';
import { UiModalComponent } from '../ui-modal/ui-modal.component';

/**
 * Sélecteur de garanties existantes avec autocomplétion (recherche par nom).
 * Règle métier : un pack ne peut être associé qu'à des garanties déjà existantes.
 * Si le terme saisi ne correspond à aucune garantie, propose de choisir une autre
 * garantie existante ou d'en créer une nouvelle (rattachée automatiquement à la
 * sélection une fois créée) — jamais de création silencieuse.
 */
@Component({
  selector: 'app-garantie-selector',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    AutoCompleteModule,
    DropdownModule,
    InputTextModule,
    InputTextareaModule,
    InputNumberModule,
    ButtonModule,
    UiModalComponent
  ],
  templateUrl: './garantie-selector.component.html',
  styleUrls: ['./garantie-selector.component.css']
})
export class GarantieSelectorComponent implements OnInit {
  @Input() label = 'Garanties';
  @Input() initialGaranties: Garantie[] = [];
  @Output() garantiesChange = new EventEmitter<Garantie[]>();

  selectedGaranties: Garantie[] = [];
  suggestions: Garantie[] = [];
  currentQuery = '';

  showCreateModal = false;
  creating = false;
  createForm: FormGroup;
  domaineOptions = Object.values(DomaineMedical).map(d => ({
    label: getDomaineMedicalLabel(d),
    value: d
  }));

  constructor(
    private readonly fb: FormBuilder,
    private readonly garantieService: GestionProduitService,
    private readonly toastService: ToastService
  ) {
    this.createForm = this.fb.group({
      codeGarantie: ['', [Validators.required]],
      nomGarantie: ['', [Validators.required]],
      description: ['', [Validators.required]],
      domaine: [null, [Validators.required]],
      tauxRemboursementBase: [80]
    });
  }

  ngOnInit(): void {
    if (this.initialGaranties?.length) {
      this.selectedGaranties = [...this.initialGaranties];
    }
  }

  search(event: AutoCompleteCompleteEvent): void {
    this.currentQuery = event.query;
    if (!event.query || !event.query.trim()) {
      this.suggestions = [];
      return;
    }
    this.garantieService.searchGaranties(event.query).subscribe({
      next: (results) => {
        const selectedIds = new Set(this.selectedGaranties.map(g => g.idGarantie));
        this.suggestions = results.filter(g => !selectedIds.has(g.idGarantie));
      },
      error: () => {
        this.suggestions = [];
      }
    });
  }

  onSelectionChange(selection: Garantie[]): void {
    this.selectedGaranties = selection;
    this.garantiesChange.emit(this.selectedGaranties);
  }

  openCreateModal(): void {
    this.createForm.reset({
      codeGarantie: '',
      nomGarantie: this.currentQuery || '',
      description: '',
      domaine: null,
      tauxRemboursementBase: 80
    });
    this.showCreateModal = true;
  }

  closeCreateModal(): void {
    this.showCreateModal = false;
  }

  confirmCreateGarantie(): void {
    if (this.createForm.invalid) {
      Object.keys(this.createForm.controls).forEach(key => this.createForm.get(key)?.markAsTouched());
      return;
    }

    this.creating = true;
    const v = this.createForm.value;
    const nouvelleGarantie: Partial<Garantie> = {
      codeGarantie: v.codeGarantie,
      nomGarantie: v.nomGarantie,
      description: v.description,
      domaine: v.domaine,
      tauxRemboursementBase: v.tauxRemboursementBase,
      statutWorkflow: StatutWorkflow.PUBLIE
    };

    this.garantieService.createGarantie(nouvelleGarantie as Garantie).subscribe({
      next: (garantie) => {
        this.creating = false;
        this.showCreateModal = false;
        this.selectedGaranties = [...this.selectedGaranties, garantie];
        this.garantiesChange.emit(this.selectedGaranties);
        this.currentQuery = '';
        this.suggestions = [];
        this.toastService.showSuccess('Garantie créée', `"${garantie.nomGarantie}" a été créée et ajoutée au pack`);
      },
      error: () => {
        this.creating = false;
        this.toastService.showError('Erreur', 'Impossible de créer la garantie');
      }
    });
  }
}
