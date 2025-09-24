package org.custobaixo.repository;

import org.custobaixo.entity.ProductCategory;
import org.custobaixo.entity.ProductMonitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProductMonitorRepository extends JpaRepository<ProductMonitor, Long> {

    // Buscar produtos ativos
    List<ProductMonitor> findByIsActiveTrue();

    // Buscar produtos por categoria
    List<ProductMonitor> findByCategory(ProductCategory category);

    // Buscar produtos por ativos por categoria
    List<ProductMonitor> findByCategoryAndIsActiveTrue(ProductCategory category);

    // Buscar produtos que precisam ser verificados (não verificados há mais de X minutos)
    @Query("SELECT p FROM ProductMonitor p WHERE p.isActive = true AND (p.lastChecked IS NULL OR p.lastChecked < :cutoffTime)")
    List<ProductMonitor> findProductsToCheck(@Param("cutoffTime") LocalDateTime cutoffTime);

    // Buscar produtos com preço alvo atingido
    @Query("SELECT p FROM ProductMonitor p WHERE p.isActive = true AND p.currentPrice IS NOT NULL AND p.currentPrice <= p.targetPrice AND p.notificationSent = false")
    List<ProductMonitor> findProductsWithTargetPriceReached();

    // Buscar produtos por site
    List<ProductMonitor> findBySiteNameContainingIgnoreCase(String siteName);

    // Contar produtos ativos por categoria
    @Query("SELECT p.category, COUNT(p) FROM ProductMonitor p WHERE p.isActive = true GROUP BY p.category")
    List<Object[]> countActiveProductsByCategory();

    // Buscar produtos que não foram verificados há muito tempo
    @Query("SELECT p FROM ProductMonitor p WHERE p.isActive = true AND p.lastChecked < :threshold")
    List<ProductMonitor> findStaleProducts(@Param("threshold") LocalDateTime threshold);
}

