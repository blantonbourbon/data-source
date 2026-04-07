import { ChangeDetectionStrategy, Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, ParamMap, Router } from '@angular/router';
import { Subscription, catchError, combineLatest, map, of, switchMap } from 'rxjs';

import { DataQueryComponent } from '../../features/data-query/data-query.component';
import { ExternalIframeComponent } from '../../features/external-iframe/external-iframe.component';
import { AuthorizationService } from '../services/authorization.service';
import { ConfigService } from '../services/config.service';
import { TabItem, TabService } from './tab.service';

@Component({
  standalone: false,
  selector: 'app-workspace',
  templateUrl: './workspace.component.html',
  styleUrl: './workspace.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class WorkspaceComponent implements OnInit, OnDestroy {
  private routeSub?: Subscription;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    readonly tabService: TabService,
    private readonly configService: ConfigService,
    private readonly authorizationService: AuthorizationService
  ) {}

  ngOnInit(): void {
    this.routeSub = combineLatest([this.route.paramMap, this.route.queryParamMap])
      .pipe(switchMap(([params, queryParams]) => this.restoreWorkspaceFromRoute(params, queryParams)))
      .subscribe();
  }

  ngOnDestroy(): void {
    this.routeSub?.unsubscribe();
  }

  closeTab(index: number, event: MouseEvent): void {
    event.stopPropagation();
    this.tabService.removeTab(index);
    this.syncRouteWithCurrentState();
  }

  onTabChange(index: number): void {
    this.tabService.setActiveTab(index);
    this.syncRouteWithCurrentState();
  }

  private restoreWorkspaceFromRoute(params: ParamMap, queryParams: ParamMap) {
    const routeTabIds = this.parseTabIds(queryParams.get('tabs'));
    const routeActiveTabId = queryParams.get('active');
    const legacyModuleId = params.get('moduleId');
    const currentTabIds = this.tabService.getTabIds();

    if (routeTabIds.length > 0) {
      return this.restoreTabs(routeTabIds, routeActiveTabId);
    }

    if (legacyModuleId) {
      const nextTabIds = currentTabIds.includes(legacyModuleId) ? currentTabIds : [...currentTabIds, legacyModuleId];
      return this.restoreTabs(nextTabIds, legacyModuleId);
    }

    if (currentTabIds.length > 0) {
      this.syncRouteWithCurrentState();
      return of(void 0);
    }

    this.tabService.clearTabs();
    return of(void 0);
  }

  private restoreTabs(tabIds: string[], activeTabId: string | null) {
    const uniqueTabIds = [...new Set(tabIds)];

    return this.ensureExternalAppsLoaded(uniqueTabIds).pipe(
      map(() => {
        const authorizedTabIds = this.authorizationService.filterAuthorizedModuleIds(uniqueTabIds, moduleId =>
          this.configService.getModuleById(moduleId)
        );
        const deniedTabIds = uniqueTabIds.filter(tabId => !authorizedTabIds.includes(tabId));

        if (authorizedTabIds.length === 0 && deniedTabIds.length > 0) {
          this.tabService.clearTabs();
          void this.router.navigate(['/access-denied'], {
            queryParams: {
              module: activeTabId && deniedTabIds.includes(activeTabId) ? activeTabId : deniedTabIds[0]
            }
          });
          return;
        }

        const tabs = authorizedTabIds
          .map(moduleId => this.buildTab(moduleId))
          .filter((tab): tab is TabItem => tab !== undefined);
        const resolvedActiveTabId = tabs.some(tab => tab.id === activeTabId) ? activeTabId : (tabs[0]?.id ?? null);

        this.tabService.replaceTabs(tabs, resolvedActiveTabId);
        this.syncRouteWithCurrentState();
      })
    );
  }

  private ensureExternalAppsLoaded(tabIds: string[]) {
    if (!tabIds.some(tabId => tabId.startsWith('ext-'))) {
      return of(void 0);
    }

    return this.configService.loadExternalApps().pipe(
      map(() => void 0),
      catchError(error => {
        console.error('Failed to load external apps config', error);
        return of(void 0);
      })
    );
  }

  private buildTab(moduleId: string): TabItem | undefined {
    const internalConfig = this.configService.getInternalModuleById(moduleId);
    if (internalConfig) {
      return {
        id: moduleId,
        title: internalConfig.name,
        componentType: DataQueryComponent,
        data: { config: internalConfig }
      };
    }

    if (moduleId.startsWith('ext-')) {
      const appConfig = this.configService.getExternalAppById(moduleId);
      return {
        id: moduleId,
        title: appConfig?.name ?? 'External App',
        componentType: ExternalIframeComponent,
        data: { url: appConfig?.url ?? '' }
      };
    }

    console.warn('Unknown module:', moduleId);
    return undefined;
  }

  private syncRouteWithCurrentState(): void {
    const tabIds = this.tabService.getTabIds();
    const activeTabId = this.tabService.getActiveTabId();
    const currentQueryTabIds = this.parseTabIds(this.route.snapshot.queryParamMap.get('tabs'));
    const currentActiveTabId = this.route.snapshot.queryParamMap.get('active');
    const currentLegacyModuleId = this.route.snapshot.paramMap.get('moduleId');
    const hasMatchingTabs =
      currentQueryTabIds.length === tabIds.length &&
      currentQueryTabIds.every((tabId, index) => tabId === tabIds[index]);
    const hasMatchingActiveTab = currentActiveTabId === activeTabId;

    if (!currentLegacyModuleId && hasMatchingTabs && hasMatchingActiveTab) {
      return;
    }

    void this.router.navigate(['/workspace'], {
      queryParams: tabIds.length > 0 ? { tabs: tabIds.join(','), active: activeTabId } : {},
      replaceUrl: true
    });
  }

  private parseTabIds(tabsParam: string | null): string[] {
    if (!tabsParam) {
      return [];
    }

    return tabsParam
      .split(',')
      .map(tabId => tabId.trim())
      .filter(tabId => tabId.length > 0);
  }
}
