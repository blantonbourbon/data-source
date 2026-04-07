import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { RouterTestingModule } from '@angular/router/testing';

import { HomeComponent } from './home.component';
import { AuthorizationService } from '../services/authorization.service';
import { ConfigService } from '../services/config.service';
import { TabService } from '../workspace/tab.service';

describe('HomeComponent', () => {
  let component: HomeComponent;
  let fixture: ComponentFixture<HomeComponent>;

  const configServiceStub = {
    getInternalModules: () => [],
    getExternalApps: () => [],
    loadExternalApps: () => of([])
  };
  const authorizationServiceStub = {
    hasModuleAccess: () => true
  };
  const tabServiceStub = {
    getTabIds: () => []
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RouterTestingModule],
      declarations: [HomeComponent],
      providers: [
        { provide: ConfigService, useValue: configServiceStub },
        { provide: AuthorizationService, useValue: authorizationServiceStub },
        { provide: TabService, useValue: tabServiceStub }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    fixture = TestBed.createComponent(HomeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
