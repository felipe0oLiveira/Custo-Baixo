package org.custobaixo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SitePrice {
    private String siteName;
    private BigDecimal price;
    private String productUrl;
    private boolean available;
    private boolean isBestPrice;
    private int rank;
    private String productName;
    private Long productId;
}


