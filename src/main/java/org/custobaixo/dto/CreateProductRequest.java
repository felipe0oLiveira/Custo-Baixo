package org.custobaixo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class CreateProductRequest {
    @NotBlank(message = "URL do produto é obrigatória")
    private String productUrl;

    @NotNull(message = "Preço alvo é obrigatório")
    @DecimalMin(value = "0.01", message = "Preço alvo deve ser maior que zero")
    private BigDecimal targetPrice;

    private String productName;

    // Campos opcionais para passagens aéreas
    private LocalDate travelDate;
    private String originCity;
    private String destinationCity;
    private Integer passengersCount;
}
