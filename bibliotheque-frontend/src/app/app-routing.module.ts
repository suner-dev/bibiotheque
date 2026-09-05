import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { BookDetailsComponent } from './book-details/book-details.component';
import { BooksListComponent } from './books-list/books-list.component';
import { BorrowBookComponent } from './borrow-book/borrow-book.component';
import { CreateBookComponent } from './create-book/create-book.component';
import { ForbiddenComponent } from './forbidden/forbidden.component';
import { HomeComponent } from './home/home.component';
import { LoginComponent } from './login/login.component';
import { RegistrationComponent } from './registration/registration.component';
import { ReturnBookComponent } from './return-book/return-book.component';
import { UpdateBookComponent } from './update-book/update-book.component';
import { UpdateUserComponent } from './update-user/update-user.component';
import { UserDetailsComponent } from './user-details/user-details.component';
import { UsersListComponent } from './users-list/users-list.component';
import { ReservationContainerComponent } from './reservation-container/reservation-container.component';
import { AuthGuard } from './_auth/auth.guard';const routes: Routes = [
  {path: '', component: HomeComponent, data: { animation: '' }},
  {path: 'login', component: LoginComponent, data: { animation: 'login' }},
  {path: 'books', component: BooksListComponent, canActivate:[AuthGuard], data:{roles:['Admin'], animation: 'books'}},
  {path: 'create-book', component: CreateBookComponent, canActivate:[AuthGuard], data:{roles:['Admin'], animation: 'create-book'}},
  {path: 'update-book/:bookId', component: UpdateBookComponent, canActivate:[AuthGuard], data:{roles:['Admin'], animation: 'update-book'}},
  {path: 'book-details/:bookId', component: BookDetailsComponent, canActivate:[AuthGuard], data:{roles:['Admin'], animation: 'book-details'}},
  {path: 'users', component: UsersListComponent, canActivate:[AuthGuard], data:{roles:['Admin'], animation: 'users'}},
  {path: 'register-user', component: RegistrationComponent, canActivate:[AuthGuard], data:{roles:['Admin'], animation: 'register-user'}},
  {path: 'user-details/:userId', component: UserDetailsComponent, canActivate:[AuthGuard], data:{roles:['Admin'], animation: 'user-details'}},
  {path: 'update-user/:userId', component: UpdateUserComponent, canActivate:[AuthGuard], data:{roles:['Admin'], animation: 'update-user'}},
  {path: 'forbidden', component: ForbiddenComponent, data: { animation: 'forbidden' }},
  {path: 'borrow-book', component: BorrowBookComponent, canActivate:[AuthGuard], data:{roles:['User'], animation: 'borrow-book'}},
  {path: 'return-book', component: ReturnBookComponent, canActivate:[AuthGuard], data:{roles:['User'], animation: 'return-book'}},
  {path: 'reservations', component: ReservationContainerComponent, canActivate:[AuthGuard], data:{roles:['Admin'], animation: 'reservations'}},
  {path: 'create-reservation', component: ReservationContainerComponent, canActivate:[AuthGuard], data:{roles:['Admin'], animation: 'create-reservation'}}
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
