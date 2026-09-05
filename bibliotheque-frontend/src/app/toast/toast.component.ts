import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import { Toast, ToastService } from '../_service/toast.service';
import { trigger, transition, style, animate } from '@angular/animations';

@Component({
  selector: 'app-toast',
  templateUrl: './toast.component.html',
  styleUrls: ['./toast.component.css'],
  animations: [
    trigger('toastAnimation', [
      // Entrée depuis la droite
      transition(':enter', [
        style({ opacity: 0, transform: 'translateX(80px) scale(0.9)', filter: 'blur(4px)' }),
        animate('400ms cubic-bezier(0.22, 1, 0.36, 1)', style({
          opacity: 1,
          transform: 'translateX(0) scale(1)',
          filter: 'blur(0px)'
        }))
      ]),
      // Sortie vers la droite
      transition(':leave', [
        animate('300ms cubic-bezier(0.4, 0, 1, 1)', style({
          opacity: 0,
          transform: 'translateX(80px) scale(0.9)',
          filter: 'blur(4px)'
        }))
      ])
    ])
  ]
})
export class ToastComponent implements OnInit, OnDestroy {

  toasts: Toast[] = [];
  private sub = new Subscription();

  constructor(private toastService: ToastService) {}

  ngOnInit(): void {
    this.sub.add(
      this.toastService.toasts$.subscribe(toasts => {
        this.toasts = toasts;
      })
    );
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
  }

  dismiss(id: number): void {
    this.toastService.dismiss(id);
  }

  getIcon(type: string): string {
    const icons: Record<string, string> = {
      success: 'bi-check-circle-fill',
      error: 'bi-x-circle-fill',
      warning: 'bi-exclamation-triangle-fill',
      info: 'bi-info-circle-fill'
    };
    return icons[type] || 'bi-info-circle-fill';
  }

  getTitle(toast: Toast): string {
    if (toast.title) return toast.title;
    const titles: Record<string, string> = {
      success: 'Succès',
      error: 'Erreur',
      warning: 'Attention',
      info: 'Information'
    };
    return titles[toast.type] || 'Notification';
  }
}
