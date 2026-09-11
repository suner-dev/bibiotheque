import { HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Router } from '@angular/router';
import { catchError } from 'rxjs/operators';
import { Observable, throwError } from 'rxjs';
import { UserAuthService } from '../_service/user-auth.service';
import { Injectable } from '@angular/core';

/**
 * Intercepteur HTTP pour l'authentification JWT.
 *
 * - Ajoute automatiquement le token JWT dans les requêtes
 * - Gère les erreurs 401 (session expirée) et 403 (accès refusé)
 * - Nettoie la session en cas de 401
 */
@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(
    private userAuthService: UserAuthService,
    private router: Router
  ) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    if (req.headers.get('No-Auth') === 'True') {
      return next.handle(req.clone());
    }

    const token = this.userAuthService.getToken();
    req = this.addToken(req, token);

    return next.handle(req).pipe(
      catchError(
        (err: HttpErrorResponse) => {
          console.log('HTTP Error:', err.status, err.message);

          if (err.status === 401) {
            // RS-01 : Session expirée ou token invalide
            // Nettoyer la session et rediriger vers login
            this.userAuthService.clear();
            this.router.navigate(['/login']);
          } else if (err.status === 403) {
            // RS-02 / RS-03 : Accès refusé (rôle insuffisant ou pas propriétaire)
            this.router.navigate(['/forbidden']);
          }
          return throwError(() => err);
        }
      )
    );
  }

  private addToken(request: HttpRequest<any>, token: string) {
    return request.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }
}