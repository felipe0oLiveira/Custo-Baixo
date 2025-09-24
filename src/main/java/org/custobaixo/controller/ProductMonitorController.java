package org.custobaixo.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.custobaixo.entity.ProductCategory;
import org.custobaixo.entity.ProductMonitor;
import org.custobaixo.service.ProductMonitorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ProductMonitorController {

    private final ProductMonitorService productService;

    //  ENDPOINTS CRUD

    @PostMapping
    public ResponseEntity<ProductMonitor> createProduct(@Valid @RequestBody CreateProductRequest request) {
        return executeWithLogging("Criando produto", () -> {
            ProductMonitor product = buildProductFromRequest(request);
            ProductMonitor savedProduct = productService.createProduct(product);
            log.info("Produto criado com sucesso: ID {}", savedProduct.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(savedProduct);
        });
    }

    @GetMapping
    public ResponseEntity<List<ProductMonitor>> getAllProducts() {
        return executeWithLogging("Buscando todos os produtos", () -> {
            List<ProductMonitor> products = productService.getAllActiveProducts();
            log.info("Retornando {} produtos ativos", products.size());
            return ResponseEntity.ok(products);
        });
    }

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



 }