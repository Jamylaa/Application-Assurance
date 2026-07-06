import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { LayoutModule } from '../layout/layout.module';
import { SharedModule } from '../shared/shared.module';

import { DashboardComponent } from './dashboard/dashboard.component';
import { ProduitsComponent } from './produits/produits.component';
import { PacksComponent } from './packs/packs.component';
import { GarantiesComponent } from './garanties/garanties.component';

@NgModule({
  declarations: [],
  imports: [
    CommonModule,
    RouterModule,
    LayoutModule,
    SharedModule,
    DashboardComponent,
    ProduitsComponent,
    PacksComponent,
    GarantiesComponent
  ],
  exports: [
    DashboardComponent,
    ProduitsComponent,
    PacksComponent,
    GarantiesComponent
  ]
})
export class PagesModule { }