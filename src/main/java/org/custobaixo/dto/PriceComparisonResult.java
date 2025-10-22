package org.custobaixo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.custobaixo.model.SitePrice;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceComparisonResult {
    
    private String productName;
    private String originalUrl;
    private BigDecimal originalPrice;
    private LocalDateTime searchDate;
    private int totalSitesSearched;
    private int sitesWithProduct;
    private String bestPriceSite;
    private BigDecimal bestPrice;
    private String bestPriceUrl;
    private BigDecimal savings;
    private BigDecimal savingsPercentage;
    private List<SitePrice> allPrices;
    private String status; // "SUCCESS", "ERROR", "PARTIAL"
    private String message;
}

