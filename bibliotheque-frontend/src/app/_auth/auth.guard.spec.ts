import { TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { AuthGuard } from './auth.guard';
import { UserAuthService } from '../_service/user-auth.service';
import { UsersService } from '../_service/users.service';

describe('AuthGuard', () => {
  let guard: AuthGuard;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [RouterTestingModule],
      providers: [
        { provide: UserAuthService, useValue: jasmine.createSpyObj('UserAuthService', ['getToken']) },
        { provide: UsersService, useValue: jasmine.createSpyObj('UsersService', ['roleMatch']) }
      ]
    });
    guard = TestBed.inject(AuthGuard);
  });

  it('should be created', () => {
    expect(guard).toBeTruthy();
  });
});
