package org.custobaixo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimpleProductRequest {
    
    @NotBlank(message = "Nome do produto é obrigatório")
    private String productName;
}


