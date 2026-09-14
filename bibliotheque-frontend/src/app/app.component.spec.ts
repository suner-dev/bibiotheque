import { TestBed } from '@angular/core/testing';
import { Component } from '@angular/core';
import { RouterTestingModule } from '@angular/router/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { AppComponent } from './app.component';
import { UserAuthService } from './_service/user-auth.service';
import { UsersService } from './_service/users.service';
import { ThemeService } from './_service/theme.service';
import { AuthGuard } from './_auth/auth.guard';

@Component({ selector: 'app-header', template: '<span>Bibliothèque LMS</span>' })
class MockHeaderComponent {}

@Component({ selector: 'app-toast', template: '' })
class MockToastComponent {}

describe('AppComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        RouterTestingModule.withRoutes([]),
        BrowserAnimationsModule,
        HttpClientTestingModule,
      ],
      declarations: [
        AppComponent,
        MockToastComponent,
        MockHeaderComponent,
      ],
      providers: [
        { provide: UserAuthService, useValue: jasmine.createSpyObj('UserAuthService', ['getName', 'isLoggedIn', 'clear']) },
        { provide: UsersService, useValue: jasmine.createSpyObj('UsersService', ['roleMatch']) },
        { provide: ThemeService, useValue: { isDark: false, toggle: jasmine.createSpy() } },
        { provide: AuthGuard, useValue: { canActivate: () => true } },
      ],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it(`should have as title 'bibliotheque-frontend'`, () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app.title).toEqual('Library Management System');
  });

  it('should render title', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('app-header')?.textContent).toContain('Bibliothèque');
  });
});