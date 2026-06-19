import { Component, Input, Output, EventEmitter, HostBinding } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-ui-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ui-card.component.html',
  styleUrls: ['./ui-card.component.scss']
})
export class UiCardComponent {
  @Input() title?: string;
  @Input() subtitle?: string;
  @Input() icon?: string;
  @Input() variant: 'default' | 'primary' | 'success' | 'warning' | 'error' = 'default';
  @Input() hoverable: boolean = true;
  @Input() clickable: boolean = false;
  @Input() loading: boolean = false;
  @Input() padding: 'none' | 'sm' | 'md' | 'lg' = 'md';
  @Input() shadow: 'none' | 'sm' | 'md' | 'lg' | 'xl' = 'md';

  @Output() cardClick = new EventEmitter<void>();
  @Output() cardHover = new EventEmitter<boolean>();

  @HostBinding('class.clickable') get isClickable() {
    return this.clickable;
  }

  @HostBinding('class.loading') get isLoading() {
    return this.loading;
  }

  @HostBinding('class.hoverable') get isHoverable() {
    return this.hoverable;
  }

  onCardClick(): void {
    if (this.clickable) {
      this.cardClick.emit();
    }
  }

  onCardHover(hovering: boolean): void {
    if (this.hoverable) {
      this.cardHover.emit(hovering);
    }
  }

  getIconClass(): string {
    const baseClass = 'card-icon';
    return `${baseClass} ${baseClass}--${this.variant}`;
  }

  getPaddingClass(): string {
    return `card--padding-${this.padding}`;
  }

  getShadowClass(): string {
    return `card--shadow-${this.shadow}`;
  }
}
