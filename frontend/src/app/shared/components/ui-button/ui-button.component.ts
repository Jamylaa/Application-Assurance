import { Component, Input, Output, EventEmitter, HostBinding, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'button[app-ui-button], a[app-ui-button]',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ui-button.component.html',
  styleUrls: ['./ui-button.component.scss']
})
export class UiButtonComponent {
  @Input() variant: 'primary' | 'secondary' | 'success' | 'warning' | 'error' | 'ghost' | 'link' = 'primary';
  @Input() size: 'xs' | 'sm' | 'md' | 'lg' | 'xl' = 'md';
  @Input() icon?: string;
  @Input() iconPosition: 'left' | 'right' = 'left';
  @Input() loading: boolean = false;
  @Input() disabled: boolean = false;
  @Input() fullWidth: boolean = false;
  @Input() rounded: boolean = false;
  @Input() outlined: boolean = false;

  @Output() buttonClick = new EventEmitter<Event>();

  @HostBinding('class.btn-loading') get isLoading() {
    return this.loading;
  }

  @HostBinding('class.btn-disabled') get isDisabled() {
    return this.disabled || this.loading;
  }

  @HostBinding('class.btn-full-width') get isFullWidth() {
    return this.fullWidth;
  }

  @HostBinding('class.btn-rounded') get isRounded() {
    return this.rounded;
  }

  @HostBinding('class.btn-outlined') get isOutlined() {
    return this.outlined;
  }

  @HostBinding('disabled') get hostDisabled() {
    return this.isDisabled;
  }

  @HostBinding('attr.type') get buttonType() {
    return 'button';
  }

  @HostListener('click', ['$event'])
  onClick(event: Event): void {
    if (!this.isDisabled) {
      this.buttonClick.emit(event);
    }
  }

  getVariantClass(): string {
    return `btn--${this.variant}`;
  }

  getSizeClass(): string {
    return `btn--${this.size}`;
  }

  getIconClass(): string {
    const baseClass = 'btn-icon';
    return `${baseClass} btn-icon--${this.iconPosition}`;
  }
}
