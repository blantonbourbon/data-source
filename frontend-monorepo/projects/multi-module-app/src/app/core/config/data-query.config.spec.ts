import { BuiltinModules } from './data-query.config';

describe('BuiltinModules', () => {
  it('uses human audience routes for xms', () => {
    expect(BuiltinModules['xms'].apiEndpoint).toBe('/api/user/trades');
    expect(BuiltinModules['xms'].metricEndpoint).toBe('/api/user/trades/metric');
  });

  it('uses human audience routes for libra', () => {
    expect(BuiltinModules['libra'].apiEndpoint).toBe('/api/user/cryptoassets');
    expect(BuiltinModules['libra'].metricEndpoint).toBe('/api/user/cryptoassets/metric');
  });
});
