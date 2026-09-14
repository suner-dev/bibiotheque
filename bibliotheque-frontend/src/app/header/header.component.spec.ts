import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Component } from '@angular/core';
import { RouterTestingModule } from '@angular/router/testing';
import { HeaderComponent } from './header.component';
import { UserAuthService } from '../_service/user-auth.service';
import { UsersService } from '../_service/users.service';
import { ThemeService } from '../_service/theme.service';

@Component({ selector: 'app-dummy', template: '' })
class DummyComponent {}

const routes = [
  { path: '', component: DummyComponent },
  { path: 'books', component: DummyComponent },
  { path: 'create-book', component: DummyComponent },
  { path: 'users', component: DummyComponent },
  { path: 'register-user', component: DummyComponent },
  { path: 'borrow-book', component: DummyComponent },
  { path: 'return-book', component: DummyComponent },
  { path: 'reservations', component: DummyComponent },
  { path: 'login', component: DummyComponent },
];

describe('HeaderComponent', () => {
  let component: HeaderComponent;
  let fixture: ComponentFixture<HeaderComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ HeaderComponent, DummyComponent ],
      imports: [RouterTestingModule.withRoutes(routes)],
      providers: [
        { provide: UserAuthService, useValue: jasmine.createSpyObj('UserAuthService', ['getName', 'isLoggedIn', 'clear']) },
        { provide: UsersService, useValue: jasmine.createSpyObj('UsersService', ['roleMatch']) },
        { provide: ThemeService, useValue: { isDark: false, toggle: jasmine.createSpy() } }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(HeaderComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});