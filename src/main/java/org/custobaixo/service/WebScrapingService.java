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
import java.util.Random;


@Service
public class WebScrapingService {

    private static final Logger log = LoggerFactory.getLogger(WebScrapingService.class);

    //  ROTAÇÃO DE USER-AGENTS
    private final String[] USER_AGENTS = {
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/121.0",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:109.0) Gecko/20100101 Firefox/121.0"
    };
    
    //  PROXIES (exemplo - em produção use serviços pagos)
    private final String[][] PROXIES = {
        {"proxy1.example.com", "8080"},
        {"proxy2.example.com", "8080"},
        {"proxy3.example.com", "8080"}
    };
    
    private final Random random = new Random();

    // Mapa de sites e seus métodos de extração
    private final Map<String, Function<ProductMonitor, BigDecimal>> siteExtractors = createSiteExtractors();

    private Map<String, Function<ProductMonitor, BigDecimal>> createSiteExtractors() {
        Map<String, Function<ProductMonitor, BigDecimal>> extractors = new HashMap<>();
        // Apenas os 4 sites ativos do sistema
        extractors.put("amazon.com.br", this::extractAmazonPrice);
        extractors.put("mercadolivre.com.br", this::extractMercadoLivrePrice);
        extractors.put("kabum.com.br", this::extractGenericPrice);
        extractors.put("netshoes.com.br", this::extractGenericPrice);
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

    // MÉTODOS DE EXTRAÇÃO ESPECÍFICOS (apenas sites ativos)

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
        //  Selecionar User-Agent aleatório
        String userAgent = USER_AGENTS[random.nextInt(USER_AGENTS.length)];
        log.debug("Usando User-Agent: {}", userAgent);
        
        // Selecionar proxy aleatório (se disponível)
        String[] proxy = PROXIES[random.nextInt(PROXIES.length)];
        String proxyHost = proxy[0];
        int proxyPort = Integer.parseInt(proxy[1]);
        log.debug("Usando proxy: {}:{}", proxyHost, proxyPort);
        
        return Jsoup.connect(url)
                .userAgent(userAgent)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                .header("Accept-Language", "pt-BR,pt;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("DNT", "1")
                .header("Connection", "keep-alive")
                .header("Upgrade-Insecure-Requests", "1")
                .header("Sec-Fetch-Dest", "document")
                .header("Sec-Fetch-Mode", "navigate")
                .header("Sec-Fetch-Site", "none")
                .header("Sec-Fetch-User", "?1")
                .header("Cache-Control", "max-age=0")
                .header("sec-ch-ua", "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Google Chrome\";v=\"120\"")
                .header("sec-ch-ua-mobile", "?0")
                .header("sec-ch-ua-platform", "\"Windows\"")
                .timeout(10000)
                .followRedirects(true)
                .maxBodySize(0)
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