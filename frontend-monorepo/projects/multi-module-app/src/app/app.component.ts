import { ChangeDetectionStrategy, Component } from '@angular/core';

import { AuthService } from './core/services/auth.service';

@Component({
  standalone: false,
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppComponent {
  readonly title = 'multi-module-app';
  readonly currentUser$: AuthService['currentUser$'];

  constructor(private readonly authService: AuthService) {
    this.currentUser$ = this.authService.currentUser$;
  }

  signOut(): void {
    this.authService.logout();
  }
}
