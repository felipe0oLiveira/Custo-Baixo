package org.custobaixo.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class HealthCheckResponse {

    private String status;
    private LocalDateTime timestamp;
    private String message;
}
