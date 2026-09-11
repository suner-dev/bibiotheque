import { Component, Input, Output, EventEmitter } from '@angular/core';
import { Reservation } from '../_model/reservation';
import { UserAuthService } from '../_service/user-auth.service';

/**
 * Composant d'affichage de la liste des réservations.
 *
 * RS-02 : Le bouton de suppression est masqué pour les ADHERENT.
 * Seul le BIBLIOTHECAIRE peut supprimer des réservations.
 */
@Component({
  selector: 'app-reservation-list',
  templateUrl: './reservation-list.component.html',
  styleUrls: ['./reservation-list.component.css']
})
export class ReservationListComponent {

  @Input() reservations: Reservation[] = [];
  @Input() isLoading: boolean = false;
  @Input() isCancelling: boolean = false;
  @Input() errorMessage: string = '';
  @Input() selectedStatut: string = '';

  @Output() filterChange = new EventEmitter<string>();
  @Output() cancelRequested = new EventEmitter<number>();
  @Output() detailsRequested = new EventEmitter<number>();
  @Output() deleteRequested = new EventEmitter<number>();

  statuts = ['', 'EN_ATTENTE', 'DISPONIBLE', 'ANNULEE', 'EXPIREE', 'HONOREE'];

  constructor(private userAuthService: UserAuthService) {}

  /**
   * RS-02 : Vérifie si l'utilisateur connecté est un bibliothécaire (Admin).
   * Seul le bibliothécaire peut supprimer des réservations.
   */
  isBibliothecaire(): boolean {
    const roles = this.userAuthService.getRoles();
    if (!roles) return false;
    return roles.some((role: any) => role.roleName === 'Admin');
  }

  onFilterChange(): void {
    this.filterChange.emit(this.selectedStatut);
  }

  onCancel(id: number): void {
    if (confirm('Êtes-vous sûr de vouloir annuler cette réservation ?')) {
      this.cancelRequested.emit(id);
    }
  }

  onDetails(id: number): void {
    this.detailsRequested.emit(id);
  }

  onDelete(id: number): void {
    if (confirm('Êtes-vous sûr de vouloir supprimer définitivement cette réservation ?')) {
      this.deleteRequested.emit(id);
    }
  }

  canCancel(statut: string): boolean {
    return statut === 'EN_ATTENTE' || statut === 'DISPONIBLE';
  }

  formatStatut(statut: string): string {
    const statutLabels: { [key: string]: string } = {
      'EN_ATTENTE': 'En attente',
      'DISPONIBLE': 'Disponible',
      'ANNULEE': 'Annulée',
      'EXPIREE': 'Expirée',
      'HONOREE': 'Honorée'
    };
    return statutLabels[statut] || statut;
  }

  formatDate(dateVal: Date | string | any): string {
    if (!dateVal) return '-';
    const d = new Date(dateVal);
    return d.toLocaleString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }
}
