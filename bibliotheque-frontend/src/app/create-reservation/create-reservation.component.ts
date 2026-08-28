import { Component, Input, Output, EventEmitter, OnInit, ViewChild } from '@angular/core';
import { NgForm } from '@angular/forms';
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

  @ViewChild('reservationForm') reservationForm?: NgForm;

  request: ReservationRequest = new ReservationRequest();
  errorMessage: string = '';
  successMessage: string = '';
  isSubmitting: boolean = false;

  constructor(private reservationService: ReservationService) {}

  ngOnInit(): void {
    this.request = new ReservationRequest();
  }

  onSubmit(form: NgForm): void {
    if (form.invalid || this.isSubmitting) {
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';
    this.successMessage = '';

    // ngModel fournit des strings : on normalise en nombres pour l'API
    const payload: ReservationRequest = {
      livreId: Number(form.value.livreId),
      adherentId: Number(form.value.adherentId)
    };

    this.reservationService.createReservation(payload).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.successMessage = 'Réservation créée avec succès !';
        // Réinitialisation canonique Angular : resynchronise le modèle ET les champs du DOM
        form.resetForm(new ReservationRequest());
        setTimeout(() => {
          this.successMessage = '';
          this.reservationCreated.emit();
        }, 1200);
      },
      error: (err) => {
        this.isSubmitting = false;
        this.errorMessage = this.parseError(err);
      }
    });
  }

  /** Parse les erreurs métier du serveur : le message réel du serveur est privilégié. */
  private parseError(err: any): string {
    const status = err?.status;
    const msg: string | undefined = err?.error?.message;

    if (status === 0) {
      return 'Impossible de contacter le serveur. Vérifiez que le backend est démarré.';
    }
    if (status === 409) {
      return msg || 'Conflit métier : la réservation ne peut pas être créée.';
    }
    if (status === 400) {
      return msg || 'Champ manquant ou invalide. Veuillez vérifier le formulaire.';
    }
    if (status === 404) {
      return 'Livre ou adhérent introuvable.';
    }
    return msg || `Erreur lors de la création de la réservation (code ${status ?? 'inconnu'}).`;
  }
}
