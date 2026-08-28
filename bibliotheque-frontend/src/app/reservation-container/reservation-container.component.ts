import { Component, OnInit } from '@angular/core';
import { Reservation } from '../_model/reservation';
import { Books } from '../_model/books';
import { Users } from '../_model/users';
import { ReservationService } from '../_service/reservation.service';
import { BooksService } from '../_service/books.service';
import { UsersService } from '../_service/users.service';

@Component({
  selector: 'app-reservation-container',
  templateUrl: './reservation-container.component.html',
    styleUrls: ['./reservation-container.component.css']
})
export class ReservationContainerComponent implements OnInit {

  reservations: Reservation[] = [];
  books: Books[] = [];
  users: Users[] = [];

  isLoading = true;
  isCancelling = false;
  errorMessage = '';
  selectedStatut = '';

  constructor(
    private reservationService: ReservationService,
    private booksService: BooksService,
    private usersService: UsersService
  ) {}

  ngOnInit(): void {
    this.loadBooks();
    this.loadUsers();
    this.loadReservations();
  }

  loadBooks(): void {
    this.booksService.getBooksList().subscribe({
      next: (data) => this.books = data,
      error: (err) => console.error('Erreur chargement livres:', err)
    });
  }

  loadUsers(): void {
    this.usersService.getUsersList().subscribe({
      next: (data) => this.users = data,
      error: (err) => console.error('Erreur chargement utilisateurs:', err)
    });
  }

  loadReservations(): void {
    this.isLoading = true;
    this.errorMessage = '';
    const statut = this.selectedStatut || undefined;
    this.reservationService.getReservations(statut).subscribe({
      next: (data) => {
        this.reservations = data;
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = this.buildErrorMessage(err);
        this.isLoading = false;
      }
    });
  }

  onFilterChange(statut: string): void {
    this.selectedStatut = statut;
    this.loadReservations();
  }

  onReservationCreated(): void {
    this.loadReservations();
  }

  onCancelReservation(id: number): void {
    this.isCancelling = true;
    this.errorMessage = '';
    this.reservationService.annulerReservation(id).subscribe({
      next: () => {
        this.isCancelling = false;
        this.loadReservations();
      },
      error: (err) => {
        this.isCancelling = false;
        this.errorMessage = this.buildErrorMessage(err);
      }
    });
  }

  retry(): void {
    this.loadReservations();
  }

  private buildErrorMessage(err: any): string {
    if (!err) {
      return 'Une erreur inconnue est survenue.';
    }
    if (err.status === 0) {
      return 'Impossible de contacter le serveur. Vérifiez que le backend est bien démarré (port 8087).';
    }
    if (err.status === 409) {
      return err.error?.message || 'Conflit métier : cette réservation ne peut pas être annulée.';
    }
    return err.error?.message || 'Erreur lors du chargement des réservations.';
  }
}
