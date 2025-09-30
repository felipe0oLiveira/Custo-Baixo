package org.custobaixo.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class PriceCheckResponse {
    private Long productId;
    private BigDecimal oldPrice;
    private BigDecimal newPrice;
    private BigDecimal targetPrice;
    private Boolean priceChanged;
    private Boolean targetReached;
    private LocalDateTime lastChecked;
}
