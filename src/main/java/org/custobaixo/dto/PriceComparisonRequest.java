package org.custobaixo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceComparisonRequest {
    
    @NotBlank(message = "URL do produto é obrigatória")
    private String productUrl;
    
    @NotBlank(message = "Nome do produto é obrigatório")
    private String productName;
}

