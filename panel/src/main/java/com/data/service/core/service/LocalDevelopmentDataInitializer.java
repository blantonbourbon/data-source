package com.data.service.core.service;

import com.data.service.core.model.CryptoAssetEntity;
import com.data.service.core.model.TradeEntity;
import com.data.service.core.repository.CryptoAssetRepository;
import com.data.service.core.repository.TradeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "panel.security.local-dev", name = "auth-disabled", havingValue = "true")
public class LocalDevelopmentDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LocalDevelopmentDataInitializer.class);

    private final TradeRepository tradeRepository;
    private final CryptoAssetRepository cryptoAssetRepository;

    public LocalDevelopmentDataInitializer(TradeRepository tradeRepository,
                                           CryptoAssetRepository cryptoAssetRepository) {
        this.tradeRepository = tradeRepository;
        this.cryptoAssetRepository = cryptoAssetRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedTradesIfEmpty();
        seedCryptoAssetsIfEmpty();
    }

    private void seedTradesIfEmpty() {
        if (tradeRepository.count() > 0) {
            return;
        }

        tradeRepository.saveAll(List.of(
                trade("SPOT", LocalDate.of(2026, 1, 6), 125000.00, "USD", "Bank A"),
                trade("SPOT", LocalDate.of(2026, 1, 8), 84500.50, "EUR", "Bank C"),
                trade("FORWARD", LocalDate.of(2026, 1, 10), 25000.00, "EUR", "Bank B"),
                trade("FORWARD", LocalDate.of(2026, 1, 14), 91200.00, "JPY", "Bank D"),
                trade("SWAP", LocalDate.of(2026, 1, 18), 140500.75, "USD", "Broker X"),
                trade("OPTION", LocalDate.of(2026, 1, 21), 43000.00, "GBP", "Fund Y"),
                trade("NDF", LocalDate.of(2026, 2, 2), 67000.25, "BRL", "Bank E"),
                trade("SPOT", LocalDate.of(2026, 2, 5), 15800.00, "SGD", "Bank A")
        ));

        log.info("Seeded local development trades for frontend testing.");
    }

    private void seedCryptoAssetsIfEmpty() {
        if (cryptoAssetRepository.count() > 0) {
            return;
        }

        cryptoAssetRepository.saveAll(List.of(
                cryptoAsset("BTC", "910000000000.00", LocalDate.of(2009, 1, 3)),
                cryptoAsset("ETH", "320000000000.00", LocalDate.of(2015, 7, 30)),
                cryptoAsset("SOL", "68000000000.00", LocalDate.of(2020, 3, 16)),
                cryptoAsset("XRP", "34000000000.00", LocalDate.of(2012, 6, 2)),
                cryptoAsset("ADA", "26000000000.00", LocalDate.of(2017, 9, 29)),
                cryptoAsset("AVAX", "19000000000.00", LocalDate.of(2020, 9, 21))
        ));

        log.info("Seeded local development crypto assets for frontend testing.");
    }

    private TradeEntity trade(String tradeType, LocalDate tradeDate, double amount, String currency, String counterparty) {
        return TradeEntity.builder()
                .tradeType(tradeType)
                .tradeDate(tradeDate)
                .amount(amount)
                .currency(currency)
                .counterparty(counterparty)
                .build();
    }

    private CryptoAssetEntity cryptoAsset(String symbol, String marketCap, LocalDate listingDate) {
        return CryptoAssetEntity.builder()
                .symbol(symbol)
                .marketCap(new BigDecimal(marketCap))
                .listingDate(listingDate)
                .build();
    }
}
