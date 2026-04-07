package com.data.service.core;

import com.data.service.core.model.TradeEntity;
import com.data.service.core.repository.TradeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TradeIntegrationTest {

    @Autowired
    TradeRepository tradeRepository;

    @BeforeEach
    void setUp() {
        tradeRepository.deleteAll();
        tradeRepository.save(TradeEntity.builder()
                .tradeType("SPOT")
                .amount(1000.0)
                .currency("USD")
                .tradeDate(LocalDate.now())
                .counterparty("Bank A")
                .build());
        tradeRepository.save(TradeEntity.builder()
                .tradeType("FORWARD")
                .amount(25000.0)
                .currency("EUR")
                .tradeDate(LocalDate.now())
                .counterparty("Bank B")
                .build());
    }

    @Test
    void testTradesAreLoaded() {
        List<TradeEntity> trades = tradeRepository.findAll();
        assertThat(trades).hasSize(2);

        TradeEntity trade = trades.stream().filter(t -> t.getTradeType().equals("SPOT")).findFirst().orElseThrow();
        assertThat(trade.getCurrency()).isEqualTo("USD");
        assertThat(trade.getAmount()).isEqualTo(1000.0);
    }
}
