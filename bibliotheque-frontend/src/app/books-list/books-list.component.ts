import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Books } from '../_model/books'
import { BooksService } from '../_service/books.service';
import { ToastService } from '../_service/toast.service';

@Component({
  selector: 'app-books-list',
  templateUrl: './books-list.component.html',
  styleUrls: ['./books-list.component.css']
})
export class BooksListComponent implements OnInit {

  books: Books[] = [];

  constructor(
    private booksService: BooksService,
    private router: Router,
    private toastService: ToastService
  ) { }

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

  updateBook(bookId: number) {
    this.router.navigate(['update-book', bookId ]);
  }

  deleteBook(bookId: number) {
    this.booksService.deleteBook(bookId).subscribe({
      next: () => {
        this.toastService.success('Livre supprimé avec succès.', 'Suppression');
        this.getBooks();
      },
      error: (err) => {
        const msg = this.buildErrorMessage(err);
        this.toastService.error(msg, 'Suppression impossible');
      }
    });
  }

  bookDetails(bookId: number) {
    this.router.navigate(['book-details', bookId ]);
  }

  private buildErrorMessage(err: any): string {
    if (!err) return 'Une erreur inconnue est survenue.';
    if (err.status === 0) return 'Impossible de contacter le serveur. Vérifiez que le backend est démarré.';
    return err.error?.message || `Erreur (code ${err.status ?? 'inconnu'}).`;
  }
}
