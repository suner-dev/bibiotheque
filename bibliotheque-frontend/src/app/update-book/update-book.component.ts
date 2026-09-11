import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable } from 'rxjs';
import { Books } from '../_model/books';
import { BooksService } from '../_service/books.service';
import { ToastService } from '../_service/toast.service';

@Component({
  selector: 'app-update-book',
  templateUrl: './update-book.component.html',
  styleUrls: ['./update-book.component.css']
})
export class UpdateBookComponent implements OnInit {

  bookId: number;
  book: Books = new Books();

  constructor(
    private booksService: BooksService,
    private route: ActivatedRoute,
    private router: Router,
    private toastService: ToastService
  ) { }

  ngOnInit(): void {
    this.bookId = this.route.snapshot.params['bookId'];
    this.booksService.getBookById(this.bookId).subscribe({
      next: (data) => this.book = data,
      error: (err) => {
        const msg = this.buildErrorMessage(err);
        this.toastService.error(msg, 'Erreur de chargement');
      }
    });
  }

  onSubmit() {
    this.booksService.updateBook(this.bookId, this.book).subscribe({
      next: (data) => {
        this.toastService.success('Livre modifié avec succès !', 'Modification');
        this.goToBooksList();
      },
      error: (err) => {
        const msg = this.buildErrorMessage(err);
        this.toastService.error(msg, 'Erreur de modification');
      }
    });
  }

  goToBooksList() {
    this.router.navigate(['/books']);
  }

  private buildErrorMessage(err: any): string {
    if (!err) return 'Une erreur inconnue est survenue.';
    if (err.status === 0) return 'Impossible de contacter le serveur. Vérifiez que le backend est démarré.';
    return err.error?.message || `Erreur (code ${err.status ?? 'inconnu'}).`;
  }
}
