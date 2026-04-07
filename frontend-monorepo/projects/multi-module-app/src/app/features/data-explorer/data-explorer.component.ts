import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { QueryBuilderComponent, QueryCondition } from '../../shared/components/query-builder/query-builder.component';
import { DynamicTableComponent } from '../../shared/components/dynamic-table/dynamic-table.component';
import { FilterField } from '../../core/config/data-query.config';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-data-explorer',
  templateUrl: './data-explorer.component.html',
  standalone: true,
  imports: [CommonModule, QueryBuilderComponent, DynamicTableComponent]
})
export class DataExplorerComponent implements OnInit {
  fields: FilterField[] = [
    { name: 'id', label: 'ID', type: 'number' },
    { name: 'name', label: 'Name', type: 'string' },
    { name: 'status', label: 'Status', type: 'string' },
    { name: 'createdAt', label: 'Created At', type: 'date' }
  ];

  tableColumns = [
    { field: 'id', headerName: 'ID', width: 90 },
    { field: 'name', headerName: 'Name', flex: 1 },
    { field: 'status', headerName: 'Status', width: 120 },
    { field: 'createdAt', headerName: 'Created At', width: 200 }
  ];

  tableData: any[] = [];
  isLoading = false;

  username: string = '';

  constructor(private authService: AuthService) {
    const user = this.authService.currentUserValue;
    if (user) {
      this.username = user.username;
    }
  }

  ngOnInit(): void {}

  onQuerySubmit(conditions: QueryCondition[]) {
    this.isLoading = true;

    // Simulate API Call for multi-datasource search
    setTimeout(() => {
      this.tableData = [
        { id: 1, name: 'Project Alpha', status: 'Active', createdAt: '2023-01-15' },
        { id: 2, name: 'System Beta', status: 'Pending', createdAt: '2023-03-22' },
        { id: 3, name: 'Module Gamma', status: 'Completed', createdAt: '2023-06-11' },
        { id: 4, name: 'Application Delta', status: 'Active', createdAt: '2023-08-05' }
      ];
      this.isLoading = false;
    }, 800);
  }

  logout() {
    this.authService.logout();
  }
}
