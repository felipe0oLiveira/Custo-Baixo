package org.custobaixo.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.custobaixo.dto.*;
import org.custobaixo.entity.ProductCategory;
import org.custobaixo.entity.ProductMonitor;
import org.custobaixo.service.ProductMonitorService;
import org.custobaixo.service.SmartProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ProductMonitorController {

    private final ProductMonitorService productService;
    private final SmartProductService smartProductService;

    //  ENDPOINTS CRUD

    // MONITORAMENTO INTELIGENTE - BUSCAR EM TODOS OS SITES E MONITORAR AUTOMATICAMENTE
    @PostMapping("/smart-monitor")
    public ResponseEntity<SmartMonitorResult> createSmartMonitoring(@Valid @RequestBody SmartMonitorRequest request) {
        return executeWithLogging("Criando monitoramento inteligente", () -> {
            log.info("Iniciando monitoramento inteligente para: {}", request.getProductName());
            SmartMonitorResult result = smartProductService.createSmartMonitoring(request);
            log.info("Monitoramento inteligente criado - {} sites monitorados", result.getSitesWithProduct());
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        });
    }

    // COMPARAÇÃO DE PREÇOS - ENCONTRAR MELHOR PREÇO EM TODOS OS SITES (SEM SALVAMENTO)
    @PostMapping("/compare-prices")
    public ResponseEntity<PriceComparisonResult> comparePrices(@Valid @RequestBody PriceComparisonRequest request) {
        return executeWithLogging("Comparando preços", () -> {
            log.info("Iniciando comparação de preços para: {}", request.getProductName());
            PriceComparisonResult result = smartProductService.comparePrices(request);
            log.info("Comparação concluída - Melhor preço: {} - {}", result.getBestPriceSite(), result.getBestPrice());
            return ResponseEntity.ok(result);
        });
    }

    // COMPARAÇÃO DE PREÇOS SIMPLES - APENAS COM NOME DO PRODUTO
    @PostMapping("/compare-prices-simple")
    public ResponseEntity<PriceComparisonResult> comparePricesSimple(
            @RequestParam(required = false) String productName,
            @RequestBody(required = false) SimpleProductRequest bodyRequest) {
        return executeWithLogging("Comparando preços simples", () -> {
            // Aceitar nome do produto de query param OU body
            String finalProductName = productName != null ? productName : 
                    (bodyRequest != null ? bodyRequest.getProductName() : null);
            
            if (finalProductName == null || finalProductName.trim().isEmpty()) {
                throw new IllegalArgumentException("Nome do produto é obrigatório");
            }
            
            log.info("Iniciando comparação de preços simples para: {}", finalProductName);
            
            // Criar request com URL fake para o endpoint existente
            PriceComparisonRequest request = PriceComparisonRequest.builder()
                    .productName(finalProductName)
                    .productUrl("https://exemplo.com/" + finalProductName.toLowerCase().replace(" ", "-"))
                    .build();
            
            PriceComparisonResult result = smartProductService.comparePrices(request);
            log.info("Comparação simples concluída - Melhor preço: {} - {}", result.getBestPriceSite(), result.getBestPrice());
            return ResponseEntity.ok(result);
        });
    }

    // Cria Produtos
    @PostMapping
    public ResponseEntity<ProductMonitor> createProduct(@Valid @RequestBody CreateProductRequest request) {
        return executeWithLogging("Criando produto", () -> {
            ProductMonitor product = buildProductFromRequest(request);
            ProductMonitor savedProduct = productService.createProduct(product);
            log.info("Produto criado com sucesso: ID {}", savedProduct.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(savedProduct);
        });
    }

    // Busca Produtos
    @GetMapping
    public ResponseEntity<List<ProductMonitor>> getAllProducts() {
        return executeWithLogging("Buscando todos os produtos", () -> {
            List<ProductMonitor> products = productService.getAllActiveProducts();
            log.info("Retornando {} produtos ativos", products.size());
            return ResponseEntity.ok(products);
        });
    }

    // Buscar produto por Id
    @GetMapping("/{id}")
    public ResponseEntity<ProductMonitor> getProductById(@PathVariable Long id) {
        return executeWithLogging("Buscando produto por ID: " + id, () -> {
            Optional<ProductMonitor> product = productService.getProductById(id);

            if (product.isEmpty()) {
                log.warn("Produto não encontrado: ID {}", id);
                return ResponseEntity.notFound().build();
            }

            log.info("Produto encontrado: ID {}", id);
            return ResponseEntity.ok(product.get());
        });
    }
    // Buscar produto por categoria
    @GetMapping("/category/{category}")
    public ResponseEntity<List<ProductMonitor>> getProductsByCategory(@PathVariable ProductCategory category) {
        return executeWithLogging("Buscando produtos da categoria: " + category, () -> {
            List<ProductMonitor> products = productService.getProductsByCategory(category);
            log.info("Retornando {} produtos da categoria {}", products.size(), category);
            return ResponseEntity.ok(products);
        });
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductMonitor> updateProduct(@PathVariable Long id, @Valid @RequestBody UpdateProductRequest request) {
        return executeWithLogging("Atualizando produto ID: " + id, () -> {
            Optional<ProductMonitor> existingProduct = productService.getProductById(id);

            if (existingProduct.isEmpty()) {
                log.warn("Produto não encontrado para atualização: ID {}", id);
                return ResponseEntity.notFound().build();
            }

            ProductMonitor updatedProduct = updateProductFromRequest(existingProduct.get(), request);
            ProductMonitor savedProduct = productService.updateProduct(updatedProduct);

            log.info("Produto atualizado com sucesso: ID {}", id);
            return ResponseEntity.ok(savedProduct);
        });
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        return executeWithLogging("Desativando produto ID: " + id, () -> {
            productService.deactivateProduct(id);
            log.info("Produto desativado com sucesso: ID {}", id);
            return ResponseEntity.noContent().build();
        });
    }

    // ENDPOINTS DE AÇÃO


    // Verificar Preço
    @PostMapping("/{id}/check-price")
    public ResponseEntity<PriceCheckResponse> checkProductPrice(@PathVariable Long id) {
        return executeWithLogging("Verificando preço do produto ID: " + id, () -> {
            Optional<ProductMonitor> productOpt = productService.getProductById(id);

            if (productOpt.isEmpty()) {
                log.warn("Produto não encontrado para verificação: ID {}", id);
                return ResponseEntity.notFound().build();
            }

            ProductMonitor product = productOpt.get();
            BigDecimal oldPrice = product.getCurrentPrice();

            productService.checkProductPrice(product);

            PriceCheckResponse response = buildPriceCheckResponse(id, product, oldPrice);

            log.info("Verificação de preço concluída para produto ID {}: R$ {} → R$ {}",
                    id, oldPrice, product.getCurrentPrice());

            return ResponseEntity.ok(response);
        });
    }

    @PostMapping("/check-all-prices")
    public ResponseEntity<BulkPriceCheckResponse> checkAllPrices() {
        return executeWithLogging("Verificando preços de todos os produtos", () -> {
            int totalProducts = productService.getAllActiveProducts().size();
            productService.checkAllActivePrices();

            BulkPriceCheckResponse response = buildBulkPriceCheckResponse(totalProducts);

            log.info("Verificação de preços concluída para {} produtos", totalProducts);
            return ResponseEntity.ok(response);
        });
    }

    // ENDPOINTS DE ESTATÍSTICAS

    @GetMapping("/target-reached")
    public ResponseEntity<List<ProductMonitor>> getProductsWithTargetReached() {
        return executeWithLogging("Buscando produtos com preço alvo atingido", () -> {
            List<ProductMonitor> products = productService.getProductsWithTargetPriceReached();
            log.info("Retornando {} produtos com preço alvo atingido", products.size());
            return ResponseEntity.ok(products);
        });
    }

    @GetMapping("/stats")
    public ResponseEntity<SystemStatsResponse> getSystemStats() {
        return executeWithLogging("Buscando estatísticas do sistema", () -> {
            SystemStatsResponse response = buildSystemStatsResponse();
            log.info("Estatísticas do sistema: {} produtos ativos", response.getTotalActiveProducts());
            return ResponseEntity.ok(response);
        });
    }

    //  ENDPOINTS DE HEALTH CHECK

    @GetMapping("/health")
    public ResponseEntity<HealthCheckResponse> healthCheck() {
        return executeWithLogging("Health check da API", () -> {
            HealthCheckResponse response = buildHealthCheckResponse();
            return ResponseEntity.ok(response);
        });
    }

    // LIMPAR TODOS OS PRODUTOS
    @DeleteMapping("/clear-all")
    public ResponseEntity<String> clearAllProducts() {
        return executeWithLogging("Limpando todos os produtos", () -> {
            int deletedCount = productService.deleteAllProducts();
            log.info("Limpados {} produtos do sistema", deletedCount);
            return ResponseEntity.ok("Limpados " + deletedCount + " produtos do sistema");
        });
    }

    // LIMPAR TODOS OS PRODUTOS (GET - Para usar no browser)
    @GetMapping("/clear-all")
    public ResponseEntity<String> clearAllProductsGet() {
        return executeWithLogging("Limpando todos os produtos", () -> {
            int deletedCount = productService.deleteAllProducts();
            log.info("Limpados {} produtos do sistema", deletedCount);
            return ResponseEntity.ok("Limpados " + deletedCount + " produtos do sistema");
        });
    }

    // LIMPAR TODOS OS PRODUTOS (GET - Endpoint alternativo)
    @GetMapping("/reset")
    public ResponseEntity<String> resetDatabase() {
        try {
            int deletedCount = productService.deleteAllProducts();
            return ResponseEntity.ok("Database resetado! Removidos " + deletedCount + " produtos.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro ao resetar: " + e.getMessage());
        }
    }

    // TESTE DE EXTRAÇÃO DE PREÇO
    @PostMapping("/test-price-extraction")
    public ResponseEntity<String> testPriceExtraction(@RequestBody String url) {
        try {
            BigDecimal price = smartProductService.extractPriceFromUrl(url);
            if (price != null) {
                return ResponseEntity.ok("Preço extraído com sucesso: R$ " + price);
            } else {
                return ResponseEntity.ok("Não foi possível extrair preço da URL: " + url);
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro: " + e.getMessage());
        }
    }

    // DEBUG DETALHADO DA EXTRAÇÃO DE PREÇO
    @PostMapping("/debug-price-extraction")
    public ResponseEntity<String> debugPriceExtraction(@RequestBody String url) {
        try {
            return ResponseEntity.ok(smartProductService.debugPriceExtraction(url));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro: " + e.getMessage());
        }
    }

    // DEBUG DE BUSCA EM SITES ESPECÍFICOS
    @PostMapping("/debug-site-search")
    public ResponseEntity<String> debugSiteSearch(@RequestBody DebugSiteSearchRequest request) {
        try {
            return ResponseEntity.ok(smartProductService.debugSiteSearch(request.getProductName(), request.getSiteName()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro: " + e.getMessage());
        }
    }

    // DEBUG DE BUSCA EM TODOS OS SITES
    @PostMapping("/debug-all-sites")
    public ResponseEntity<String> debugAllSites(@RequestBody String productName) {
        try {
            return ResponseEntity.ok(smartProductService.debugAllSites(productName));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro: " + e.getMessage());
        }
    }

    // TESTE SELENIUM PARA MERCADO LIVRE
    @PostMapping("/test-selenium")
    public ResponseEntity<String> testSelenium(@RequestBody DebugSiteSearchRequest request) {
        try {
            return ResponseEntity.ok(smartProductService.testSeleniumMercadoLivre(request.getProductName()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro: " + e.getMessage());
        }
    }

    //  MÉTODOS AUXILIARES

    private <T> ResponseEntity<T> executeWithLogging(String operation, Supplier<ResponseEntity<T>> operationSupplier) {
        try {
            log.info("Iniciando operação: {}", operation);
            return operationSupplier.get();
        } catch (Exception e) {
            log.error("Erro na operação '{}': {}", operation, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private ProductMonitor buildProductFromRequest(CreateProductRequest request) {
        return ProductMonitor.builder()
                .productUrl(request.getProductUrl())
                .targetPrice(request.getTargetPrice())
                .productName(request.getProductName())
                .originCity(request.getOriginCity())
                .destinationCity(request.getDestinationCity())
                .travelDate(request.getTravelDate())
                .passengersCount(request.getPassengersCount())
                .build();
    }

    private ProductMonitor updateProductFromRequest(ProductMonitor existingProduct, UpdateProductRequest request) {
        return existingProduct.toBuilder()
                .productName(request.getProductName() != null ? request.getProductName() : existingProduct.getProductName())
                .targetPrice(request.getTargetPrice() != null ? request.getTargetPrice() : existingProduct.getTargetPrice())
                .productUrl(request.getProductUrl() != null ? request.getProductUrl() : existingProduct.getProductUrl())
                .build();
    }

    private PriceCheckResponse buildPriceCheckResponse(Long id, ProductMonitor product, BigDecimal oldPrice) {
        BigDecimal newPrice = product.getCurrentPrice();
        boolean priceChanged = oldPrice != null && !oldPrice.equals(newPrice);
        boolean targetReached = newPrice != null && newPrice.compareTo(product.getTargetPrice()) <= 0;

        return PriceCheckResponse.builder()
                .productId(id)
                .oldPrice(oldPrice)
                .newPrice(newPrice)
                .targetPrice(product.getTargetPrice())
                .priceChanged(priceChanged)
                .targetReached(targetReached)
                .lastChecked(product.getLastChecked())
                .build();
    }

    private BulkPriceCheckResponse buildBulkPriceCheckResponse(int totalProducts) {
        return BulkPriceCheckResponse.builder()
                .totalProducts(totalProducts)
                .checkedAt(LocalDateTime.now())
                .message("Verificação de preços concluída")
                .build();
    }

    // ESTATÍSTICAS COMPLETAS - Todas as categorias
    private SystemStatsResponse buildSystemStatsResponse() {
        long totalProducts = productService.getTotalActiveProducts();

        // Contagem por categoria - TODAS as 10 categorias
        long electronicsCount = productService.getProductsByCategoryCount(ProductCategory.ELETRONICOS);
        long flightsCount = productService.getProductsByCategoryCount(ProductCategory.PASSAGENS_AEREAS);
        long hotelsCount = productService.getProductsByCategoryCount(ProductCategory.HOTEIS);
        long clothesCount = productService.getProductsByCategoryCount(ProductCategory.ROUPAS);
        long booksCount = productService.getProductsByCategoryCount(ProductCategory.LIVROS);
        long homeGardenCount = productService.getProductsByCategoryCount(ProductCategory.CASA_E_JARDIM);
        long sportsCount = productService.getProductsByCategoryCount(ProductCategory.ESPORTES);
        long healthBeautyCount = productService.getProductsByCategoryCount(ProductCategory.SAUDE_E_BELEZA);
        long automotiveCount = productService.getProductsByCategoryCount(ProductCategory.AUTOMOTIVO);
        long othersCount = productService.getProductsByCategoryCount(ProductCategory.OUTROS);

        return SystemStatsResponse.builder()
                .totalActiveProducts(totalProducts)
                .electronicsCount(electronicsCount)
                .flightsCount(flightsCount)
                .hotelsCount(hotelsCount)
                .clothesCount(clothesCount)
                .booksCount(booksCount)
                .homeGardenCount(homeGardenCount)
                .sportsCount(sportsCount)
                .healthBeautyCount(healthBeautyCount)  // Perfumes, cosméticos
                .automotiveCount(automotiveCount)
                .othersCount(othersCount)
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    private HealthCheckResponse buildHealthCheckResponse() {
        return HealthCheckResponse.builder()
                .status("UP")
                .timestamp(LocalDateTime.now())
                .message("API funcionando normalmente")
                .build();
    }
}