import { Injectable, Type } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface TabItem {
  id: string;
  title: string;
  componentType?: Type<unknown>;
  componentName?: string;
  data?: Record<string, unknown>;
}

@Injectable({
  providedIn: 'root'
})
export class TabService {
  private readonly tabsSource = new BehaviorSubject<TabItem[]>([]);
  public readonly tabs$ = this.tabsSource.asObservable();

  private readonly activeTabIndexSource = new BehaviorSubject<number>(0);
  public readonly activeTabIndex$ = this.activeTabIndexSource.asObservable();

  get tabs(): TabItem[] {
    return this.tabsSource.value;
  }

  addTab(tab: TabItem): void {
    const currentTabs = this.tabs;
    const existingIndex = currentTabs.findIndex(currentTab => currentTab.id === tab.id);

    if (existingIndex !== -1) {
      this.activeTabIndexSource.next(existingIndex);
      return;
    }

    const newTabs = [...currentTabs, tab];
    this.tabsSource.next(newTabs);
    this.activeTabIndexSource.next(newTabs.length - 1);
  }

  replaceTabs(tabs: TabItem[], activeTabId?: string | null): void {
    this.tabsSource.next(tabs);

    if (tabs.length === 0) {
      this.activeTabIndexSource.next(0);
      return;
    }

    const activeIndex = activeTabId ? tabs.findIndex(tab => tab.id === activeTabId) : 0;
    this.activeTabIndexSource.next(activeIndex >= 0 ? activeIndex : 0);
  }

  removeTab(index: number): void {
    const newTabs = [...this.tabs];
    newTabs.splice(index, 1);
    this.tabsSource.next(newTabs);

    const activeIndex = this.activeTabIndexSource.value;
    if (activeIndex > index) {
      this.activeTabIndexSource.next(activeIndex - 1);
      return;
    }

    if (activeIndex >= newTabs.length) {
      this.activeTabIndexSource.next(Math.max(0, newTabs.length - 1));
    }
  }

  setActiveTab(index: number): void {
    if (this.tabs.length === 0) {
      this.activeTabIndexSource.next(0);
      return;
    }

    const boundedIndex = Math.max(0, Math.min(index, this.tabs.length - 1));
    this.activeTabIndexSource.next(boundedIndex);
  }

  clearTabs(): void {
    this.tabsSource.next([]);
    this.activeTabIndexSource.next(0);
  }

  getTabIds(): string[] {
    return this.tabs.map(tab => tab.id);
  }

  getActiveTabId(): string | null {
    return this.tabs[this.activeTabIndexSource.value]?.id ?? null;
  }
}
