import { TestBed } from '@angular/core/testing';

import { ModuleAuthorization } from '../models/auth.models';
import { AuthService } from './auth.service';
import { AuthorizationService } from './authorization.service';

describe('AuthorizationService', () => {
  let service: AuthorizationService;

  const authServiceStub = {
    currentUserValue: {
      id: 'user-1',
      username: 'alex',
      displayName: 'Alex',
      permissions: ['module:xms:read', 'external:angular-docs:view'],
      dataScopes: {},
      claims: {}
    }
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [AuthorizationService, { provide: AuthService, useValue: authServiceStub }]
    });

    service = TestBed.inject(AuthorizationService);
  });

  it('rejects legacy group and entitlement rules when no permission codes are configured', () => {
    const authorization = {
      groups: ['xms-users'],
      entitlements: ['module:xms:read'],
      match: 'all'
    } as ModuleAuthorization;

    expect(service.hasAuthorization(authorization)).toBeFalse();
  });

  it('rejects modules when all requirements are not satisfied', () => {
    const authorization: ModuleAuthorization = { permissions: ['module:libra:write', 'module:xms:read'], match: 'all' };

    expect(service.hasAuthorization(authorization)).toBeFalse();
  });

  it('allows modules when any required permission code matches', () => {
    const authorization: ModuleAuthorization = {
      permissions: ['module:libra:write', 'module:xms:read'],
      match: 'any'
    };

    expect(service.hasAuthorization(authorization)).toBeTrue();
  });

  it('allows modules with matching permission codes', () => {
    const authorization: ModuleAuthorization = {
      permissions: ['module:xms:read'],
      match: 'all'
    };

    expect(service.hasAuthorization(authorization)).toBeTrue();
  });

  it('rejects modules when required permission codes are missing', () => {
    const authorization: ModuleAuthorization = {
      permissions: ['module:libra:write'],
      match: 'all'
    };

    expect(service.hasAuthorization(authorization)).toBeFalse();
  });
});
