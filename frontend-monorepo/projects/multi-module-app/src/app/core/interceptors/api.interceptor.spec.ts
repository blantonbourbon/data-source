import { HttpErrorResponse, HttpHandler, HttpRequest, HttpResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AuthService } from '../services/auth.service';
import { ApiInterceptor } from './api.interceptor';

describe('ApiInterceptor', () => {
  let interceptor: ApiInterceptor;
  let authServiceStub: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(() => {
    authServiceStub = jasmine.createSpyObj<AuthService>('AuthService', ['login']);
    routerSpy = jasmine.createSpyObj<Router>('Router', ['navigate'], { url: '/workspace' });

    TestBed.configureTestingModule({
      providers: [
        ApiInterceptor,
        { provide: AuthService, useValue: authServiceStub },
        { provide: Router, useValue: routerSpy }
      ]
    });

    interceptor = TestBed.inject(ApiInterceptor);
  });

  it('does not add an Authorization header for api requests', () => {
    const request = new HttpRequest('GET', '/api/me');
    let forwardedRequest: HttpRequest<unknown> | undefined;
    const next: HttpHandler = {
      handle: req => {
        forwardedRequest = req;
        return of(new HttpResponse({ status: 200 }));
      }
    };

    interceptor.intercept(request, next).subscribe();

    expect(forwardedRequest?.headers.has('Authorization')).toBeFalse();
  });

  it('redirects 403 responses to the access denied page', () => {
    const request = new HttpRequest('GET', '/api/me');
    const next: HttpHandler = {
      handle: () =>
        throwError(
          () =>
            new HttpErrorResponse({
              status: 403,
              statusText: 'Forbidden'
            })
        )
    };

    interceptor.intercept(request, next).subscribe({
      error: () => undefined
    });

    expect(routerSpy.navigate).toHaveBeenCalledWith(['/access-denied']);
  });

  it('restarts login when an api request returns 401', () => {
    const request = new HttpRequest('GET', '/api/me');
    const next: HttpHandler = {
      handle: () =>
        throwError(
          () =>
            new HttpErrorResponse({
              status: 401,
              statusText: 'Unauthorized'
            })
        )
    };

    interceptor.intercept(request, next).subscribe({
      error: () => undefined
    });

    expect(authServiceStub.login).toHaveBeenCalledWith('/workspace');
  });
});
