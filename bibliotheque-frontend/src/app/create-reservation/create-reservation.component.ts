import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { Books } from '../_model/books';
import { Users } from '../_model/users';
import { ReservationRequest } from '../_model/reservation';
import { ReservationService } from '../_service/reservation.service';

@Component({
  selector: 'app-create-reservation',
  templateUrl: './create-reservation.component.html',
  styleUrls: ['./create-reservation.component.css']
})
export class CreateReservationComponent implements OnInit {

  @Input() books: Books[] = [];
  @Input() users: Users[] = [];
  @Output() reservationCreated = new EventEmitter<void>();

  request: ReservationRequest = new ReservationRequest();
  errorMessage: string = '';
  successMessage: string = '';
    isSubmitting: boolean = false;

  constructor(private reservationService: ReservationService) {}

  ngOnInit(): void {
    this.request = new ReservationRequest();
  }

  /** Le bouton est inactif tant que les deux champs ne sont pas renseignés */
  get canSubmit(): boolean {
    return !!(this.request.livreId && this.request.adherentId);
  }

  onSubmit(): void {
    if (!this.canSubmit) {
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.reservationService.createReservation(this.request).subscribe({
      next: (data: any) => {
        this.isSubmitting = false;
        this.successMessage = 'Réservation créée avec succès !';
        this.request = new ReservationRequest();
        setTimeout(() => {
          this.successMessage = '';
          this.reservationCreated.emit();
        }, 1500);
      },
      error: (err) => {
        this.isSubmitting = false;
        this.errorMessage = this.parseError(err);
      }
    });
  }

  /** Parse les erreurs métier du serveur */
  private parseError(err: any): string {
    const status = err?.status;
    const msg = err?.error?.message;

    if (status === 0) {
      return 'Impossible de contacter le serveur. Vérifiez que le backend est démarré.';
    }
    if (status === 409) {
      // Messages métier spécifiques
      if (msg && msg.includes('disponible')) {
        return '⚠️ Ce livre est disponible. Vous ne pouvez pas le réserver (RG-01).';
      }
      if (msg && msg.includes('déjà une réservation')) {
        return '⚠️ Vous avez déjà une réservation active pour ce livre (RG-02).';
      }
      if (msg && msg.includes('3 réservations')) {
        return '⚠️ Vous avez atteint le quota maximum de 3 réservations actives (RG-03).';
      }
      return msg || 'Conflit métier : la réservation ne peut pas être créée.';
    }
    if (status === 400) {
      return msg || 'Champs manquant ou invalide. Veuillez vérifier le formulaire.';
    }
    if (status === 404) {
      return 'Livre ou adhérent introuvable.';
    }
    return msg || 'Erreur lors de la création de la réservation.';
  }
}
