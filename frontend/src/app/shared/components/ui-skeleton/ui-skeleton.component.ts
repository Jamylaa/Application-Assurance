import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-ui-skeleton',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ui-skeleton.component.html',
  styleUrls: ['./ui-skeleton.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class UiSkeletonComponent {
  @Input() type: 'card' | 'text' | 'circle' | 'table' | 'chart' = 'text';
  @Input() width = '100%';
  @Input() height = '20px';
  @Input() count = 1;
  @Input() rows: number[] = [];

  skeletons = Array(this.count).fill(null);

  constructor() {
    this.updateSkeletons();
  }

  ngOnInit(): void {
    this.updateSkeletons();
  }

  ngOnChanges(): void {
    this.updateSkeletons();
  }

  private updateSkeletons(): void {
    this.skeletons = Array(this.count).fill(null);
  }
}
