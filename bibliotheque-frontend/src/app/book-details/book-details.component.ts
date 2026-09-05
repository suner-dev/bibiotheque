import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Books } from '../_model/books';
import { Borrow } from '../_model/borrow';
import { Users } from '../_model/users';
import { BooksService } from '../_service/books.service';
import { BorrowService } from '../_service/borrow.service';
import { UsersService } from '../_service/users.service';
import { ToastService } from '../_service/toast.service';

@Component({
  selector: 'app-book-details',
  templateUrl: './book-details.component.html',
  styleUrls: ['./book-details.component.css']
})
export class BookDetailsComponent implements OnInit {

  id: number;
  book: Books = new Books();
  borrow: Borrow[] = [];
  user: Users = new Users();

  constructor(
    private route: ActivatedRoute,
    private bookService: BooksService,
    private borrowService: BorrowService,
    public userService: UsersService,
    private toastService: ToastService
  ) { }

  ngOnInit(): void {
    this.id = this.route.snapshot.params['bookId'];

    this.bookService.getBookById(this.id).subscribe({
      next: (data) => this.book = data,
      error: (err) => {
        const msg = this.buildErrorMessage(err);
        this.toastService.error(msg, 'Erreur de chargement');
      }
    });

    this.getBorrowHistory(this.id);
  }

  private getBorrowHistory(bookId: number) {
    this.borrowService.getBookBorrowHistory(bookId).subscribe({
      next: (data) => this.borrow = data,
      error: (err) => {
        const msg = this.buildErrorMessage(err);
        this.toastService.error(msg, 'Erreur de chargement');
      }
    });
  }

  private buildErrorMessage(err: any): string {
    if (!err) return 'Une erreur inconnue est survenue.';
    if (err.status === 0) return 'Impossible de contacter le serveur. Vérifiez que le backend est démarré.';
    return err.error?.message || `Erreur (code ${err.status ?? 'inconnu'}).`;
  }
}
