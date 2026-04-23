import { OverlayContainer } from '@angular/cdk/overlay';
import { FormBuilder } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ComponentFixture, TestBed, fakeAsync, flush } from '@angular/core/testing';

import { QueryBuilderComponent } from './query-builder.component';

describe('QueryBuilderComponent', () => {
  let component: QueryBuilderComponent;

  beforeEach(() => {
    component = new QueryBuilderComponent(new FormBuilder());
    component.availableFields = [
      {
        name: 'tradeType',
        label: 'Trade Type',
        type: 'dropdown',
        dropdownOptions: [
          { label: 'Spot', value: 'Spot' },
          { label: 'Forward', value: 'Forward' }
        ]
      },
      { name: 'counterparty', label: 'Counterparty', type: 'string' },
      { name: 'tradeDate', label: 'Trade Date', type: 'date' }
    ];
    component.initForm();
  });

  it('keeps quick search separate from advanced filters', () => {
    spyOn(component.querySubmit, 'emit');
    component.queryForm.get('simpleSearch')?.setValue('USD');
    component.advancedForm.patchValue({ tradeType: ['Spot'] });

    component.onSearchClick();

    expect(component.querySubmit.emit).toHaveBeenCalledWith([{ field: '_keyword_', operator: 'LIKE', value: 'USD' }]);
  });

  it('counts active advanced filters by field', () => {
    component.advancedForm.patchValue({
      tradeType: ['Spot'],
      tradeDate_start: new Date('2024-03-01'),
      tradeDate_end: new Date('2024-03-31')
    });

    expect(component.activeAdvancedFilterCount).toBe(2);
  });

  it('clears only advanced values from the floating panel', () => {
    component.queryForm.get('simpleSearch')?.setValue('counterparty');
    component.advancedForm.patchValue({ tradeType: ['Spot'] });

    component.clearAdvancedFilters();

    expect(component.queryForm.get('simpleSearch')?.value).toBe('counterparty');
    expect(component.activeAdvancedFilterCount).toBe(0);
  });

  it('toggles the floating panel explicitly', () => {
    expect(component.isAdvancedOpen).toBeFalse();

    component.toggleAdvanced();
    expect(component.isAdvancedOpen).toBeTrue();

    component.toggleAdvanced();
    expect(component.isAdvancedOpen).toBeFalse();
  });

  it('applies advanced filters and closes the floating panel', () => {
    spyOn(component.querySubmit, 'emit');
    component.isAdvancedOpen = true;
    component.advancedForm.patchValue({
      tradeType: ['Spot'],
      tradeDate_start: new Date('2024-03-01')
    });

    component.applyAdvancedFilters();

    expect(component.querySubmit.emit).toHaveBeenCalledWith([
      { field: 'tradeType', operator: '=', value: 'Spot' },
      { field: 'tradeDate', operator: '>=', value: '2024-03-01' }
    ]);
    expect(component.isAdvancedOpen).toBeFalse();
  });

  it('treats checked checkbox fields as equals filters', () => {
    component.availableFields = [{ name: 'isActive', label: 'Active Only', type: 'checkbox' }];
    component.initForm();
    spyOn(component.querySubmit, 'emit');
    component.advancedForm.patchValue({ isActive: true });

    component.applyAdvancedFilters();

    expect(component.querySubmit.emit).toHaveBeenCalledWith([{ field: 'isActive', operator: '=', value: true }]);
  });

  it('maps checkbox options to string query conditions', () => {
    component.availableFields = [
      {
        name: 'msgdirection',
        label: 'Message Direction',
        type: 'checkbox',
        checkboxOptions: [
          { label: 'IN', value: 'in' },
          { label: 'OUT', value: 'out' }
        ]
      }
    ];
    component.initForm();
    const emitSpy = spyOn(component.querySubmit, 'emit');

    component.onCheckboxOptionToggle(component.availableFields[0], 'in', true);
    component.applyAdvancedFilters();
    expect(emitSpy).toHaveBeenCalledWith([{ field: 'msgdirection', operator: '=', value: 'in' }]);

    emitSpy.calls.reset();
    component.clearAdvancedFilters();
    component.onCheckboxOptionToggle(component.availableFields[0], 'in', true);
    component.onCheckboxOptionToggle(component.availableFields[0], 'out', true);
    component.applyAdvancedFilters();
    expect(emitSpy).toHaveBeenCalledWith([{ field: 'msgdirection', operator: 'IN', value: 'in,out' }]);
  });

  it('maps dropdown multi-select values to query conditions', () => {
    component.availableFields = [
      {
        name: 'status',
        label: 'Status',
        type: 'dropdown',
        dropdownOptions: [
          { label: 'Open', value: 'open' },
          { label: 'Closed', value: 'closed' }
        ]
      }
    ];
    component.initForm();
    const emitSpy = spyOn(component.querySubmit, 'emit');

    component.advancedForm.patchValue({ status: ['open'] });
    component.applyAdvancedFilters();
    expect(emitSpy).toHaveBeenCalledWith([{ field: 'status', operator: '=', value: 'open' }]);

    emitSpy.calls.reset();
    component.clearAdvancedFilters();
    component.advancedForm.patchValue({ status: ['open', 'closed'] });
    component.applyAdvancedFilters();
    expect(emitSpy).toHaveBeenCalledWith([{ field: 'status', operator: 'IN', value: 'open,closed' }]);
  });
});

describe('QueryBuilderComponent DOM', () => {
  let fixture: ComponentFixture<QueryBuilderComponent>;
  let overlayContainer: OverlayContainer;
  let overlayContainerElement: HTMLElement;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [QueryBuilderComponent, NoopAnimationsModule]
    });

    overlayContainer = TestBed.inject(OverlayContainer);
    overlayContainerElement = overlayContainer.getContainerElement();

    fixture = TestBed.createComponent(QueryBuilderComponent);
    fixture.componentInstance.availableFields = [
      {
        name: 'status',
        label: 'Status',
        type: 'dropdown',
        dropdownOptions: [
          { label: 'Open', value: 'open' },
          { label: 'Closed', value: 'closed' }
        ]
      }
    ];
    fixture.componentInstance.isAdvancedOpen = true;
    fixture.detectChanges();
  });

  afterEach(() => {
    overlayContainer.ngOnDestroy();
  });

  it('renders dropdown options without nested material checkboxes', fakeAsync(() => {
    const hostElement = fixture.nativeElement as HTMLElement;
    const trigger = hostElement.querySelector('.mat-mdc-select-trigger') as HTMLElement | null;

    expect(trigger).not.toBeNull();

    trigger?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    fixture.detectChanges();
    flush();
    fixture.detectChanges();

    expect(overlayContainerElement.querySelectorAll('mat-option').length).toBe(2);
    expect(overlayContainerElement.querySelectorAll('mat-checkbox').length).toBe(0);
    expect(overlayContainerElement.querySelectorAll('.mat-pseudo-checkbox').length).toBe(2);
  }));
});
