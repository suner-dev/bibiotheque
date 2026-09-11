import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { UpdateBookComponent } from './update-book.component';
import { BooksService } from '../_service/books.service';
import { ToastService } from '../_service/toast.service';
import { of } from 'rxjs';

describe('UpdateBookComponent', () => {
  let component: UpdateBookComponent;
  let fixture: ComponentFixture<UpdateBookComponent>;
  let booksServiceSpy: jasmine.SpyObj<BooksService>;
  let toastServiceSpy: jasmine.SpyObj<ToastService>;

  beforeEach(async () => {
    booksServiceSpy = jasmine.createSpyObj('BooksService', ['updateBook', 'getBookById']);
    toastServiceSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);
    await TestBed.configureTestingModule({
      declarations: [UpdateBookComponent],
      imports: [HttpClientTestingModule, RouterTestingModule],
      providers: [
        { provide: BooksService, useValue: booksServiceSpy },
        { provide: ToastService, useValue: toastServiceSpy },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => '1' } } } },
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
