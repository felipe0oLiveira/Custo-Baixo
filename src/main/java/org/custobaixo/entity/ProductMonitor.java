package org.custobaixo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_monitor")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class ProductMonitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "URL do produto é obrigatória")
    @Column(name = "product_url", length = 500, nullable = false)
    private String productUrl;

    @NotNull(message = "Preço alvo é obrigatório")
    @DecimalMin(value = "0.01", message = "Preço alvo deve ser maior que zero")
    @Column(name = "target_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal targetPrice;

    @DecimalMin(value = "0.01", message = "Preço atual deve ser maior que zero")
    @Column(name = "current_price", precision = 10, scale = 2)
    private BigDecimal currentPrice;

    @Column
    private String productName;

    @Column(name = "site_name", length = 100)
    private String siteName;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_category")
    private ProductCategory category;

    @Column(name = "travel_date")
    private LocalDate travelDate;

    @Column(name = "origin_city", length = 100)
    private String originCity;

    @Column(name = "destination_city", length = 100)
    private String destinationCity;

    @Column(name = "passengers_count")
    private Integer passengersCount;

    @Column(name = "is_active")
    private Boolean isActive;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_checked")
    private LocalDateTime lastChecked;

    @Column(name = "notification_sent")
    private Boolean notificationSent;
}