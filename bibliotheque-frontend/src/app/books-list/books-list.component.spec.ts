import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { BooksListComponent } from './books-list.component';
import { BooksService } from '../_service/books.service';
import { of } from 'rxjs';

describe('BooksListComponent', () => {
  let component: BooksListComponent;
  let fixture: ComponentFixture<BooksListComponent>;
  let booksServiceSpy: jasmine.SpyObj<BooksService>;

  beforeEach(async () => {
    booksServiceSpy = jasmine.createSpyObj('BooksService', ['getBooksList']);
    await TestBed.configureTestingModule({
      declarations: [BooksListComponent],
      imports: [HttpClientTestingModule, RouterTestingModule],
      providers: [
        { provide: BooksService, useValue: booksServiceSpy },
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    fixture = TestBed.createComponent(BooksListComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load books on init', fakeAsync(() => {
    booksServiceSpy.getBooksList.and.returnValue(of([]));
    component.ngOnInit();
    tick();
    expect(booksServiceSpy.getBooksList).toHaveBeenCalled();
  }));
});
