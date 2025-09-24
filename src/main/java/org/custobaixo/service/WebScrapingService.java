package org.custobaixo.service;

import org.custobaixo.entity.ProductMonitor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Function;
import java.util.HashMap;


@Service
public class WebScrapingService {

    private static final Logger log = LoggerFactory.getLogger(WebScrapingService.class);

    // Mapa de sites e seus métodos de extração
    private final Map<String, Function<ProductMonitor, BigDecimal>> siteExtractors = createSiteExtractors();

    private Map<String, Function<ProductMonitor, BigDecimal>> createSiteExtractors() {
        Map<String, Function<ProductMonitor, BigDecimal>> extractors = new HashMap<>();
        extractors.put("amazon.com.br", this::extractAmazonPrice);
        extractors.put("mercadolivre.com.br", this::extractMercadoLivrePrice);
        extractors.put("magazineluiza.com.br", this::extractMagazineLuizaPrice);
        extractors.put("americanas.com.br", this::extractAmericanasPrice);
        extractors.put("submarino.com.br", this::extractSubmarinoPrice);
        extractors.put("shoptime.com.br", this::extractGenericPrice);
        extractors.put("extra.com.br", this::extractGenericPrice);
        extractors.put("ponto.com.br", this::extractGenericPrice);
        extractors.put("zara.com", this::extractZaraPrice);
        extractors.put("renner.com.br", this::extractGenericPrice);
        extractors.put("cea.com.br", this::extractGenericPrice);
        extractors.put("hm.com", this::extractGenericPrice);
        extractors.put("nike.com.br", this::extractNikePrice);
        extractors.put("adidas.com.br", this::extractGenericPrice);
        extractors.put("olympikus.com.br", this::extractGenericPrice);
        extractors.put("puma.com.br", this::extractGenericPrice);
        extractors.put("centauro.com.br", this::extractGenericPrice);
        extractors.put("saraiva.com.br", this::extractSaraivaPrice);
        extractors.put("cultura.com.br", this::extractGenericPrice);
        extractors.put("voeazul.com.br", this::extractAzulPrice);
        extractors.put("voegol.com.br", this::extractGolPrice);
        extractors.put("latam.com", this::extractLatamPrice);
        extractors.put("decolar.com.br", this::extractGenericPrice);
        extractors.put("123milhas.com.br", this::extractGenericPrice);
        extractors.put("booking.com", this::extractGenericPrice);
        extractors.put("hotels.com", this::extractGenericPrice);
        extractors.put("leroymerlin.com.br", this::extractGenericPrice);
        extractors.put("casasbahia.com.br", this::extractGenericPrice);
        extractors.put("sephora.com.br", this::extractGenericPrice);
        extractors.put("oboticario.com.br", this::extractGenericPrice);
        extractors.put("natura.com.br", this::extractGenericPrice);
        return extractors;
    }

    public BigDecimal extractPrice(ProductMonitor product) {
        try {
            String url = product.getProductUrl().toLowerCase();

            // Encontrar o extrator específico para o site
            Function<ProductMonitor, BigDecimal> extractor = findSiteExtractor(url);

            // Executar extração
            return extractor.apply(product);

        } catch (Exception e) {
            log.error("Erro ao extrair preço do produto: {}", product.getId(), e);
            return null;
        }
    }

    private Function<ProductMonitor, BigDecimal> findSiteExtractor(String url) {
        return siteExtractors.entrySet().stream()
                .filter(entry -> url.contains(entry.getKey()))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(this::extractGenericPrice);
    }

    // MÉTODOS DE EXTRAÇÃO ESPECÍFICOS

    private BigDecimal extractAmazonPrice(ProductMonitor product) {
        return extractPriceWithSelectors(product,
                ".a-price-whole",
                ".a-price .a-offscreen",
                ".a-price-range .a-price-whole",
                ".a-price .a-text-price"
        );
    }

    private BigDecimal extractMercadoLivrePrice(ProductMonitor product) {
        return extractPriceWithSelectors(product,
                ".andes-money-amount__fraction",
                ".price-tag-fraction",
                ".andes-money-amount__cents",
                ".price-tag"
        );
    }

    private BigDecimal extractMagazineLuizaPrice(ProductMonitor product) {
        return extractPriceWithSelectors(product,
                ".price-template__text",
                ".price-template__text--strong",
                ".price-template",
                ".price-value"
        );
    }

    private BigDecimal extractAmericanasPrice(ProductMonitor product) {
        return extractPriceWithSelectors(product,
                ".price-current",
                ".price-value",
                ".price-now",
                ".price-main"
        );
    }

    private BigDecimal extractSubmarinoPrice(ProductMonitor product) {
        return extractPriceWithSelectors(product,
                ".price-current",
                ".price-value",
                ".price-now",
                ".price-main"
        );
    }

    private BigDecimal extractZaraPrice(ProductMonitor product) {
        return extractPriceWithSelectors(product,
                ".price-current",
                ".price-value",
                ".price-now",
                ".price-main"
        );
    }

    private BigDecimal extractNikePrice(ProductMonitor product) {
        return extractPriceWithSelectors(product,
                ".product-price",
                ".price-value",
                ".price-current",
                ".price-now"
        );
    }

    private BigDecimal extractSaraivaPrice(ProductMonitor product) {
        return extractPriceWithSelectors(product,
                ".price-value",
                ".price-current",
                ".price-now",
                ".price-main"
        );
    }

    private BigDecimal extractAzulPrice(ProductMonitor product) {
        return extractPriceWithSelectors(product,
                ".flight-price .amount",
                ".price-value",
                ".price-current",
                ".price-now"
        );
    }

    private BigDecimal extractGolPrice(ProductMonitor product) {
        return extractPriceWithSelectors(product,
                ".price-value",
                ".flight-price",
                ".price-current",
                ".price-now"
        );
    }

    private BigDecimal extractLatamPrice(ProductMonitor product) {
        return extractPriceWithSelectors(product,
                ".flight-price",
                ".price-value",
                ".price-current",
                ".price-now"
        );
    }

    //  MÉTODOS AUXILIARES

    private BigDecimal extractPriceWithSelectors(ProductMonitor product, String... selectors) {
        try {
            Document doc = connectToUrl(product.getProductUrl());

            for (String selector : selectors) {
                BigDecimal price = tryExtractPrice(doc, selector);
                if (price != null) {
                    return price;
                }
            }

        } catch (Exception e) {
            log.error("Erro ao extrair preço do produto: {}", product.getProductUrl(), e);
        }
        return null;
    }

    private Document connectToUrl(String url) throws Exception {
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(10000)
                .get();
    }

    private BigDecimal tryExtractPrice(Document doc, String selector) {
        try {
            Element priceElement = doc.selectFirst(selector);
            if (priceElement != null) {
                String priceText = priceElement.text().replaceAll("[^0-9,]", "");
                if (!priceText.isEmpty()) {
                    return new BigDecimal(priceText.replace(",", "."));
                }
            }
        } catch (Exception e) {
            log.debug("Erro ao extrair preço com seletor '{}': {}", selector, e.getMessage());
        }
        return null;
    }

    private BigDecimal extractGenericPrice(ProductMonitor product) {
        String[] genericSelectors = {
                ".price", ".valor", ".preco", ".amount", ".cost",
                ".price-current", ".price-value", ".price-now",
                ".price-main", ".price-tag", ".price-fraction",
                "[class*='price']", "[class*='valor']", "[class*='preco']",
                "[data-testid*='price']", "[data-test*='price']"
        };

        return extractPriceWithSelectors(product, genericSelectors);
    }
}