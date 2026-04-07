import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { HomeComponent } from './core/home/home.component';
import { WorkspaceComponent } from './core/workspace/workspace.component';
import { LoginComponent } from './features/auth/login/login.component';
import { AccessDeniedComponent } from './features/auth/access-denied/access-denied.component';
import { DataExplorerComponent } from './features/data-explorer/data-explorer.component';
import { AuthGuard } from './core/guards/auth.guard';
import { ModuleAccessGuard } from './core/guards/module-access.guard';

const routes: Routes = [
  { path: '', component: HomeComponent, canActivate: [AuthGuard] },
  { path: 'auth/login', component: LoginComponent },
  { path: 'access-denied', component: AccessDeniedComponent, canActivate: [AuthGuard] },
  { path: 'explorer', component: DataExplorerComponent, canActivate: [AuthGuard] },
  { path: 'workspace', component: WorkspaceComponent, canActivate: [AuthGuard] },
  { path: 'workspace/:moduleId', component: WorkspaceComponent, canActivate: [AuthGuard, ModuleAccessGuard] },
  { path: '**', redirectTo: '' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}
