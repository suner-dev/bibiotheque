import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
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

  beforeEach(async () => {
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
        { provide: ReservationService, useValue: jasmine.createSpyObj('ReservationService', ['getReservations', 'getReservationById', 'annulerReservation', 'deleteReservation']) },
        { provide: BooksService, useValue: jasmine.createSpyObj('BooksService', ['getBooksList']) },
        { provide: UsersService, useValue: jasmine.createSpyObj('UsersService', ['getUsersList']) },
        { provide: ToastService, useValue: jasmine.createSpyObj('ToastService', ['success', 'error']) },
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
