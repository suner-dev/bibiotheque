import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Books } from '../_model/books';
import { BooksService } from '../_service/books.service';
import { ToastService } from '../_service/toast.service';

@Component({
  selector: 'app-create-book',
  templateUrl: './create-book.component.html',
  styleUrls: ['./create-book.component.css']
})
export class CreateBookComponent implements OnInit {

  book: Books = new Books();

  constructor(
    private booksService: BooksService,
    private router: Router,
    private toastService: ToastService
  ) { }

  ngOnInit(): void {
  }

  saveBook() {
    this.booksService.createBook(this.book).subscribe({
      next: (data) => {
        this.toastService.success('Livre ajouté avec succès !', 'Création');
        this.goToBooksList();
      },
      error: (err) => {
        const msg = this.buildErrorMessage(err);
        this.toastService.error(msg, 'Erreur de création');
      }
    });
  }

  goToBooksList() {
    this.router.navigate(['/books']);
  }

  onSubmit() {
    this.saveBook();
  }

  private buildErrorMessage(err: any): string {
    if (!err) return 'Une erreur inconnue est survenue.';
    if (err.status === 0) return 'Impossible de contacter le serveur. Vérifiez que le backend est démarré.';
    return err.error?.message || `Erreur (code ${err.status ?? 'inconnu'}).`;
  }
}
