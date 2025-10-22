package org.custobaixo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmartProductResult {
    
    private String productName;
    private String originalUrl;
    private BigDecimal originalPrice;
    private BigDecimal targetPrice;
    private LocalDateTime createdAt;
    private int totalSitesMonitored;
    private int sitesWithProduct;
    private String status; // "MONITORING", "TARGET_REACHED", "ERROR"
    private String message;
}

