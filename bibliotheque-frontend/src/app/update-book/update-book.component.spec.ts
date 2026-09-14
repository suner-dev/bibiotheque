import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { Component, NO_ERRORS_SCHEMA } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { UpdateBookComponent } from './update-book.component';
import { BooksService } from '../_service/books.service';
import { ToastService } from '../_service/toast.service';

@Component({ selector: 'stub-books-list', template: '' })
class BooksListStubComponent {}

describe('UpdateBookComponent', () => {
  let component: UpdateBookComponent;
  let fixture: ComponentFixture<UpdateBookComponent>;
  let booksServiceSpy: jasmine.SpyObj<BooksService>;
  let toastServiceSpy: jasmine.SpyObj<ToastService>;

  beforeEach(async () => {
    booksServiceSpy = jasmine.createSpyObj('BooksService', ['updateBook', 'getBookById']);
    toastServiceSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);
    booksServiceSpy.getBookById.and.returnValue(of({ bookId: 1, bookName: 'Test', bookAuthor: 'Author', bookGenre: 'Fiction', noOfCopies: 1 }));
    await TestBed.configureTestingModule({
      declarations: [UpdateBookComponent],
      imports: [HttpClientTestingModule, RouterTestingModule.withRoutes([{ path: 'books', component: BooksListStubComponent }])],
      providers: [
        { provide: BooksService, useValue: booksServiceSpy },
        { provide: ToastService, useValue: toastServiceSpy },
        { provide: ActivatedRoute, useValue: { snapshot: { params: { bookId: '1' } } } },
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    fixture = TestBed.createComponent(UpdateBookComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should call updateBook on onSubmit', () => {
    booksServiceSpy.updateBook.and.returnValue(of({}));
    component.bookId = 1;
    component.onSubmit();
    expect(booksServiceSpy.updateBook).toHaveBeenCalled();
  });
});
