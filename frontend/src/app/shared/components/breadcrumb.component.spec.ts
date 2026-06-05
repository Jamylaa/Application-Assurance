import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BreadcrumbComponent } from './breadcrumb.component';
import { BreadcrumbService } from '../services/breadcrumb.service';
import { of } from 'rxjs';

describe('BreadcrumbComponent', () => {
  let component: BreadcrumbComponent;
  let fixture: ComponentFixture<BreadcrumbComponent>;
  let breadcrumbService: jasmine.SpyObj<BreadcrumbService>;

  beforeEach(async () => {
    const spy = jasmine.createSpyObj('BreadcrumbService', ['breadcrumb$'], {
      breadcrumb$: of([
        { label: 'Home', routerLink: ['/dashboard'], icon: 'pi pi-home' },
        { label: 'Products', routerLink: ['/produits'], icon: 'pi pi-box' }
      ])
    });

    await TestBed.configureTestingModule({
      imports: [BreadcrumbComponent],
      providers: [
        { provide: BreadcrumbService, useValue: spy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(BreadcrumbComponent);
    component = fixture.componentInstance;
    breadcrumbService = TestBed.inject(BreadcrumbService) as jasmine.SpyObj<BreadcrumbService>;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize breadcrumb items', () => {
    expect(component.breadcrumbItems.length).toBe(2);
    expect(component.breadcrumbItems[0].label).toBe('Home');
  });

  it('should map breadcrumb items correctly', () => {
    const firstItem = component.breadcrumbItems[0];
    expect(firstItem.label).toBe('Home');
    expect(firstItem.routerLink).toEqual(['/dashboard']);
  });
});
