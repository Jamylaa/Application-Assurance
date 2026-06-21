import { Component, Input, Output, EventEmitter, TemplateRef, ContentChild, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';

@Component({
  selector: 'app-ui-modal',
  standalone: true,
  imports: [CommonModule, ButtonModule],
  templateUrl: './ui-modal.component.html',
  styleUrls: ['./ui-modal.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class UiModalComponent {
  @Input() visible = false;
  @Input() title = '';
  @Input() width = '500px';
  @Input() position: 'center' | 'top' = 'center';
  @Input() showHeader = true;
  @Input() showFooter = true;
  @Input() closeButtonLabel = 'Annuler';
  @Input() submitButtonLabel = 'Valider';
  @Input() submitting = false;
  @Input() closable = true;
  @Input() modal = true;

  @Output() onClose = new EventEmitter<void>();
  @Output() onSubmit = new EventEmitter<void>();

  @ContentChild('header') headerTemplate?: TemplateRef<any>;
  @ContentChild('body') bodyTemplate?: TemplateRef<any>;
  @ContentChild('footer') footerTemplate?: TemplateRef<any>;

  close(): void {
    this.onClose.emit();
  }

  submit(): void {
    if (!this.submitting) {
      this.onSubmit.emit();
    }
  }

  onBackdropClick(): void {
    if (this.closable && this.modal) {
      this.close();
    }
  }
}
