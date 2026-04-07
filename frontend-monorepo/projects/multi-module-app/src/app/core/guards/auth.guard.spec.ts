import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';

import { AuthGuard } from './auth.guard';
import { AuthService } from '../services/auth.service';

describe('AuthGuard', () => {
  let guard: AuthGuard;
  let authServiceStub: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(() => {
    authServiceStub = jasmine.createSpyObj<AuthService>('AuthService', ['isAuthenticated', 'login']);
    routerSpy = jasmine.createSpyObj<Router>('Router', ['createUrlTree']);

    TestBed.configureTestingModule({
      providers: [
        AuthGuard,
        { provide: AuthService, useValue: authServiceStub },
        { provide: Router, useValue: routerSpy }
      ]
    });

    guard = TestBed.inject(AuthGuard);
  });

  it('allows navigation for authenticated users', () => {
    authServiceStub.isAuthenticated.and.returnValue(true);

    const result = guard.canActivate({} as never, { url: '/workspace' } as never);

    expect(result).toBeTrue();
    expect(authServiceStub.login).not.toHaveBeenCalled();
  });

  it('restarts backend login for unauthenticated users', () => {
    authServiceStub.isAuthenticated.and.returnValue(false);

    const result = guard.canActivate({} as never, { url: '/workspace' } as never);

    expect(result).toBeFalse();
    expect(authServiceStub.login).toHaveBeenCalledWith('/workspace');
    expect(routerSpy.createUrlTree).not.toHaveBeenCalled();
  });
});
