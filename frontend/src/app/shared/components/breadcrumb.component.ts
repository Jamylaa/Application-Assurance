import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { Subscription } from 'rxjs';
import { BreadcrumbService, BreadcrumbItem } from '../services/breadcrumb.service';

@Component({
  selector: 'app-breadcrumb',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    BreadcrumbModule
  ],
  template: `
    <div class="breadcrumb-container" *ngIf="breadcrumbItems.length > 0">
      <p-breadcrumb [model]="breadcrumbItems"></p-breadcrumb>
    </div>
  `,
  styleUrls: ['./breadcrumb.component.css']
})
export class BreadcrumbComponent implements OnInit, OnDestroy {
  breadcrumbItems: any[] = [];
  private breadcrumbSubscription!: Subscription;

  constructor(private breadcrumbService: BreadcrumbService) {}

  ngOnInit(): void {
    this.breadcrumbSubscription = this.breadcrumbService.breadcrumb$.subscribe(
      (items: BreadcrumbItem[]) => {
        this.breadcrumbItems = items.map(item => ({
          label: item.label,
          routerLink: item.routerLink,
          url: item.url,
          icon: item.icon
        }));
      }
    );
  }

  ngOnDestroy(): void {
    if (this.breadcrumbSubscription) {
      this.breadcrumbSubscription.unsubscribe();
    }
  }
}
