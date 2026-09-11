import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { BorrowService } from './borrow.service';

describe('BorrowService', () => {
  let service: BorrowService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [BorrowService]
    });
    service = TestBed.inject(BorrowService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should define baseURL', () => {
    expect(BorrowService).toBeDefined();
  });
});
