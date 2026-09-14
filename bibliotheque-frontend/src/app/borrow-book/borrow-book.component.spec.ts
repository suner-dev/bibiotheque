import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { BorrowBookComponent } from './borrow-book.component';
import { BooksService } from '../_service/books.service';
import { BorrowService } from '../_service/borrow.service';
import { UserAuthService } from '../_service/user-auth.service';
import { ToastService } from '../_service/toast.service';
import { Books } from '../_model/books';
import { Borrow } from '../_model/borrow';

describe('BorrowBookComponent', () => {
  let component: BorrowBookComponent;
  let fixture: ComponentFixture<BorrowBookComponent>;
  let booksServiceSpy: jasmine.SpyObj<BooksService>;
  let borrowServiceSpy: jasmine.SpyObj<BorrowService>;
  let userAuthServiceSpy: jasmine.SpyObj<UserAuthService>;
  let toastServiceSpy: jasmine.SpyObj<ToastService>;

  beforeEach(async () => {
    booksServiceSpy = jasmine.createSpyObj('BooksService', ['getBooksList']);
    borrowServiceSpy = jasmine.createSpyObj('BorrowService', ['borrowBook']);
    userAuthServiceSpy = jasmine.createSpyObj('UserAuthService', ['getUserId']);
    toastServiceSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);

    booksServiceSpy.getBooksList.and.returnValue(of([]));
    userAuthServiceSpy.getUserId.and.returnValue(1);
    borrowServiceSpy.borrowBook.and.returnValue(of({ message: 'Success' }));

    await TestBed.configureTestingModule({
      declarations: [BorrowBookComponent],
      imports: [HttpClientTestingModule],
      providers: [
        { provide: BooksService, useValue: booksServiceSpy },
        { provide: BorrowService, useValue: borrowServiceSpy },
        { provide: UserAuthService, useValue: userAuthServiceSpy },
        { provide: ToastService, useValue: toastServiceSpy },
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    fixture = TestBed.createComponent(BorrowBookComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load books on init', fakeAsync(() => {
    const mockBooks: Books[] = [
      { bookId: 1, bookName: 'Test Book', bookAuthor: 'Author', bookGenre: 'Fiction', noOfCopies: 2 }
    ];
    booksServiceSpy.getBooksList.and.returnValue(of(mockBooks));
    userAuthServiceSpy.getUserId.and.returnValue(1);
    component.ngOnInit();
    tick();
    expect(booksServiceSpy.getBooksList).toHaveBeenCalled();
    expect(component.books.length).toBe(1);
  }));

  it('should call borrowBook service on borrowBook method', () => {
    userAuthServiceSpy.getUserId.and.returnValue(1);
    borrowServiceSpy.borrowBook.and.returnValue(of({ message: 'Success' }));
    booksServiceSpy.getBooksList.and.returnValue(of([]));
    component.borrowBook(1);
    expect(borrowServiceSpy.borrowBook).toHaveBeenCalled();
    expect(toastServiceSpy.success).toHaveBeenCalled();
  });

  it('should handle borrow error', () => {
    userAuthServiceSpy.getUserId.and.returnValue(1);
    borrowServiceSpy.borrowBook.and.returnValue(throwError(() => ({ status: 409, error: { message: 'Livre indisponible' } })));
    booksServiceSpy.getBooksList.and.returnValue(of([]));
    component.borrowBook(1);
    expect(toastServiceSpy.error).toHaveBeenCalled();
  });
});
