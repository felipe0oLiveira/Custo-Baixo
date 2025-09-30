package org.custobaixo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.custobaixo.entity.ProductMonitor;
import org.custobaixo.entity.ProductCategory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulerService {

    private final ProductMonitorService productService;

    // SCHEDULER PRINCIPAL

    @Scheduled(cron = "0 */5 * * * *") // A cada 5 minutos
    public void checkAllActiveProducts() {
        log.info("Iniciando verificação automática de preços...");

        executeWithErrorHandling("Verificação automática", () -> {
            List<ProductMonitor> activeProducts = productService.getAllActiveProducts();

            log.info("Verificando {} produtos ativos", activeProducts.size());

            List<ProductMonitor> productsToCheck = activeProducts.stream()
                    .filter(shouldCheckProduct())
                    .collect(Collectors.toList());

            VerificationResult result = verifyProducts(productsToCheck);
            logVerificationResult(result);

            return result;
        });
    }

    @Scheduled(cron = "0 */2 * * * *") // A cada 2 minutos
    public void checkCriticalProducts() {
        log.info("⚡ Verificação rápida de produtos críticos...");

        executeWithErrorHandling("Verificação crítica", () -> {
            List<ProductMonitor> criticalProducts = productService.getAllActiveProducts().stream()
                    .filter(isCriticalProduct())
                    .collect(Collectors.toList());

            log.info("🎯 Verificando {} produtos críticos", criticalProducts.size());

            return verifyProducts(criticalProducts);
        });
    }

    @Scheduled(cron = "0 0 * * * *") // A cada hora
    public void cleanupInactiveProducts() {
        log.info("🧹 Iniciando limpeza de produtos inativos...");

        executeWithErrorHandling("Limpeza", () -> {
            int cleanedCount = productService.cleanupInactiveProducts();
            log.info("✅ Limpeza concluída - {} produtos removidos", cleanedCount);
            return cleanedCount;
        });
    }

    // MÉTODOS AUXILIARES SEM ANINHAMENTO

    private VerificationResult verifyProducts(List<ProductMonitor> products) {
        return products.stream()
                .map(this::verifyProduct)
                .reduce(VerificationResult.empty(), VerificationResult::merge);
    }

    private VerificationResult verifyProduct(ProductMonitor product) {
        try {
            BigDecimal oldPrice = product.getCurrentPrice();
            ProductMonitor updatedProduct = productService.checkProductPrice(product.getId());

            return VerificationResult.builder()
                    .successCount(1)
                    .targetReachedCount(countTargetReached(updatedProduct))
                    .priceChangedCount(countPriceChanged(oldPrice, updatedProduct))
                    .build();

        } catch (Exception e) {
            log.error("Erro ao verificar produto ID {}: {}", product.getId(), e.getMessage());
            return VerificationResult.builder().errorCount(1).build();
        }
    }

    // PREDICATES FUNCIONAIS

    private Predicate<ProductMonitor> shouldCheckProduct() {
        return product -> !isRecentlyChecked(product) && !isAlreadyNotified(product);
    }

    private Predicate<ProductMonitor> isCriticalProduct() {
        return product -> isFlightProduct(product) || isElectronicsNearTarget(product);
    }

    //  VERIFICAÇÕES SIMPLES

    private boolean isRecentlyChecked(ProductMonitor product) {
        return product.getLastChecked() != null &&
                product.getLastChecked().isAfter(LocalDateTime.now().minusMinutes(3));
    }

    private boolean isAlreadyNotified(ProductMonitor product) {
        return Boolean.TRUE.equals(product.getNotificationSent());
    }

    private boolean isFlightProduct(ProductMonitor product) {
        return product.getCategory() == ProductCategory.PASSAGENS_AEREAS;
    }

    private boolean isElectronicsNearTarget(ProductMonitor product) {
        return product.getCategory() == ProductCategory.ELETRONICOS && isPriceNearTarget(product);
    }

    private boolean isPriceNearTarget(ProductMonitor product) {
        return hasValidPrices(product) && isWithinTargetRange(product);
    }

    private boolean hasValidPrices(ProductMonitor product) {
        return product.getCurrentPrice() != null && product.getTargetPrice() != null;
    }

    private boolean isWithinTargetRange(ProductMonitor product) {
        return calculatePriceDifference(product).abs().compareTo(new BigDecimal("0.20")) <= 0;
    }

    // CÁLCULOS SIMPLES

    private BigDecimal calculatePriceDifference(ProductMonitor product) {
        return product.getCurrentPrice().subtract(product.getTargetPrice())
                .divide(product.getTargetPrice(), 2, RoundingMode.HALF_UP);
    }

    private int countTargetReached(ProductMonitor product) {
        return isTargetPriceReached(product) ? 1 : 0;
    }

    private int countPriceChanged(BigDecimal oldPrice, ProductMonitor product) {
        return (oldPrice != null && !oldPrice.equals(product.getCurrentPrice())) ? 1 : 0;
    }

    private boolean isTargetPriceReached(ProductMonitor product) {
        return hasValidPrices(product) &&
                product.getCurrentPrice().compareTo(product.getTargetPrice()) <= 0;
    }

    // UTILITY METHODS

    private <T> void executeWithErrorHandling(String operation, java.util.function.Supplier<T> supplier) {
        try {
            log.info("Iniciando operação: {}", operation);
            supplier.get();
        } catch (Exception e) {
            log.error("Erro na operação '{}': {}", operation, e.getMessage(), e);
        }
    }

    private void logVerificationResult(VerificationResult result) {
        log.info(" Verificação concluída - Sucessos: {}, Erros: {}, Preços alvo atingidos: {}",
                result.getSuccessCount(), result.getErrorCount(), result.getTargetReachedCount());
    }

    //  INNER CLASSES =

    @lombok.Builder
    @lombok.Data
    private static class VerificationResult {
        private int successCount;
        private int errorCount;
        private int targetReachedCount;
        private int priceChangedCount;

        public static VerificationResult empty() {
            return VerificationResult.builder().build();
        }

        public VerificationResult merge(VerificationResult other) {
            return VerificationResult.builder()
                    .successCount(this.successCount + other.successCount)
                    .errorCount(this.errorCount + other.errorCount)
                    .targetReachedCount(this.targetReachedCount + other.targetReachedCount)
                    .priceChangedCount(this.priceChangedCount + other.priceChangedCount)
                    .build();
        }
    }
}