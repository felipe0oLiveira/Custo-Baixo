package org.custobaixo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.custobaixo.entity.ProductCategory;
import org.custobaixo.entity.ProductMonitor;
import org.custobaixo.repository.ProductMonitorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductMonitorService {

    private final ProductMonitorRepository repository;
    private final WebScrapingService webScrapingService;

    // Criar novo produto para monitoramento
    public ProductMonitor createProduct(ProductMonitor product) {
        log.info("Criando novo produto para monitoramento: {}", product.getProductName());

        // Definir nome do site automaticamente baseado na URL
        if (product.getSiteName() == null) {
            product.setSiteName(extractSiteNameFromUrl(product.getProductUrl()));
        }

        // Definir categoria automaticamente se não especificada
        if (product.getCategory() == null) {
            product.setCategory(detectCategoryFromUrl(product.getProductUrl()));
        }

        product.setIsActive(true);
        product.setNotificationSent(false);

        return repository.save(product);
    }

    // Buscar todos os produtos ativos
    @Transactional(readOnly = true)
    public List<ProductMonitor> getAllActiveProducts() {
        return repository.findByIsActiveTrue();
    }

    // Buscar todos os produtos (ativos e inativos)
    @Transactional(readOnly = true)
    public List<ProductMonitor> getAllProducts() {
        return repository.findAll();
    }

    // Buscar produtos por categoria
    @Transactional(readOnly = true)
    public List<ProductMonitor> getProductsByCategory(ProductCategory category) {
        return repository.findByCategoryAndIsActiveTrue(category);
    }

    // Buscar produto por ID
    @Transactional(readOnly = true)
    public Optional<ProductMonitor> getProductById(Long id) {
        return repository.findById(id);
    }

    // Atualizar produto
    public ProductMonitor updateProduct(ProductMonitor product) {
        log.info("Atualizando produto: {}", product.getId());
        return repository.save(product);
    }

    // Desativar produto
    public void deactivateProduct(Long id) {
        log.info("Desativando produto: {}", id);
        repository.findById(id).ifPresent(product -> {
            product.setIsActive(false);
            repository.save(product);
        });
    }

    // Verificar preço de um produto específico por ID
    public ProductMonitor checkProductPrice(Long id) {
        log.info("Verificando preço do produto ID: {}", id);

        Optional<ProductMonitor> productOpt = repository.findById(id);
        if (productOpt.isEmpty()) {
            log.warn("Produto ID {} não encontrado", id);
            return null;
        }

        ProductMonitor product = productOpt.get();

        try {
            // Extrair preço atual
            BigDecimal currentPrice = webScrapingService.extractPrice(product);

            if (currentPrice != null) {
                // Atualizar preço atual
                product.setCurrentPrice(currentPrice);
                product.setLastChecked(LocalDateTime.now());

                log.info("Produto {}: Preço atual R$ {}, Alvo R$ {}",
                        product.getId(), currentPrice, product.getTargetPrice());

                // Verificar se preço alvo foi atingido
                if (currentPrice.compareTo(product.getTargetPrice()) <= 0) {
                    log.info(" PREÇO ALVO ATINGIDO! Produto {}: R$ {} (Alvo: R$ {})",
                            product.getId(), currentPrice, product.getTargetPrice());

                    // Marcar para notificação
                    product.setNotificationSent(false); // Para enviar notificação
                }

                // Salvar atualizações
                return repository.save(product);
            } else {
                log.warn("Não foi possível extrair preço do produto: {}", product.getId());
                return product;
            }

        } catch (Exception e) {
            log.error("Erro ao verificar preço do produto: {}", product.getId(), e);
            return product;
        }
    }

    // Verificar preço de um produto específico (método original mantido)
    public void checkProductPrice(ProductMonitor product) {
        try {
            log.info("Verificando preço do produto: {} ({})", product.getId(), product.getProductName());

            // Extrair preço atual
            BigDecimal currentPrice = webScrapingService.extractPrice(product);

            if (currentPrice != null) {
                // Atualizar preço atual
                product.setCurrentPrice(currentPrice);
                product.setLastChecked(LocalDateTime.now());

                log.info(" Produto {}: Preço atual R$ {}, Alvo R$ {}",
                        product.getId(), currentPrice, product.getTargetPrice());

                // Verificar se preço alvo foi atingido
                if (currentPrice.compareTo(product.getTargetPrice()) <= 0) {
                    log.info("PREÇO ALVO ATINGIDO! Produto {}: R$ {} (Alvo: R$ {})",
                            product.getId(), currentPrice, product.getTargetPrice());

                    // Marcar para notificação
                    product.setNotificationSent(false); // Para enviar notificação

                    // Opcional: desativar monitoramento após atingir o alvo
                    // product.setIsActive(false);
                }

                // Salvar atualizações
                repository.save(product);
            } else {
                log.warn(" Não foi possível extrair preço do produto: {}", product.getId());
            }

        } catch (Exception e) {
            log.error("Erro ao verificar preço do produto: {}", product.getId(), e);
        }
    }

    // Verificar todos os produtos ativos
    public void checkAllActivePrices() {
        log.info("Iniciando verificação de preços de todos os produtos ativos");

        List<ProductMonitor> activeProducts = repository.findByIsActiveTrue();
        log.info("Encontrados {} produtos ativos para verificação", activeProducts.size());

        for (ProductMonitor product : activeProducts) {
            try {
                checkProductPrice(product);

                // Pequena pausa entre verificações para não sobrecarregar os sites
                Thread.sleep(1000); // 1 segundo

            } catch (Exception e) {
                log.error("Erro ao verificar produto: {}", product.getId(), e);
            }
        }

        log.info("Verificação de preços concluída");
    }

    // Buscar produtos que atingiram o preço alvo
    @Transactional(readOnly = true)
    public List<ProductMonitor> getProductsWithTargetPriceReached() {
        return repository.findProductsWithTargetPriceReached();
    }

    // Buscar produtos que precisam ser verificados
    @Transactional(readOnly = true)
    public List<ProductMonitor> getProductsToCheck() {
        // Buscar produtos não verificados há mais de 5 minutos
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(5);
        return repository.findProductsToCheck(cutoffTime);
    }

    // Estatísticas do sistema
    @Transactional(readOnly = true)
    public long getTotalActiveProducts() {
        return repository.findByIsActiveTrue().size();
    }

    @Transactional(readOnly = true)
    public long getProductsByCategoryCount(ProductCategory category) {
        return repository.findByCategoryAndIsActiveTrue(category).size();
    }

    // Remover produtos inativos há mais de 30 dias
    public int cleanupInactiveProducts() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        List<ProductMonitor> inactiveProducts = repository.findByIsActiveFalseAndUpdatedAtBefore(thirtyDaysAgo);

        if (inactiveProducts.isEmpty()) {
            log.info("Nenhum produto inativo para remover");
            return 0;
        }

        repository.deleteAll(inactiveProducts);
        log.info("Removidos {} produtos inativos", inactiveProducts.size());

        return inactiveProducts.size();
    }

    // Métodos auxiliares
    private String extractSiteNameFromUrl(String url) {
        try {
            String domain = url.replaceAll("https?://(www\\.)?", "").split("/")[0];
            return domain.replace(".com.br", "").replace(".com", "").toUpperCase();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    private ProductCategory detectCategoryFromUrl(String url) {
        String urlLower = url.toLowerCase();

        if (urlLower.contains("voeazul") || urlLower.contains("voegol") || urlLower.contains("latam")) {
            return ProductCategory.PASSAGENS_AEREAS;
        } else if (urlLower.contains("zara") || urlLower.contains("renner") || urlLower.contains("cea")) {
            return ProductCategory.ROUPAS;
        } else if (urlLower.contains("nike") || urlLower.contains("adidas") || urlLower.contains("olympikus")) {
            return ProductCategory.ESPORTES;
        } else if (urlLower.contains("saraiva") || urlLower.contains("cultura") || urlLower.contains("livro")) {
            return ProductCategory.LIVROS;
        } else if (urlLower.contains("booking") || urlLower.contains("hotel")) {
            return ProductCategory.HOTEIS;
        } else if (urlLower.contains("leroymerlin") || urlLower.contains("casasbahia")) {
            return ProductCategory.CASA_E_JARDIM;
        } else if (urlLower.contains("sephora") || urlLower.contains("oboticario")) {
            return ProductCategory.SAUDE_E_BELEZA;
        } else {
            return ProductCategory.ELETRONICOS; // Padrão
        }
    }
}