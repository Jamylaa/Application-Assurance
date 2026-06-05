import { TestBed } from '@angular/core/testing';
import { ToastService } from './toast.service';
import { take } from 'rxjs/operators';
import { Toast } from './toast.service';

describe('ToastService', () => {
  let service: ToastService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ToastService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should show success toast', (done) => {
    service.success('Operation completed');
    service.toasts.pipe(take(1)).subscribe(toasts => {
      expect(toasts.length).toBe(1);
      expect(toasts[0].type).toBe('success');
      expect(toasts[0].message).toBe('Operation completed');
      done();
    });
  });

  it('should show error toast', (done) => {
    service.error('Operation failed');
    service.toasts.pipe(take(1)).subscribe(toasts => {
      expect(toasts.length).toBe(1);
      expect(toasts[0].type).toBe('error');
      expect(toasts[0].message).toBe('Operation failed');
      done();
    });
  });

  it('should show warning toast', (done) => {
    service.warning('Warning message');
    service.toasts.pipe(take(1)).subscribe(toasts => {
      expect(toasts.length).toBe(1);
      expect(toasts[0].type).toBe('warning');
      expect(toasts[0].message).toBe('Warning message');
      done();
    });
  });

  it('should show info toast', (done) => {
    service.info('Information message');
    service.toasts.pipe(take(1)).subscribe(toasts => {
      expect(toasts.length).toBe(1);
      expect(toasts[0].type).toBe('info');
      expect(toasts[0].message).toBe('Information message');
      done();
    });
  });

  it('should remove toast by id', (done) => {
    let toastId: string | null = null;
    service.success('Test message');

    service.toasts.pipe(take(1)).subscribe(toasts => {
      toastId = toasts[0].id;
      service.remove(toastId);

      service.toasts.pipe(take(1)).subscribe((updatedToasts: Toast[]) => {
        expect(updatedToasts.length).toBe(0);
        done();
      });
    });
  });

  it('should clear all toasts', (done) => {
    service.success('Message1');
    service.error('Message2');
    service.clear();

    service.toasts.pipe(take(1)).subscribe(toasts => {
      expect(toasts.length).toBe(0);
      done();
    });
  });
});
