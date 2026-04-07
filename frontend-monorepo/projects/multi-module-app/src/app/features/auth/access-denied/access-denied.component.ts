import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { map } from 'rxjs';

@Component({
  selector: 'app-access-denied',
  imports: [CommonModule, RouterLink],
  template: `
    <section class="denied-page" aria-labelledby="denied-title">
      <div class="denied-card">
        <p class="denied-code">403</p>
        <h1 id="denied-title">Access denied</h1>
        <p class="denied-copy">
          Your identity is authenticated, but your current permissions do not allow access to
          <strong>{{ moduleName$ | async }}</strong
          >.
        </p>

        <div class="denied-actions">
          <a routerLink="/" class="primary-link">Return to dashboard</a>
          <a routerLink="/workspace" class="secondary-link">Open workspace</a>
        </div>
      </div>
    </section>
  `,
  styles: [
    `
      .denied-page {
        min-height: 100%;
        display: grid;
        place-items: center;
        padding: 2rem;
        background: linear-gradient(180deg, #fff6ee 0%, #fffdf9 100%);
      }

      .denied-card {
        width: min(34rem, 100%);
        border-radius: 1rem;
        border: 1px solid #f0d6bc;
        background: #fff;
        padding: 2.25rem;
        box-shadow: 0 18px 50px rgba(103, 58, 13, 0.12);
      }

      .denied-code {
        margin: 0 0 0.5rem;
        font-size: 0.875rem;
        letter-spacing: 0.24em;
        text-transform: uppercase;
        color: #aa6b2f;
      }

      h1 {
        margin: 0 0 0.75rem;
        color: #6e3d14;
      }

      .denied-copy {
        margin: 0;
        color: #65462b;
        line-height: 1.6;
      }

      .denied-actions {
        display: flex;
        gap: 0.75rem;
        margin-top: 1.5rem;
        flex-wrap: wrap;
      }

      .primary-link,
      .secondary-link {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        min-height: 2.75rem;
        padding: 0 1rem;
        border-radius: 999px;
        text-decoration: none;
        font-weight: 600;
      }

      .primary-link {
        background: #6e3d14;
        color: white;
      }

      .secondary-link {
        border: 1px solid #d6b391;
        color: #6e3d14;
      }
    `
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AccessDeniedComponent {
  protected readonly moduleName$;

  constructor(private readonly route: ActivatedRoute) {
    this.moduleName$ = this.route.queryParamMap.pipe(map(params => params.get('module') || 'the requested module'));
  }
}
