import { NO_ERRORS_SCHEMA } from '@angular/core';
import { convertToParamMap } from '@angular/router';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { RouterTestingModule } from '@angular/router/testing';

import { WorkspaceComponent } from './workspace.component';
import { AuthorizationService } from '../services/authorization.service';
import { ConfigService } from '../services/config.service';
import { TabService } from './tab.service';

describe('WorkspaceComponent', () => {
  let component: WorkspaceComponent;
  let fixture: ComponentFixture<WorkspaceComponent>;

  const routeStub = {
    paramMap: of(convertToParamMap({})),
    queryParamMap: of(convertToParamMap({})),
    snapshot: {
      paramMap: convertToParamMap({}),
      queryParamMap: convertToParamMap({})
    }
  };
  const configServiceStub = {
    loadExternalApps: () => of([]),
    getModuleById: () => undefined,
    getInternalModuleById: () => undefined,
    getExternalAppById: () => undefined
  };
  const authorizationServiceStub = {
    filterAuthorizedModuleIds: (moduleIds: string[]) => moduleIds
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RouterTestingModule],
      declarations: [WorkspaceComponent],
      providers: [
        TabService,
        { provide: ActivatedRoute, useValue: routeStub },
        { provide: ConfigService, useValue: configServiceStub },
        { provide: AuthorizationService, useValue: authorizationServiceStub }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    fixture = TestBed.createComponent(WorkspaceComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
