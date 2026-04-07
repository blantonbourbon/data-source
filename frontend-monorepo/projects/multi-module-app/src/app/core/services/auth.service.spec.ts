import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AuthConfig, BackendUserContext } from '../models/auth.models';
import { AuthConfigService } from './auth-config.service';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpTestingController: HttpTestingController | undefined;
  let authConfig: AuthConfig;
  let redirectBrowserSpy: jasmine.Spy;

  const routerSpy = jasmine.createSpyObj<Router>('Router', ['navigate', 'navigateByUrl']);
  const authConfigServiceStub = jasmine.createSpyObj<AuthConfigService>('AuthConfigService', [
    'loadConfig',
    'getConfig'
  ]);

  beforeEach(() => {
    authConfig = {
      mePath: '/api/me',
      loginPath: '/api/auth/login',
      logoutPath: '/api/auth/logout',
      defaultReturnUrl: '/'
    };

    authConfigServiceStub.loadConfig.and.callFake(() => of(authConfig));
    authConfigServiceStub.getConfig.and.callFake(() => authConfig);

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: routerSpy },
        { provide: AuthConfigService, useValue: authConfigServiceStub }
      ]
    });

    service = TestBed.inject(AuthService);
    httpTestingController = TestBed.inject(HttpTestingController);
    redirectBrowserSpy = spyOn<any>(service, 'redirectBrowser');
  });

  afterEach(() => {
    httpTestingController?.verify();
  });

  it('loads the current user from /api/me during initialize', async () => {
    const initializePromise = service.initialize();
    await flushAsyncWork();

    const userRequest = httpTestingController!.expectOne('/api/me');
    expect(userRequest.request.method).toBe('GET');
    userRequest.flush(createUserContext());

    await expectAsync(initializePromise).toBeResolved();
    expect(service.currentUserValue).toEqual(
      jasmine.objectContaining({
        id: 'backend-user',
        displayName: 'Alex Backend',
        permissions: ['module:xms:read', 'module:xms:export']
      })
    );
    expect('groups' in (service.currentUserValue as object)).toBeFalse();
    expect('entitlements' in (service.currentUserValue as object)).toBeFalse();
    expect('roles' in (service.currentUserValue as object)).toBeFalse();
  });

  it('clears auth state when /api/me returns 401 during initialize', async () => {
    const initializePromise = service.initialize();
    await flushAsyncWork();

    const userRequest = httpTestingController!.expectOne('/api/me');
    userRequest.flush('Unauthorized', {
      status: 401,
      statusText: 'Unauthorized'
    });

    await expectAsync(initializePromise).toBeResolved();
    expect(service.currentUserValue).toBeNull();
    expect(service.isAuthenticated()).toBeFalse();
  });

  it('redirects login to the backend auth entrypoint', async () => {
    await service.login('/workspace');

    expect(redirectBrowserSpy).toHaveBeenCalledWith('/api/auth/login?returnUrl=%2Fworkspace');
  });

  it('redirects logout to the backend auth logout entrypoint', async () => {
    const initializePromise = service.initialize();
    await flushAsyncWork();

    httpTestingController!.expectOne('/api/me').flush(createUserContext());
    await expectAsync(initializePromise).toBeResolved();
    service.logout();

    expect(service.currentUserValue).toBeNull();
    expect(redirectBrowserSpy).toHaveBeenCalledWith('/api/auth/logout');
  });

  function createUserContext(): BackendUserContext {
    return {
      id: 'backend-user',
      username: 'alex',
      displayName: 'Alex Backend',
      email: 'alex@example.com',
      permissions: ['module:xms:read', 'module:xms:export'],
      dataScopes: {
        xms: ['book-a', 'book-b']
      }
    };
  }

  async function flushAsyncWork(): Promise<void> {
    await Promise.resolve();
    await Promise.resolve();
  }
});
