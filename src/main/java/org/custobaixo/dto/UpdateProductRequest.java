package org.custobaixo.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class UpdateProductRequest {

    private String productName;

    @DecimalMin(value = "0.01", message = "Preço alvo deve ser maior que zero")
        private BigDecimal targetPrice;

    private String productUrl;
}
