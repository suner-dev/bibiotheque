import { Component, Input, Output, EventEmitter } from '@angular/core';
import { Reservation } from '../_model/reservation';

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

  statuts = ['', 'EN_ATTENTE', 'DISPONIBLE', 'ANNULEE', 'EXPIREE', 'HONOREE'];

  onFilterChange(): void {
    this.filterChange.emit(this.selectedStatut);
  }

  onCancel(id: number): void {
    if (confirm('Êtes-vous sûr de vouloir annuler cette réservation ?')) {
      this.cancelRequested.emit(id);
    }
  }

  canCancel(statut: string): boolean {
    return statut === 'EN_ATTENTE' || statut === 'DISPONIBLE';
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
    return d.toLocaleString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }
}
