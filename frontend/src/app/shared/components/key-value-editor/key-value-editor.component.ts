import { Component, Input, forwardRef, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ControlValueAccessor,
  FormArray,
  FormBuilder,
  FormGroup,
  NG_VALUE_ACCESSOR,
  ReactiveFormsModule
} from '@angular/forms';
import { Subscription } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';

@Component({
  selector: 'app-key-value-editor',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ButtonModule, InputTextModule, InputNumberModule],
  templateUrl: './key-value-editor.component.html',
  styleUrls: ['./key-value-editor.component.css'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => KeyValueEditorComponent),
      multi: true
    }
  ]
})
export class KeyValueEditorComponent implements ControlValueAccessor, OnDestroy {
  @Input() keyPlaceholder = 'Clé';
  @Input() valuePlaceholder = 'Valeur';
  @Input() addLabel = 'Ajouter';

  rows: FormArray;
  disabled = false;

  private onChange: (value: Record<string, number>) => void = () => {};
  private onTouched: () => void = () => {};
  private valueSub?: Subscription;

  constructor(private fb: FormBuilder) {
    this.rows = this.fb.array([]);
    this.valueSub = this.rows.valueChanges.subscribe(() => this.emitValue());
  }

  ngOnDestroy(): void {
    this.valueSub?.unsubscribe();
  }

  private buildRow(key: string, value: number | null): FormGroup {
    return this.fb.group({ key: [key], value: [value] });
  }

  private emitValue(): void {
    const result: Record<string, number> = {};
    for (const row of this.rows.value as { key: string; value: number | null }[]) {
      if (row.key && row.key.trim().length > 0 && row.value !== null && row.value !== undefined) {
        result[row.key.trim()] = row.value;
      }
    }
    this.onChange(result);
  }

  addRow(): void {
    this.rows.push(this.buildRow('', null));
  }

  removeRow(index: number): void {
    this.rows.removeAt(index);
    this.onTouched();
  }

  writeValue(value: Record<string, number> | null | undefined): void {
    this.rows.clear();
    if (value) {
      Object.keys(value).forEach(key => this.rows.push(this.buildRow(key, value[key])));
    }
  }

  registerOnChange(fn: (value: Record<string, number>) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.rows.disable();
    } else {
      this.rows.enable();
    }
  }
}
