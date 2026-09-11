import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Reservation, ReservationRequest } from '../_model/reservation';

@Injectable({
  providedIn: 'root'
})
export class ReservationService {

  private baseURL = `${environment.apiUrl}/api/reservations`;

  constructor(private httpClient: HttpClient) { }

  getReservations(statut?: string, adherentId?: number): Observable<Reservation[]> {
    let params = new HttpParams();
    if (statut) {
      params = params.set('statut', statut);
    }
    if (adherentId) {
        params = params.set('adherentId', adherentId.toString());
    }
    return this.httpClient.get<Reservation[]>(`${this.baseURL}`, { params });
  }

  getReservationById(id: number): Observable<Reservation> {
    return this.httpClient.get<Reservation>(`${this.baseURL}/${id}`);
  }

  createReservation(request: ReservationRequest): Observable<Reservation> {
    return this.httpClient.post<Reservation>(`${this.baseURL}`, request);
  }

  annulerReservation(id: number): Observable<Reservation> {
    return this.httpClient.patch<Reservation>(`${this.baseURL}/${id}/annuler`, {});
  }

  deleteReservation(id: number): Observable<void> {
    return this.httpClient.delete<void>(`${this.baseURL}/${id}`);
  }
}
