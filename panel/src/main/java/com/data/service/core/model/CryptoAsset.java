package com.data.service.core.model;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CryptoAsset {

    private Long id;

    private String symbol;

    private java.math.BigDecimal marketCap;

    private java.time.LocalDate listingDate;

}
