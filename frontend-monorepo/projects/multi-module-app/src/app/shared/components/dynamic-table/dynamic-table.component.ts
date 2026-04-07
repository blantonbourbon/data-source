import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges
} from '@angular/core';
import { AgGridAngular } from 'ag-grid-angular';
import { ColDef, GridReadyEvent } from 'ag-grid-community';

@Component({
  selector: 'app-dynamic-table',
  templateUrl: './dynamic-table.component.html',
  styleUrl: './dynamic-table.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AgGridAngular]
})
export class DynamicTableComponent implements OnChanges {
  private static readonly minColumnWidth = 140;
  private static readonly autoSizeColumnLimit = 8;
  private static readonly estimatedHeaderCharacterWidth = 10;
  private static readonly estimatedHeaderPadding = 48;
  private static readonly maxEstimatedColumnWidth = 320;

  @Input() rowData: any[] = [];
  @Input() columnDefs: ColDef[] = [];
  @Input() isLoading = false;

  @Output() rowClicked = new EventEmitter<any>();

  resolvedColumnDefs: ColDef[] = [];

  defaultColDef: ColDef = {
    sortable: true,
    filter: true,
    resizable: true,
    minWidth: DynamicTableComponent.minColumnWidth,
    wrapHeaderText: true,
    autoHeaderHeight: true
  };

  ngOnChanges(changes: SimpleChanges) {
    if (changes['columnDefs']) {
      this.resolvedColumnDefs = this.withReadableHeaders(this.columnDefs);
    }
  }

  onGridReady(params: GridReadyEvent) {
    if (this.resolvedColumnDefs.length === 0) {
      return;
    }

    setTimeout(() => {
      if (this.resolvedColumnDefs.length <= DynamicTableComponent.autoSizeColumnLimit) {
        params.api.autoSizeAllColumns(false);
        return;
      }

      params.api.refreshHeader();
    });
  }

  onRowClicked(event: any) {
    this.rowClicked.emit(event.data);
  }

  private withReadableHeaders(columnDefs: ColDef[]): ColDef[] {
    return columnDefs.map(columnDef => {
      const minWidth = this.getPreferredMinWidth(columnDef);
      const preferredWidth = this.getPreferredWidth(columnDef, minWidth);

      return {
        ...columnDef,
        ...(preferredWidth != null ? { width: preferredWidth } : {}),
        minWidth,
        wrapHeaderText: columnDef.wrapHeaderText ?? true,
        autoHeaderHeight: columnDef.autoHeaderHeight ?? true,
        headerTooltip: columnDef.headerTooltip ?? columnDef.headerName ?? columnDef.field
      };
    });
  }

  private getPreferredMinWidth(columnDef: ColDef): number {
    if (columnDef.minWidth != null) {
      return columnDef.minWidth;
    }

    if (columnDef.maxWidth != null) {
      return Math.min(DynamicTableComponent.minColumnWidth, columnDef.maxWidth);
    }

    return DynamicTableComponent.minColumnWidth;
  }

  private getPreferredWidth(columnDef: ColDef, minWidth: number): number | undefined {
    if (columnDef.flex != null || columnDef.width != null || columnDef.initialWidth != null) {
      return undefined;
    }

    const headerText = columnDef.headerName ?? columnDef.field;
    if (!headerText) {
      return minWidth;
    }

    const estimatedWidth =
      headerText.length * DynamicTableComponent.estimatedHeaderCharacterWidth +
      DynamicTableComponent.estimatedHeaderPadding;
    const maxWidth =
      columnDef.maxWidth != null
        ? Math.min(columnDef.maxWidth, DynamicTableComponent.maxEstimatedColumnWidth)
        : DynamicTableComponent.maxEstimatedColumnWidth;

    return Math.min(Math.max(estimatedWidth, minWidth), maxWidth);
  }
}
