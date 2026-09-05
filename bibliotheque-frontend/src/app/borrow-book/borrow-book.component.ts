import { Component, OnInit } from '@angular/core';
import { Books } from '../_model/books';
import { Borrow } from '../_model/borrow';
import { BooksService } from '../_service/books.service';
import { BorrowService } from '../_service/borrow.service';
import { UserAuthService } from '../_service/user-auth.service';
import { ToastService } from '../_service/toast.service';

@Component({
  selector: 'app-borrow-book',
  templateUrl: './borrow-book.component.html',
  styleUrls: ['./borrow-book.component.css']
})
export class BorrowBookComponent implements OnInit {

  books: Books[] = [];

  constructor(
    private booksService: BooksService,
    private userAuthService: UserAuthService,
    private borrowService: BorrowService,
    private toastService: ToastService
  ) { }

  userId = this.userAuthService.getUserId();

  ngOnInit(): void {
    this.getBooks();
  }

  private getBooks() {
    this.booksService.getBooksList().subscribe({
      next: (data) => this.books = data,
      error: (err) => {
        const msg = this.buildErrorMessage(err);
        this.toastService.error(msg, 'Erreur de chargement');
      }
    });
  }

  borrow: Borrow = new Borrow();

  borrowBook(bookId: number) {
    this.borrow.bookId = bookId;
    this.borrow.userId = this.userId;
    this.borrowService.borrowBook(this.borrow).subscribe({
      next: (data: any) => {
        this.toastService.success(data || 'Livre emprunté avec succès !', 'Emprunt');
        this.getBooks();
      },
      error: (err) => {
        const msg = this.buildErrorMessage(err);
        this.toastService.error(msg, 'Erreur d\'emprunt');
      }
    });
  }

  private buildErrorMessage(err: any): string {
    if (!err) return 'Une erreur inconnue est survenue.';
    if (err.status === 0) return 'Impossible de contacter le serveur. Vérifiez que le backend est démarré.';
    return err.error?.message || err.error || `Erreur (code ${err.status ?? 'inconnu'}).`;
  }
}
