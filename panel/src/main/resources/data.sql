INSERT INTO trade (trade_type, trade_date, amount, currency, counterparty) VALUES
  ('SPOT', DATE '2026-01-06', 125000.00, 'USD', 'Bank A'),
  ('SPOT', DATE '2026-01-08', 84500.50, 'EUR', 'Bank C'),
  ('FORWARD', DATE '2026-01-10', 25000.00, 'EUR', 'Bank B'),
  ('FORWARD', DATE '2026-01-14', 91200.00, 'JPY', 'Bank D'),
  ('SWAP', DATE '2026-01-18', 140500.75, 'USD', 'Broker X'),
  ('OPTION', DATE '2026-01-21', 43000.00, 'GBP', 'Fund Y'),
  ('NDF', DATE '2026-02-02', 67000.25, 'BRL', 'Bank E'),
  ('SPOT', DATE '2026-02-05', 15800.00, 'SGD', 'Bank A');

INSERT INTO crypto_assets (symbol, market_cap, listing_date) VALUES
  ('BTC', 910000000000.00, DATE '2009-01-03'),
  ('ETH', 320000000000.00, DATE '2015-07-30'),
  ('SOL', 68000000000.00, DATE '2020-03-16'),
  ('XRP', 34000000000.00, DATE '2012-06-02'),
  ('ADA', 26000000000.00, DATE '2017-09-29'),
  ('AVAX', 19000000000.00, DATE '2020-09-21');
