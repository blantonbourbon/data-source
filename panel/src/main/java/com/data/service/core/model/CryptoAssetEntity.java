package com.data.service.core.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "crypto_assets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CryptoAssetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol;

    private java.math.BigDecimal marketCap;

    private java.time.LocalDate listingDate;

}
