package com.data.service.core.model;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Trade {

    private Long id;

    private String tradeType;

    private java.time.LocalDate tradeDate;

    private Double amount;

    private String currency;

    private String counterparty;

}
