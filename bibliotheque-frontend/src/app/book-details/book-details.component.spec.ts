import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { BookDetailsComponent } from './book-details.component';
import { BooksService } from '../_service/books.service';
import { BorrowService } from '../_service/borrow.service';
import { UsersService } from '../_service/users.service';
import { ToastService } from '../_service/toast.service';

describe('BookDetailsComponent', () => {
  let component: BookDetailsComponent;
  let fixture: ComponentFixture<BookDetailsComponent>;

  beforeEach(async () => {
    const booksServiceSpy = jasmine.createSpyObj('BooksService', ['getBookById']);
    const borrowServiceSpy = jasmine.createSpyObj('BorrowService', ['getBookBorrowHistory']);
    booksServiceSpy.getBookById.and.returnValue(of({}));
    borrowServiceSpy.getBookBorrowHistory.and.returnValue(of([]));
    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      declarations: [BookDetailsComponent],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { params: { bookId: '1' } } } },
        { provide: BooksService, useValue: booksServiceSpy },
        { provide: BorrowService, useValue: borrowServiceSpy },
        { provide: UsersService, useValue: jasmine.createSpyObj('UsersService', ['roleMatch']) },
        { provide: ToastService, useValue: jasmine.createSpyObj('ToastService', ['success', 'error']) },
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .overrideComponent(BookDetailsComponent, { set: { template: '<div></div>' } })
    .compileComponents();

    fixture = TestBed.createComponent(BookDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});