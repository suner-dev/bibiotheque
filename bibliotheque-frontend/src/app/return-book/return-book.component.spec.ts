import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ReturnBookComponent } from './return-book.component';
import { BooksService } from '../_service/books.service';
import { BorrowService } from '../_service/borrow.service';
import { UserAuthService } from '../_service/user-auth.service';
import { ToastService } from '../_service/toast.service';
import { of } from 'rxjs';

describe('ReturnBookComponent', () => {
  let component: ReturnBookComponent;
  let fixture: ComponentFixture<ReturnBookComponent>;
  let booksServiceSpy: jasmine.SpyObj<BooksService>;
  let borrowServiceSpy: jasmine.SpyObj<BorrowService>;
  let userAuthServiceSpy: jasmine.SpyObj<UserAuthService>;
  let toastServiceSpy: jasmine.SpyObj<ToastService>;

  beforeEach(async () => {
    booksServiceSpy = jasmine.createSpyObj('BooksService', ['getBooksList']);
    borrowServiceSpy = jasmine.createSpyObj('BorrowService', ['getBooksBorrowedByUser', 'returnBook']);
    userAuthServiceSpy = jasmine.createSpyObj('UserAuthService', ['getUserId']);
    toastServiceSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);

    await TestBed.configureTestingModule({
      declarations: [ReturnBookComponent],
      imports: [HttpClientTestingModule],
      providers: [
        { provide: BooksService, useValue: booksServiceSpy },
        { provide: BorrowService, useValue: borrowServiceSpy },
        { provide: UserAuthService, useValue: userAuthServiceSpy },
        { provide: ToastService, useValue: toastServiceSpy },
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    fixture = TestBed.createComponent(ReturnBookComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load books and user borrows on init', fakeAsync(() => {
    booksServiceSpy.getBooksList.and.returnValue(of([]));
    borrowServiceSpy.getBooksBorrowedByUser.and.returnValue(of([]));
    userAuthServiceSpy.getUserId.and.returnValue(1);
    component.ngOnInit();
    tick();
    expect(booksServiceSpy.getBooksList).toHaveBeenCalled();
    expect(borrowServiceSpy.getBooksBorrowedByUser).toHaveBeenCalledWith(1);
  }));

  it('should call returnBook service on returnBook method', () => {
    borrowServiceSpy.returnBook.and.returnValue(of({}));
    component.returnBook(1);
    expect(borrowServiceSpy.returnBook).toHaveBeenCalled();
  });
});
