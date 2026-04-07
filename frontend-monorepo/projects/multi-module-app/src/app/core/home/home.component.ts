import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

import { ModuleAuthorization, ModuleDefinition } from '../models/auth.models';
import { AuthorizationService } from '../services/authorization.service';
import { ConfigService, ExternalAppConfig } from '../services/config.service';
import { TabService } from '../workspace/tab.service';

interface ModuleCard extends ModuleDefinition {
  authorization?: ModuleAuthorization;
}

@Component({
  standalone: false,
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HomeComponent implements OnInit {
  currentTab: 'all' | 'favorites' = 'all';
  favoriteIds: string[] = [];
  isLoading = true;
  modules: ModuleCard[] = [];
  searchTerm = '';

  constructor(
    private readonly router: Router,
    private readonly configService: ConfigService,
    private readonly cdr: ChangeDetectorRef,
    private readonly tabService: TabService,
    private readonly authorizationService: AuthorizationService
  ) {}

  ngOnInit(): void {
    this.modules = this.configService
      .getInternalModules()
      .filter(moduleDefinition => this.authorizationService.hasModuleAccess(moduleDefinition));

    const cachedApps = this.configService.getExternalApps();
    if (cachedApps.length > 0) {
      this.appendExternalApps(cachedApps);
      this.isLoading = false;
      this.cdr.markForCheck();
      return;
    }

    this.configService.loadExternalApps().subscribe({
      next: apps => {
        this.appendExternalApps(apps);
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: error => {
        console.error('Failed to load external apps config', error);
        this.initializeFavorites();
        this.isLoading = false;
        this.cdr.markForCheck();
      }
    });
  }

  get displayedModules(): ModuleCard[] {
    let filtered = [...this.modules];

    if (this.currentTab === 'favorites') {
      filtered = filtered.filter(moduleDefinition => this.favoriteIds.includes(moduleDefinition.id));
    }

    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(moduleDefinition => moduleDefinition.name.toLowerCase().includes(term));
    }

    return filtered;
  }

  appendExternalApps(apps: ExternalAppConfig[]): void {
    const externalModules = apps
      .filter(app => this.authorizationService.hasModuleAccess(app))
      .map(app => ({
        id: `ext-${app.id}`,
        name: app.name,
        description: app.description,
        bgStyle: app.bgStyle || 'linear-gradient(135deg, #4b1b9c 0%, #ca08d1 100%)',
        logo: app.logo || 'EA',
        authorization: app.authorization
      }));

    this.modules = [...this.modules, ...externalModules];
    this.initializeFavorites();
  }

  initializeFavorites(): void {
    const savedFavorites = localStorage.getItem('favoriteModules');

    if (!savedFavorites) {
      this.favoriteIds = this.modules.map(moduleDefinition => moduleDefinition.id);
      this.saveFavorites();
      return;
    }

    this.favoriteIds = JSON.parse(savedFavorites) as string[];
    this.favoriteIds = this.favoriteIds.filter(moduleId =>
      this.modules.some(moduleDefinition => moduleDefinition.id === moduleId)
    );
  }

  saveFavorites(): void {
    localStorage.setItem('favoriteModules', JSON.stringify(this.favoriteIds));
  }

  toggleFavorite(moduleId: string): void {
    if (this.favoriteIds.includes(moduleId)) {
      this.favoriteIds = this.favoriteIds.filter(id => id !== moduleId);
    } else {
      this.favoriteIds = [...this.favoriteIds, moduleId];
    }

    this.saveFavorites();
  }

  removeFavorite(moduleId: string): void {
    this.favoriteIds = this.favoriteIds.filter(id => id !== moduleId);
    this.saveFavorites();
  }

  onSearch(event: Event): void {
    this.searchTerm = (event.target as HTMLInputElement).value;
  }

  openWorkspace(moduleId: string): void {
    const currentTabIds = this.tabService.getTabIds();
    const nextTabIds = currentTabIds.includes(moduleId) ? currentTabIds : [...currentTabIds, moduleId];

    void this.router.navigate(['/workspace'], {
      queryParams: {
        tabs: nextTabIds.join(','),
        active: moduleId
      }
    });
  }
}
