package org.custobaixo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmartProductRequest {
    
    @NotBlank(message = "URL do produto é obrigatória")
    private String productUrl;
    
    @NotNull(message = "Preço alvo é obrigatório")
    @DecimalMin(value = "0.01", message = "Preço alvo deve ser maior que zero")
    private BigDecimal targetPrice;
    
    @NotBlank(message = "Nome do produto é obrigatório")
    private String productName;
}

