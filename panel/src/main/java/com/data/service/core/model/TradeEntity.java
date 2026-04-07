package com.data.service.core.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "trade")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tradeType;

    private java.time.LocalDate tradeDate;

    private Double amount;

    private String currency;

    private String counterparty;

}
