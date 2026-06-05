import { Component, forwardRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';

interface CustomFieldRow {
  key: string;
  value: string;
}

@Component({
  selector: 'app-custom-fields-editor',
  standalone: true,
  imports: [CommonModule, FormsModule, ButtonModule, InputTextModule],
  template: `
    <div class="custom-fields-editor">
      <div class="cf-header">
        <label class="field-label">
          <i class="bi bi-sliders"></i> Champs personnalisés
        </label>
        <button
          type="button"
          class="cf-add-btn"
          (click)="addRow()">
          <i class="bi bi-plus-lg"></i> Ajouter un champ
        </button>
      </div>

      <div class="cf-empty" *ngIf="rows.length === 0">
        Aucun champ personnalisé. Cliquez sur "Ajouter un champ" pour en créer un.
      </div>

      <div class="cf-row" *ngFor="let row of rows; let i = index">
        <input
          pInputText
          class="cf-input cf-key"
          [(ngModel)]="row.key"
          (ngModelChange)="emitChange()"
          [name]="'cf-key-' + i"
          placeholder="Nom du champ" />
        <input
          pInputText
          class="cf-input cf-value"
          [(ngModel)]="row.value"
          (ngModelChange)="emitChange()"
          [name]="'cf-value-' + i"
          placeholder="Valeur" />
        <button
          type="button"
          class="cf-remove-btn"
          (click)="removeRow(i)"
          aria-label="Supprimer le champ">
          <i class="bi bi-trash"></i>
        </button>
      </div>
    </div>
  `,
  styles: [`
    .custom-fields-editor { display: flex; flex-direction: column; gap: 0.75rem; }
    .cf-header { display: flex; align-items: center; justify-content: space-between; }
    .field-label { font-weight: 600; color: var(--text-color, #1f2937); display: flex; align-items: center; gap: 0.4rem; }
    .cf-add-btn {
      display: inline-flex; align-items: center; gap: 0.35rem;
      background: #eff6ff; color: #2563eb; border: 1px solid #bfdbfe;
      border-radius: 8px; padding: 0.4rem 0.75rem; font-size: 0.85rem;
      cursor: pointer; transition: background 0.15s ease;
    }
    .cf-add-btn:hover { background: #dbeafe; }
    .cf-empty { font-size: 0.85rem; color: #6b7280; font-style: italic; }
    .cf-row { display: flex; align-items: center; gap: 0.5rem; }
    .cf-input { flex: 1; }
    .cf-key { max-width: 40%; }
    .cf-remove-btn {
      display: inline-flex; align-items: center; justify-content: center;
      width: 38px; height: 38px; flex-shrink: 0;
      background: #fef2f2; color: #dc2626; border: 1px solid #fecaca;
      border-radius: 8px; cursor: pointer; transition: background 0.15s ease;
    }
    .cf-remove-btn:hover { background: #fee2e2; }
  `],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CustomFieldsEditorComponent),
      multi: true
    }
  ]
})
export class CustomFieldsEditorComponent implements ControlValueAccessor {
  rows: CustomFieldRow[] = [];

  private onChange: (value: Record<string, unknown> | null) => void = () => {};
  private onTouched: () => void = () => {};

  writeValue(value: Record<string, unknown> | null): void {
    if (value && typeof value === 'object') {
      this.rows = Object.entries(value).map(([key, val]) => ({
        key,
        value: val === null || val === undefined ? '' : String(val)
      }));
    } else {
      this.rows = [];
    }
  }

  registerOnChange(fn: (value: Record<string, unknown> | null) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  addRow(): void {
    this.rows.push({ key: '', value: '' });
    this.emitChange();
  }

  removeRow(index: number): void {
    this.rows.splice(index, 1);
    this.emitChange();
  }

  emitChange(): void {
    this.onTouched();
    const result: Record<string, unknown> = {};
    for (const row of this.rows) {
      const key = row.key.trim();
      if (key) {
        result[key] = this.coerceValue(row.value);
      }
    }
    this.onChange(Object.keys(result).length > 0 ? result : null);
  }

  private coerceValue(raw: string): unknown {
    const trimmed = raw.trim();
    if (trimmed === '') {
      return '';
    }
    if (trimmed === 'true') return true;
    if (trimmed === 'false') return false;
    const num = Number(trimmed);
    if (!Number.isNaN(num) && /^-?\d+(\.\d+)?$/.test(trimmed)) {
      return num;
    }
    return trimmed;
  }
}
