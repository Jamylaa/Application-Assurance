import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface Toast {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  message: string;
  duration?: number;
}

@Injectable({
  providedIn: 'root'
})
export class ToastService {
  private toasts$ = new BehaviorSubject<Toast[]>([]);
  private toastId = 0;

  get toasts() {
    return this.toasts$.asObservable();
  }

  show(type: Toast['type'], message: string, duration: number = 3000) {
    const toast: Toast = {
      id: `${this.toastId++}`,
      type,
      message,
      duration
    };

    const current = this.toasts$.value;
    this.toasts$.next([...current, toast]);

    if (duration > 0) {
      setTimeout(() => this.remove(toast.id), duration);
    }
  }

  success(message: string, duration?: number) {
    this.show('success', message, duration);
  }

  error(message: string, duration?: number) {
    this.show('error', message, duration);
  }

  warning(message: string, duration?: number) {
    this.show('warning', message, duration);
  }

  info(message: string, duration?: number) {
    this.show('info', message, duration);
  }

  remove(id: string) {
    const current = this.toasts$.value;
    this.toasts$.next(current.filter(t => t.id !== id));
  }

  clear() {
    this.toasts$.next([]);
  }
}
