import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { Component, NO_ERRORS_SCHEMA } from '@angular/core';
import { CreateBookComponent } from './create-book.component';
import { BooksService } from '../_service/books.service';
import { ToastService } from '../_service/toast.service';
import { of } from 'rxjs';

@Component({ selector: 'stub-books-list', template: '' })
class BooksListStubComponent {}

describe('CreateBookComponent', () => {
  let component: CreateBookComponent;
  let fixture: ComponentFixture<CreateBookComponent>;
  let booksServiceSpy: jasmine.SpyObj<BooksService>;
  let toastServiceSpy: jasmine.SpyObj<ToastService>;

  beforeEach(async () => {
    booksServiceSpy = jasmine.createSpyObj('BooksService', ['createBook']);
    toastServiceSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);
    await TestBed.configureTestingModule({
      declarations: [CreateBookComponent],
      imports: [HttpClientTestingModule, RouterTestingModule.withRoutes([{ path: 'books', component: BooksListStubComponent }])],
      providers: [
        { provide: BooksService, useValue: booksServiceSpy },
        { provide: ToastService, useValue: toastServiceSpy },
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    fixture = TestBed.createComponent(CreateBookComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should call saveBook on onSubmit', () => {
    const saveBookSpy = spyOn(component as any, 'saveBook').and.callThrough();
    booksServiceSpy.createBook.and.returnValue(of({}));
    component.onSubmit();
    expect(saveBookSpy).toHaveBeenCalled();
  });
});
