import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DataQueryComponent } from './data-query.component';
import { DataQueryModule } from './data-query.module';

describe('DataQueryComponent', () => {
  let component: DataQueryComponent;
  let fixture: ComponentFixture<DataQueryComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DataQueryModule]
    }).compileComponents();

    fixture = TestBed.createComponent(DataQueryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
