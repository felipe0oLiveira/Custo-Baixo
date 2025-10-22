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
public class SmartMonitorResult {
    
    private String productName;
    private String originalUrl;
    private BigDecimal originalPrice;
    private BigDecimal targetPrice;
    private LocalDateTime createdAt;
    private int totalSitesSearched;
    private int sitesWithProduct;
    private String bestPriceSite;
    private BigDecimal bestPrice;
    private String bestPriceUrl;
    private BigDecimal savings;
    private BigDecimal savingsPercentage;
    private List<SitePrice> allPrices;
    private String status; // "MONITORING", "TARGET_REACHED", "ERROR"
    private String message;
}

