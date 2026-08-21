import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Books } from '../_model/books';
import { Users } from '../_model/users';
import { ReservationRequest } from '../_model/reservation';
import { BooksService } from '../_service/books.service';
import { UsersService } from '../_service/users.service';
import { ReservationService } from '../_service/reservation.service';
import { UserAuthService } from '../_service/user-auth.service';

@Component({
  selector: 'app-create-reservation',
  templateUrl: './create-reservation.component.html',
  styleUrls: ['./create-reservation.component.css']
})
export class CreateReservationComponent implements OnInit {

  books: Books[] = [];
  users: Users[] = [];
  request: ReservationRequest = new ReservationRequest();
  errorMessage: string = '';
  successMessage: string = '';

  constructor(
    private booksService: BooksService,
    private usersService: UsersService,
    private reservationService: ReservationService,
    private userAuthService: UserAuthService,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.loadBooks();
    this.loadUsers();
    // Set current user as adherent
    this.request.adherentId = this.userAuthService.getUserId();
  }

  loadBooks(): void {
    this.booksService.getBooksList().subscribe(data => {
      this.books = data;
    });
  }

  loadUsers(): void {
    this.usersService.getUsersList().subscribe(data => {
      this.users = data;
    });
  }

  onSubmit(): void {
    this.errorMessage = '';
    this.successMessage = '';

    this.reservationService.createReservation(this.request).subscribe(
      data => {
        console.log('Réservation créée:', data);
        this.successMessage = 'Réservation créée avec succès!';
        setTimeout(() => {
          this.router.navigate(['/reservations']);
        }, 2000);
      },
      error => {
        if (error.error?.message) {
          this.errorMessage = error.error.message;
        } else if (error.status === 409) {
          this.errorMessage = 'Conflit: ' + (error.error?.message || 'Règle métier violée');
        } else if (error.status === 404) {
          this.errorMessage = 'Ressource introuvable';
        } else {
          this.errorMessage = 'Erreur lors de la création de la réservation';
        }
        console.log(error);
      }
    );
  }
}
