import { Component, Input, Output, EventEmitter, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MultiSelectModule } from 'primeng/multiselect';
import { FormsModule } from '@angular/forms';

interface Option {
  label: string;
  value: any;
  icon?: string;
}

@Component({
  selector: 'app-ui-select',
  standalone: true,
  imports: [CommonModule, MultiSelectModule, FormsModule],
  templateUrl: './ui-select.component.html',
  styleUrls: ['./ui-select.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class UiSelectComponent {
  @Input() label = '';
  @Input() placeholder = 'Sélectionner...';
  @Input() options: Option[] = [];
  @Input() selectedItems: any[] = [];
  @Input() error?: string;
  @Input() disabled = false;
  @Input() required = false;
  @Input() showClear = true;
  @Input() maxSelectedLabels = 3;
  @Input() displayLimit = 1;
  @Input() help?: string;
  @Input() size: 'sm' | 'md' | 'lg' = 'md';

  @Output() selectedItemsChange = new EventEmitter<any[]>();

  onSelectionChange(values: any[]): void {
    this.selectedItemsChange.emit(values);
  }
}
