import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export type ToastType = 'success' | 'error' | 'warning' | 'info';

export interface Toast {
  id: number;
  type: ToastType;
  message: string;
  title?: string;
  duration: number;
}

@Injectable({
  providedIn: 'root'
})
export class ToastService {

  private counter = 0;
  private toastsSubject = new BehaviorSubject<Toast[]>([]);
  toasts$: Observable<Toast[]> = this.toastsSubject.asObservable();

  /** Durée par défaut (ms) */
  private defaultDuration = 4000;

  constructor() {}

  /** Affiche un toast */
  show(message: string, type: ToastType = 'info', options?: { title?: string; duration?: number }): void {
    const toast: Toast = {
      id: ++this.counter,
      type,
      message,
      title: options?.title,
      duration: options?.duration ?? this.defaultDuration
    };

    const current = this.toastsSubject.value;
    this.toastsSubject.next([...current, toast]);

    if (toast.duration > 0) {
      setTimeout(() => this.dismiss(toast.id), toast.duration);
    }
  }

  /** Raccourcis */
  success(message: string, title?: string): void {
    this.show(message, 'success', { title });
  }

  error(message: string, title?: string): void {
    this.show(message, 'error', { title, duration: 6000 });
  }

  warning(message: string, title?: string): void {
    this.show(message, 'warning', { title });
  }

  info(message: string, title?: string): void {
    this.show(message, 'info', { title });
  }

  /** Ferme un toast par son id */
  dismiss(id: number): void {
    const current = this.toastsSubject.value.filter(t => t.id !== id);
    this.toastsSubject.next(current);
  }

  /** Ferme tous les toasts */
  dismissAll(): void {
    this.toastsSubject.next([]);
  }
}
