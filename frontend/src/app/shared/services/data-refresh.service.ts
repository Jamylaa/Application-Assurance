import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

export enum RefreshType {
  PRODUITS = 'PRODUITS',
  GARANTIES = 'GARANTIES',
  PACKS = 'PACKS',
  USERS = 'USERS',
  ALL = 'ALL'
}

@Injectable({
  providedIn: 'root'
})
export class DataRefreshService {
  private refreshSubject = new Subject<RefreshType>();

  refresh$ = this.refreshSubject.asObservable();

  refresh(type: RefreshType): void {
    this.refreshSubject.next(type);
  }

  refreshProduits(): void {
    this.refresh(RefreshType.PRODUITS);
  }

  refreshGaranties(): void {
    this.refresh(RefreshType.GARANTIES);
  }

  refreshPacks(): void {
    this.refresh(RefreshType.PACKS);
  }

  refreshUsers(): void {
    this.refresh(RefreshType.USERS);
  }

  refreshAll(): void {
    this.refresh(RefreshType.ALL);
  }
}
