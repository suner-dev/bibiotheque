import { Component, OnInit } from '@angular/core';
import { Reservation } from '../_model/reservation';
import { ReservationService } from '../_service/reservation.service';

@Component({
  selector: 'app-reservation-list',
  templateUrl: './reservation-list.component.html',
  styleUrls: ['./reservation-list.component.css']
})
export class ReservationListComponent implements OnInit {

  reservations: Reservation[] = [];
  selectedStatut: string = '';
  errorMessage: string = '';

  constructor(private reservationService: ReservationService) { }

  ngOnInit(): void {
    this.loadReservations();
  }

  loadReservations(): void {
    this.reservationService.getReservations(this.selectedStatut || undefined).subscribe(
      data => {
        this.reservations = data;
        this.errorMessage = '';
      },
      error => {
        this.errorMessage = 'Erreur lors du chargement des réservations';
        console.log(error);
      }
    );
  }

  filterByStatut(): void {
    this.loadReservations();
  }

  annulerReservation(id: number): void {
    this.reservationService.annulerReservation(id).subscribe(
      data => {
        console.log('Réservation annulée:', data);
        this.loadReservations();
      },
      error => {
        this.errorMessage = error.error?.message || 'Erreur lors de l\'annulation';
        console.log(error);
      }
    );
  }

  deleteReservation(id: number): void {
    if (confirm('Êtes-vous sûr de vouloir supprimer cette réservation ?')) {
      this.reservationService.deleteReservation(id).subscribe(
        () => {
          console.log('Réservation supprimée');
          this.loadReservations();
        },
        error => {
          this.errorMessage = error.error?.message || 'Erreur lors de la suppression';
          console.log(error);
        }
      );
    }
  }
}
