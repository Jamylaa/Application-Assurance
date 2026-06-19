import { Component, Input, HostBinding } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-ui-badge',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ui-badge.component.html',
  styleUrls: ['./ui-badge.component.scss']
})
export class UiBadgeComponent {
  @Input() variant: 'primary' | 'secondary' | 'success' | 'warning' | 'error' | 'info' | 'neutral' = 'primary';
  @Input() size: 'xs' | 'sm' | 'md' | 'lg' = 'md';
  @Input() icon?: string;
  @Input() outlined: boolean = false;
  @Input() dot: boolean = false;

  @HostBinding('class.badge-outlined') get isOutlined() {
    return this.outlined;
  }

  @HostBinding('class.badge-dot') get isDot() {
    return this.dot;
  }

  getVariantClass(): string {
    return `badge--${this.variant}`;
  }

  getSizeClass(): string {
    return `badge--${this.size}`;
  }
}
