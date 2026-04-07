import { Injectable, inject } from '@angular/core';
import { HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { AuthService } from '../services/auth.service';

@Injectable()
export class ApiInterceptor implements HttpInterceptor {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const isApiRequest = request.url.startsWith('/api') || request.url.startsWith('http');

    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        if (isApiRequest && error.status === 401) {
          void this.authService.login(this.router.url);
        } else if (isApiRequest && error.status === 403) {
          void this.router.navigate(['/access-denied']);
        }

        return throwError(() => error);
      })
    );
  }
}
