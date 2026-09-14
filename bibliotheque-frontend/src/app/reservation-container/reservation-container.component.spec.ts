import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { of } from 'rxjs';
import { ReservationContainerComponent } from './reservation-container.component';
import { CreateReservationComponent } from '../create-reservation/create-reservation.component';
import { ReservationListComponent } from '../reservation-list/reservation-list.component';
import { ReservationService } from '../_service/reservation.service';
import { BooksService } from '../_service/books.service';
import { UsersService } from '../_service/users.service';
import { ToastService } from '../_service/toast.service';

describe('ReservationContainerComponent', () => {
  let component: ReservationContainerComponent;
  let fixture: ComponentFixture<ReservationContainerComponent>;
  let reservationServiceSpy: jasmine.SpyObj<ReservationService>;
  let booksServiceSpy: jasmine.SpyObj<BooksService>;
  let usersServiceSpy: jasmine.SpyObj<UsersService>;
  let toastServiceSpy: jasmine.SpyObj<ToastService>;

  beforeEach(async () => {
    reservationServiceSpy = jasmine.createSpyObj('ReservationService', ['getReservations', 'getReservationById', 'annulerReservation', 'deleteReservation']);
    booksServiceSpy = jasmine.createSpyObj('BooksService', ['getBooksList']);
    usersServiceSpy = jasmine.createSpyObj('UsersService', ['getUsersList']);
    toastServiceSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);

    reservationServiceSpy.getReservations.and.returnValue(of([]));
    booksServiceSpy.getBooksList.and.returnValue(of([]));
    usersServiceSpy.getUsersList.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      declarations: [
        ReservationContainerComponent,
        CreateReservationComponent,
        ReservationListComponent
      ],
      imports: [
        HttpClientTestingModule,
        RouterTestingModule
      ],
      providers: [
        { provide: ReservationService, useValue: reservationServiceSpy },
        { provide: BooksService, useValue: booksServiceSpy },
        { provide: UsersService, useValue: usersServiceSpy },
        { provide: ToastService, useValue: toastServiceSpy },
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    fixture = TestBed.createComponent(ReservationContainerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
