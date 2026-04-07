import { TabItem, TabService } from './tab.service';

describe('TabService', () => {
  let service: TabService;

  const firstTab: TabItem = {
    id: 'xms',
    title: 'XMS Module'
  };

  const secondTab: TabItem = {
    id: 'libra',
    title: 'Libra Module'
  };

  beforeEach(() => {
    service = new TabService();
  });

  it('adds a new tab and activates it', () => {
    service.addTab(firstTab);

    expect(service.tabs).toEqual([firstTab]);
    expect(service.getActiveTabId()).toBe('xms');
  });

  it('does not duplicate an existing tab and focuses it instead', () => {
    service.addTab(firstTab);
    service.addTab(secondTab);
    service.setActiveTab(0);

    service.addTab(secondTab);

    expect(service.tabs).toEqual([firstTab, secondTab]);
    expect(service.getActiveTabId()).toBe('libra');
  });

  it('replaces tabs and activates the requested tab id when present', () => {
    service.replaceTabs([firstTab, secondTab], 'libra');

    expect(service.tabs).toEqual([firstTab, secondTab]);
    expect(service.getActiveTabId()).toBe('libra');
  });

  it('falls back to the first tab when replaceTabs receives an unknown active id', () => {
    service.replaceTabs([firstTab, secondTab], 'missing');

    expect(service.getActiveTabId()).toBe('xms');
  });

  it('removes a tab and keeps the active tab index in bounds', () => {
    service.replaceTabs([firstTab, secondTab], 'libra');

    service.removeTab(1);

    expect(service.tabs).toEqual([firstTab]);
    expect(service.getActiveTabId()).toBe('xms');
  });

  it('clears all tabs and resets the active tab index', () => {
    service.replaceTabs([firstTab, secondTab], 'xms');

    service.clearTabs();

    expect(service.tabs).toEqual([]);
    expect(service.getActiveTabId()).toBeNull();
  });

  it('bounds the requested active index to the available tab range', () => {
    service.replaceTabs([firstTab, secondTab], 'xms');

    service.setActiveTab(99);
    expect(service.getActiveTabId()).toBe('libra');

    service.setActiveTab(-1);
    expect(service.getActiveTabId()).toBe('xms');
  });
});
