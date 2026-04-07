import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ExternalIframeComponent } from './external-iframe.component';

describe('ExternalIframeComponent', () => {
  let component: ExternalIframeComponent;
  let fixture: ComponentFixture<ExternalIframeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ExternalIframeComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(ExternalIframeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
