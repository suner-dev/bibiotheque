import { Component, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
import { Books } from '../_model/books';
import { Borrow } from '../_model/borrow';
import { BooksService } from '../_service/books.service';
import { BorrowService } from '../_service/borrow.service';
import { UserAuthService } from '../_service/user-auth.service';
import { ToastService } from '../_service/toast.service';

@Component({
  selector: 'app-return-book',
  templateUrl: './return-book.component.html',
  styleUrls: ['./return-book.component.css']
})
export class ReturnBookComponent implements OnInit {

  books: Books[] = [];
  borrow: Borrow[] = [];

  constructor(
    private borrowService: BorrowService,
    private booksService: BooksService,
    private userAuthService: UserAuthService,
    private toastService: ToastService
  ) { }

  userId = this.userAuthService.getUserId();

  ngOnInit(): void {
    this.getBooks();
    this.getBooksByUser();
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

  private getBooksByUser() {
    this.borrowService.getBooksBorrowedByUser(this.userId).subscribe({
      next: (data) => this.borrow = data,
      error: (err) => {
        const msg = this.buildErrorMessage(err);
        this.toastService.error(msg, 'Erreur de chargement');
      }
    });
  }

  brw: Borrow = new Borrow();
  public returnBook(borrowId: number) {
    this.brw.borrowId = borrowId;
    this.borrowService.returnBook(this.brw).subscribe({
      next: (data) => {
        this.toastService.success('Livre retourné avec succès !', 'Retour');
        this.getBooksByUser();
        this.getBooks();
      },
      error: (err) => {
        const msg = this.buildErrorMessage(err);
        this.toastService.error(msg, 'Erreur de retour');
      }
    });
  }

  private buildErrorMessage(err: any): string {
    if (!err) return 'Une erreur inconnue est survenue.';
    if (err.status === 0) return 'Impossible de contacter le serveur. Vérifiez que le backend est démarré.';
    return err.error?.message || err.error || `Erreur (code ${err.status ?? 'inconnu'}).`;
  }
}
