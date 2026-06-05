import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ToastComponent } from './toast.component';
import { ToastService } from '../../core/toast.service';

describe('ToastComponent', () => {
  let component: ToastComponent;
  let fixture: ComponentFixture<ToastComponent>;
  let toastService: jasmine.SpyObj<ToastService>;

  beforeEach(async () => {
    const spy = jasmine.createSpyObj('ToastService', ['toasts', 'remove']);

    await TestBed.configureTestingModule({
      imports: [ToastComponent],
      providers: [
        { provide: ToastService, useValue: spy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ToastComponent);
    component = fixture.componentInstance;
    toastService = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have empty toasts array initially', () => {
    expect(component.toasts).toEqual([]);
  });

  it('should return correct icon for success type', () => {
    expect(component.getIcon('success')).toBe('bi-check-circle-fill');
  });

  it('should return correct icon for error type', () => {
    expect(component.getIcon('error')).toBe('bi-x-circle-fill');
  });

  it('should return correct icon for warning type', () => {
    expect(component.getIcon('warning')).toBe('bi-exclamation-triangle-fill');
  });

  it('should return correct icon for info type', () => {
    expect(component.getIcon('info')).toBe('bi-info-circle-fill');
  });

  it('should return default icon for unknown type', () => {
    expect(component.getIcon('unknown')).toBe('bi-info-circle-fill');
  });
});
