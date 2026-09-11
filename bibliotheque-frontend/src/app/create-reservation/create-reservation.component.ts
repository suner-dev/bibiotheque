import { Component, Input, Output, EventEmitter, OnInit, ViewChild } from '@angular/core';
import { NgForm } from '@angular/forms';
import { Books } from '../_model/books';
import { Users } from '../_model/users';
import { ReservationRequest } from '../_model/reservation';
import { ReservationService } from '../_service/reservation.service';
import { ToastService } from '../_service/toast.service';
import { UserAuthService } from '../_service/user-auth.service';

/**
 * Composant de création de réservation.
 *
 * RS-04 : Pour un ADHERENT, le champ adhérent est masqué.
 * L'identité est automatiquement récupérée depuis l'utilisateur connecté.
 * Le backend vérifie également que l'identité vient du token.
 */
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
  isSubmitting: boolean = false;

  constructor(
    private reservationService: ReservationService,
    private toastService: ToastService,
    private userAuthService: UserAuthService
  ) {}

  ngOnInit(): void {
    this.request = new ReservationRequest();
  }

  /**
   * RS-04 : Vérifie si l'utilisateur connecté est un bibliothécaire (Admin).
   * Les bibliothécaires peuvent choisir un adhérent pour la réservation.
   */
  isBibliothecaire(): boolean {
    const roles = this.userAuthService.getRoles();
    if (!roles) return false;
    return roles.some((role: any) => role.roleName === 'Admin');
  }

  /**
   * RS-04 : Pour un ADHERENT, l'identité est automatiquement définie.
   * Le backend ignorera toute valeur frauduleuse et utilisera l'identité du token.
   */
  getCurrentAdherentId(): number | null {
    return this.userAuthService.getUserId();
  }

  onSubmit(form: NgForm): void {
    if (form.invalid || this.isSubmitting) {
      return;
    }

    this.isSubmitting = true;

    // RS-04 : Pour un ADHERENT, on utilise l'identité du token (pas du formulaire)
    // Le backend vérifiera que l'identité vient bien du token JWT
    const adherentId = this.isBibliothecaire()
      ? Number(form.value.adherentId)
      : (this.getCurrentAdherentId() ?? 0);

    // ngModel fournit des strings : on normalise en nombres pour l'API
    const payload: ReservationRequest = {
      livreId: Number(form.value.livreId),
      adherentId: adherentId
    };

    this.reservationService.createReservation(payload).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.toastService.success('Réservation créée avec succès !', 'Réservation');
        // Réinitialisation canonique Angular : resynchronise le modèle ET les champs du DOM
        form.resetForm(new ReservationRequest());
        this.reservationCreated.emit();
      },
      error: (err) => {
        this.isSubmitting = false;
        const msg = this.parseError(err);
        this.toastService.error(msg, 'Erreur de réservation');
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
    if (status === 401) {
      return 'Votre session a expiré. Veuillez vous reconnecter.';
    }
    if (status === 403) {
      return 'Vous n\'avez pas les droits nécessaires pour effectuer cette action.';
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
