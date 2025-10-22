package org.custobaixo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.custobaixo.dto.PriceComparisonRequest;
import org.custobaixo.dto.PriceComparisonResult;
import org.custobaixo.dto.SmartMonitorRequest;
import org.custobaixo.dto.SmartMonitorResult;
import org.custobaixo.entity.ProductCategory;
import org.custobaixo.entity.ProductMonitor;
import org.custobaixo.model.ProductData;
import org.custobaixo.model.SiteConfig;
import org.custobaixo.model.SitePrice;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmartProductService {

    private final SeleniumService seleniumService;

    private final ProductMonitorService productService;
    private final SmartCategoryService categoryService;

    // ROTAÇÃO DE USER-AGENTS
    private final String[] USER_AGENTS = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/121.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:109.0) Gecko/20100101 Firefox/121.0"
    };

    private final Random random = new Random();

    // SITES PARA MONITORAMENTO
    private final List<SiteConfig> SITES = List.of(
            new SiteConfig("AMAZON", "https://www.amazon.com.br/s?k=%s", ".s-result-item, [data-component-type='s-search-result'], .s-search-result",
                    "h2 a span, .a-size-medium, .s-size-mini, .a-link-normal span, .s-color-base", ".a-price-whole, .a-price .a-offscreen, .a-price-range .a-price-whole", "h2 a, .a-link-normal", true, "https://www.amazon.com.br"),
            new SiteConfig("MERCADO_LIVRE", "https://lista.mercadolivre.com.br/%s", ".ui-search-results .ui-search-item, .shops__item, .item",
                    ".ui-search-item__title, .shops__item-title, .item__title", ".andes-money-amount__fraction, .price-tag-fraction, .andes-money-amount", ".ui-search-item__title a, .shops__item-title a, .item__title a", true, "https://www.mercadolivre.com.br"),
            new SiteConfig("KABUM", "https://www.kabum.com.br/busca/%s", "article[data-id], .productCard, .sc-dcJsrY, article.sc-fXEqDS, div[data-product-id]",
                    ".sc-iBYQkv, .nameCard, h2.sc-iBYQkv, span.sc-iBYQkv, .sc-gswNZR h2", ".priceCard, .sc-dcJsrY span, .finalPrice, [data-testid='price'], .preco, span.sc-dcJsrY", "a.productLink, a[href*='/produto/'], .sc-dAlyuH a", true, "https://www.kabum.com.br"),
            
            // SITES DE ROUPAS E MODA
            new SiteConfig("NETSHOES", "https://www.netshoes.com.br/busca?q=%s", ".item-card, .product-item",
                    ".item-card__title, .product-name", ".item-card__price, .price", "a", true, "https://www.netshoes.com.br")
    );

    // MÉTODO PRINCIPAL - MONITORAMENTO INTELIGENTE COM COMPARAÇÃO E SALVAMENTO
    public SmartMonitorResult createSmartMonitoring(SmartMonitorRequest request) {
        logSmartMonitoringStart(request.getProductName());

        try {
            // 1. DETECTAR CATEGORIA DO PRODUTO
            ProductCategory category = categoryService.detectCategory(request.getProductName(), request.getProductUrl());
            logCategoryDetected(category);

            // 2. OBTER SITES RELEVANTES PARA A CATEGORIA
            List<String> relevantSiteNames = categoryService.getRelevantSites(category);
            List<SiteConfig> relevantSites = SITES.stream()
                    .filter(site -> relevantSiteNames.contains(site.name))
                    .toList();

            logRelevantSites(category, relevantSiteNames);

            // 3. EXTRAIR PREÇO ORIGINAL
            BigDecimal originalPrice = extractPriceFromUrl(request.getProductUrl());
            if (originalPrice == null) {
                return SmartMonitorResult.builder()
                        .productName(request.getProductName())
                        .originalUrl(request.getProductUrl())
                        .targetPrice(request.getTargetPrice())
                        .status("ERROR")
                        .message("Não foi possível extrair preço da URL original")
                        .createdAt(LocalDateTime.now())
                        .build();
            }

            // 4. BUSCAR PRODUTO APENAS NOS SITES RELEVANTES
            List<SitePrice> allPrices = new ArrayList<>();
            int sitesWithProduct = 0;

            for (SiteConfig site : relevantSites) {
                try {
                    List<SitePrice> sitePrices = searchProductPricesForMonitoring(request.getProductName(), site, request.getTargetPrice(), category);
                    allPrices.addAll(sitePrices);
                    if (!sitePrices.isEmpty()) {
                        sitesWithProduct++;
                    }

                    // Pequena pausa entre sites
                    Thread.sleep(1000);

                } catch (Exception e) {
                    logSiteError(site.name, e.getMessage());
                }
            }

            // 3. ENCONTRAR MELHOR PREÇO
            SitePrice bestPrice = allPrices.stream()
                    .filter(price -> price.isAvailable() && price.getPrice() != null)
                    .min(Comparator.comparing(SitePrice::getPrice))
                    .orElse(null);

            // 4. CALCULAR ECONOMIA
            BigDecimal savings = null;
            BigDecimal savingsPercentage = null;
            if (bestPrice != null && bestPrice.getPrice().compareTo(originalPrice) < 0) {
                savings = originalPrice.subtract(bestPrice.getPrice());
                savingsPercentage = savings.divide(originalPrice, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
            }

            return SmartMonitorResult.builder()
                    .productName(request.getProductName())
                    .originalUrl(request.getProductUrl())
                    .originalPrice(originalPrice)
                    .targetPrice(request.getTargetPrice())
                    .totalSitesSearched(relevantSites.size())
                    .sitesWithProduct(sitesWithProduct)
                    .bestPriceSite(bestPrice != null ? bestPrice.getSiteName() : null)
                    .bestPrice(bestPrice != null ? bestPrice.getPrice() : null)
                    .bestPriceUrl(bestPrice != null ? bestPrice.getProductUrl() : null)
                    .savings(savings)
                    .savingsPercentage(savingsPercentage)
                    .allPrices(allPrices)
                    .status(sitesWithProduct > 0 ? "MONITORING" : "ERROR")
                    .message(sitesWithProduct > 0 ?
                            String.format("Monitoramento iniciado em %d sites. Melhor preço: %s", sitesWithProduct,
                                    bestPrice != null ? bestPrice.getSiteName() : "Nenhum") :
                            "Produto não encontrado em nenhum site")
                    .createdAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Erro no monitoramento inteligente: {}", e.getMessage(), e);
            return SmartMonitorResult.builder()
                    .productName(request.getProductName())
                    .originalUrl(request.getProductUrl())
                    .targetPrice(request.getTargetPrice())
                    .status("ERROR")
                    .message("Erro interno: " + e.getMessage())
                    .createdAt(LocalDateTime.now())
                    .build();
        }
    }

    // MÉTODO PRINCIPAL - COMPARAR PREÇOS EM TODOS OS SITES (SEM SALVAMENTO)
    public PriceComparisonResult comparePrices(PriceComparisonRequest request) {
        logPriceComparisonStart(request.getProductName());

        try {
            // 1. DETECTAR CATEGORIA DO PRODUTO
            ProductCategory category = categoryService.detectCategory(request.getProductName(), request.getProductUrl());
            logCategoryDetected(category);

            // 2. OBTER SITES RELEVANTES PARA A CATEGORIA
            List<String> relevantSiteNames = categoryService.getRelevantSites(category);
            List<SiteConfig> relevantSites = SITES.stream()
                    .filter(site -> relevantSiteNames.contains(site.name))
                    .toList();

            logRelevantSites(category, relevantSiteNames);

            // 3. EXTRAIR PREÇO ORIGINAL (opcional para comparação)
            BigDecimal originalPrice = null;
            boolean isFakeUrl = request.getProductUrl().contains("exemplo.com") || 
                               request.getProductUrl().contains("fake") ||
                               request.getProductUrl().startsWith("https://exemplo.com");
            
            if (!isFakeUrl) {
                originalPrice = extractPriceFromUrl(request.getProductUrl());
                log.info("Preço original extraído: {}", originalPrice);
            } else {
                log.info("URL fake detectada, pulando extração de preço original");
            }

            // 4. BUSCAR PRODUTO APENAS NOS SITES RELEVANTES
            List<SitePrice> allPrices = new ArrayList<>();
            int sitesWithProduct = 0;

            for (SiteConfig site : relevantSites) {
                try {
                    log.info("========== Buscando em: {} ==========", site.name);
                    List<SitePrice> sitePrices = searchProductPricesInSite(request.getProductName(), site);
                    log.info("Site {} retornou {} produtos", site.name, sitePrices.size());
                    allPrices.addAll(sitePrices);
                    if (!sitePrices.isEmpty()) {
                        sitesWithProduct++;
                        log.info("Site {} adicionado ao resultado final", site.name);
                    }

                    // Pequena pausa entre sites
                    Thread.sleep(1000);

                } catch (Exception e) {
                    log.error("Erro ao buscar em {}: {}", site.name, e.getMessage(), e);
                    logSiteError(site.name, e.getMessage());
                }
            }
            
            log.info("========== BUSCA FINALIZADA ==========");
            log.info("Total de preços coletados: {}", allPrices.size());
            log.info("Sites com produtos: {}", sitesWithProduct);

            // 3. ORDENAR PREÇOS E MARCAR MELHOR PREÇO
            List<SitePrice> sortedPrices = allPrices.stream()
                    .filter(price -> price.isAvailable() && price.getPrice() != null)
                    .sorted(Comparator.comparing(SitePrice::getPrice))
                    .collect(ArrayList::new, (list, price) -> {
                        // Criar nova instância com campos adicionais
                        SitePrice newPrice = SitePrice.builder()
                                .siteName(price.getSiteName())
                                .price(price.getPrice())
                                .productUrl(price.getProductUrl())
                                .available(price.isAvailable())
                                .productName(price.getProductName())
                                .rank(list.size() + 1) // Posição na ordenação (1 = melhor)
                                .isBestPrice(list.isEmpty()) // Primeiro é o melhor preço
                                .build();
                        list.add(newPrice);
                    }, ArrayList::addAll);

            // 4. ENCONTRAR MELHOR PREÇO (primeiro da lista ordenada)
            SitePrice bestPrice = sortedPrices.isEmpty() ? null : sortedPrices.get(0);

            // 5. CALCULAR ECONOMIA (apenas se houver preço original)
            BigDecimal savings = null;
            BigDecimal savingsPercentage = null;
            if (bestPrice != null && originalPrice != null && bestPrice.getPrice().compareTo(originalPrice) < 0) {
                savings = originalPrice.subtract(bestPrice.getPrice());
                savingsPercentage = savings.divide(originalPrice, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
            }

            return PriceComparisonResult.builder()
                    .productName(request.getProductName())
                    .originalUrl(request.getProductUrl())
                    .originalPrice(originalPrice)
                    .totalSitesSearched(relevantSites.size())
                    .sitesWithProduct(sitesWithProduct)
                    .bestPriceSite(bestPrice != null ? bestPrice.getSiteName() : null)
                    .bestPrice(bestPrice != null ? bestPrice.getPrice() : null)
                    .bestPriceUrl(bestPrice != null ? bestPrice.getProductUrl() : null)
                    .savings(savings)
                    .savingsPercentage(savingsPercentage)
                    .allPrices(sortedPrices) // Lista ordenada do menor para o maior
                    .status(sitesWithProduct > 0 ? "SUCCESS" : "ERROR")
                    .message(sitesWithProduct > 0 ?
                            String.format("Encontrado em %d sites. Melhor preço: %s", sitesWithProduct,
                                    bestPrice != null ? bestPrice.getSiteName() : "Nenhum") :
                            "Produto não encontrado em nenhum site")
                    .searchDate(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Erro na comparação de preços: {}", e.getMessage(), e);
            return PriceComparisonResult.builder()
                    .productName(request.getProductName())
                    .originalUrl(request.getProductUrl())
                    .status("ERROR")
                    .message("Erro interno: " + e.getMessage())
                    .searchDate(LocalDateTime.now())
                    .build();
        }
    }


    // EXTRAIR PREÇO DE UMA URL ESPECÍFICA
    public BigDecimal extractPriceFromUrl(String url) {
        // Remover aspas extras se existirem
        String cleanUrl = url.trim();
        if (cleanUrl.startsWith("\"") && cleanUrl.endsWith("\"")) {
            cleanUrl = cleanUrl.substring(1, cleanUrl.length() - 1);
        }
        
        try {
            log.info("Tentando extrair preço da URL: {}", cleanUrl);
            
            Document doc = Jsoup.connect(cleanUrl)
                    .userAgent(USER_AGENTS[random.nextInt(USER_AGENTS.length)])
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                    .header("Accept-Language", "pt-BR,pt;q=0.9,en;q=0.8")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("DNT", "1")
                    .header("Connection", "keep-alive")
                    .header("Upgrade-Insecure-Requests", "1")
                    .timeout(20000)
                    .followRedirects(true)
                    .get();

            log.info("Documento carregado com sucesso. Título: {}", doc.title());

            // Tentar seletores específicos para Amazon
            if (cleanUrl.contains("amazon.com.br")) {
                String[] amazonSelectors = {
                    ".a-price-whole", 
                    ".a-price .a-offscreen", 
                    ".a-price-range .a-price-whole",
                    "[data-asin] .a-price-whole",
                    ".a-price-symbol + .a-price-whole",
                    "#apex_desktop .a-price-whole",
                    "#apex_desktop .a-offscreen",
                    ".a-price .a-price-symbol + .a-price-whole"
                };
                
                for (String selector : amazonSelectors) {
                    Element priceElement = doc.selectFirst(selector);
                    if (priceElement != null) {
                        String priceText = priceElement.text();
                        BigDecimal price = parseBrazilianPrice(priceText);
                        if (price != null) {
                            log.info("Preço extraído com sucesso usando seletor: {} - R$ {}", selector, price);
                            return price;
                        }
                    }
                }
            }

            // Seletores genéricos para outros sites
            String[] genericSelectors = {
                    ".andes-money-amount__fraction", ".price-tag-fraction", // Mercado Livre
                    ".priceCard", ".oldPriceCard", // Kabum
                    ".price-current", ".price-value", // Outros sites
                    ".price", ".valor", ".preco", ".amount", ".cost"
            };

            for (String selector : genericSelectors) {
                Element priceElement = doc.selectFirst(selector);
                if (priceElement != null) {
                    String priceText = priceElement.text();
                    BigDecimal price = parseBrazilianPrice(priceText);
                    if (price != null) {
                        log.info("Preço extraído com seletor genérico: {} - R$ {}", selector, price);
                        return price;
                    }
                }
            }
            
            log.warn("Nenhum preço encontrado na URL: {}", cleanUrl);

        } catch (Exception e) {
            logPriceExtractionError(cleanUrl, e.getMessage());
        }
        return null;
    }

    // PARSING DE PREÇOS BRASILEIROS
    private BigDecimal parseBrazilianPrice(String priceText) {
        try {
            if (priceText == null || priceText.trim().isEmpty()) {
                return null;
            }

            // Remove símbolos de moeda e espaços
            String cleaned = priceText.replaceAll("[R$\\s]", "").trim();
            
            // Remove vírgulas no final (ex: "4.699," -> "4.699")
            cleaned = cleaned.replaceAll(",$", "");
            
            // Se não tem vírgula, trata como número simples
            if (!cleaned.contains(",")) {
                // Remove pontos (separadores de milhares) e converte
                String numberOnly = cleaned.replaceAll("[^\\d]", "");
                if (!numberOnly.isEmpty()) {
                    return new BigDecimal(numberOnly);
                }
            }
            
            // Se tem vírgula, trata como formato brasileiro (1.234,56)
            if (cleaned.contains(",")) {
                // Divide em parte inteira e decimal
                String[] parts = cleaned.split(",");
                if (parts.length == 2) {
                    String integerPart = parts[0].replaceAll("[^\\d]", ""); // Remove pontos
                    String decimalPart = parts[1].replaceAll("[^\\d]", ""); // Remove caracteres não numéricos
                    
                    if (!integerPart.isEmpty()) {
                        String fullNumber = integerPart + "." + decimalPart;
                        return new BigDecimal(fullNumber);
                    }
                }
            }
            
            // Fallback: tenta converter diretamente
            String numberOnly = cleaned.replaceAll("[^\\d,.-]", "");
            if (!numberOnly.isEmpty()) {
                // Se tem vírgula, assume que é decimal
                if (numberOnly.contains(",")) {
                    numberOnly = numberOnly.replace(",", ".");
                }
                return new BigDecimal(numberOnly);
            }
            
        } catch (Exception e) {
            log.debug("Erro ao fazer parse do preço '{}': {}", priceText, e.getMessage());
        }
        
        return null;
    }

    // DEBUG DETALHADO DA EXTRAÇÃO DE PREÇO
    public String debugPriceExtraction(String url) {
        StringBuilder debug = new StringBuilder();
        
        // Remover aspas extras se existirem
        String cleanUrl = url.trim();
        if (cleanUrl.startsWith("\"") && cleanUrl.endsWith("\"")) {
            cleanUrl = cleanUrl.substring(1, cleanUrl.length() - 1);
        }
        
        debug.append("=== DEBUG EXTRAÇÃO DE PREÇO ===\n");
        debug.append("URL original: ").append(url).append("\n");
        debug.append("URL limpa: ").append(cleanUrl).append("\n\n");

        try {
            debug.append("Tentando conectar à URL...\n");
            
            Document doc = Jsoup.connect(cleanUrl)
                    .userAgent(USER_AGENTS[random.nextInt(USER_AGENTS.length)])
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                    .header("Accept-Language", "pt-BR,pt;q=0.9,en;q=0.8")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("DNT", "1")
                    .header("Connection", "keep-alive")
                    .header("Upgrade-Insecure-Requests", "1")
                    .timeout(20000)
                    .followRedirects(true)
                    .get();

            debug.append(" Conexão bem-sucedida!\n");
            debug.append("Título da página: ").append(doc.title()).append("\n\n");

            // Testar seletores da Amazon
            if (url.contains("amazon.com.br")) {
                debug.append("=== TESTANDO SELETORES AMAZON ===\n");
                String[] amazonSelectors = {
                    ".a-price-whole", 
                    ".a-price .a-offscreen", 
                    ".a-price-range .a-price-whole",
                    "[data-asin] .a-price-whole",
                    ".a-price-symbol + .a-price-whole",
                    "#apex_desktop .a-price-whole",
                    "#apex_desktop .a-offscreen",
                    ".a-price .a-price-symbol + .a-price-whole"
                };
                
                for (String selector : amazonSelectors) {
                    Element element = doc.selectFirst(selector);
                    if (element != null) {
                        String text = element.text();
                        debug.append(" Seletor: ").append(selector).append("\n");
                        debug.append("   Texto encontrado: '").append(text).append("'\n");
                        BigDecimal price = parseBrazilianPrice(text);
                        if (price != null) {
                            debug.append("   Preço parseado: R$ ").append(price).append("\n");
                        } else {
                            debug.append("   Falha no parse do preço\n");
                        }
                    } else {
                        debug.append(" Seletor: ").append(selector).append(" - Não encontrado\n");
                    }
                }
            }

            debug.append("\n=== HTML RELEVANTE ===\n");
            // Buscar por elementos que contenham "price" ou "preço"
            Elements priceElements = doc.select("[class*='price'], [id*='price'], [data-*='price']");
            for (int i = 0; i < Math.min(5, priceElements.size()); i++) {
                Element elem = priceElements.get(i);
                debug.append("Elemento ").append(i + 1).append(": ").append(elem.tagName())
                     .append(" class='").append(elem.className()).append("'")
                     .append(" id='").append(elem.id()).append("'")
                     .append(" text='").append(elem.text()).append("'\n");
            }

        } catch (Exception e) {
            debug.append("Erro: ").append(e.getMessage()).append("\n");
            debug.append("Stack trace: ").append(e.getStackTrace()[0]).append("\n");
        }

        return debug.toString();
    }

    // BUSCAR PREÇOS EM SITE ESPECÍFICO E SALVAR PARA MONITORAMENTO
    private List<SitePrice> searchProductPricesForMonitoring(String productName, SiteConfig site, BigDecimal targetPrice, ProductCategory category) {
        List<SitePrice> prices = new ArrayList<>();

        try {
            String searchUrl = String.format(site.searchUrl, URLEncoder.encode(productName, StandardCharsets.UTF_8));

            Document doc = Jsoup.connect(searchUrl)
                    .userAgent(USER_AGENTS[random.nextInt(USER_AGENTS.length)])
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                    .header("Accept-Language", "pt-BR,pt;q=0.9,en;q=0.8")
                    .timeout(15000)
                    .get();

            Elements productElements = doc.select(site.productSelector);

            for (Element element : productElements) {
                try {
                    String name = extractText(element, site.nameSelector);
                    String url = extractUrl(element, site.urlSelector);
                    BigDecimal price = extractPrice(element, site.priceSelector);

                    if (name != null && url != null && price != null) {
                        // Construir URL completa se necessário
                        if (site.requiresBaseUrl && !url.startsWith("http")) {
                            url = site.baseUrl + url;
                        }

                        // Criar produto para monitoramento
                        ProductMonitor product = ProductMonitor.builder()
                                .productName(name)
                                .productUrl(url)
                                .targetPrice(targetPrice)
                                .currentPrice(price)
                                .siteName(site.name)
                                .category(category)
                                .isActive(true)
                                .notificationSent(false)
                                .build();

                        // Salvar produto no banco
                        ProductMonitor savedProduct = productService.createProduct(product);

                        // Criar preço do site com ID do produto salvo
                        SitePrice sitePrice = SitePrice.builder()
                                .siteName(site.name)
                                .price(price)
                                .productUrl(url)
                                .available(true)
                                .productId(savedProduct.getId())
                                .build();

                        prices.add(sitePrice);

                        // Limitar a 3 produtos por site
                        if (prices.size() >= 3) break;
                    }
                } catch (Exception e) {
                    logProcessingError(site.name, e.getMessage());
                }
            }

        } catch (Exception e) {
            logSiteError(site.name, e.getMessage());
        }

        return prices;
    }

    // FILTRO DE RELEVÂNCIA DE PRODUTOS
    private boolean isProductRelevant(String foundProductName, String searchedProductName, BigDecimal price) {
        String lowerFound = foundProductName.toLowerCase().trim();
        String lowerSearched = searchedProductName.toLowerCase().trim();
        
        // Filtrar nomes muito curtos ou genéricos
        if (lowerFound.length() < 5 || lowerFound.equals("deixe uma avaliação sobre o anúncio")) {
            log.debug("Produto descartado (nome genérico/curto): '{}'", foundProductName);
            return false;
        }
        
        // Extrair palavras-chave importantes do produto pesquisado
        String[] searchedWords = lowerSearched.split("\\s+");
        List<String> keywords = new ArrayList<>();
        for (String word : searchedWords) {
            // Ignorar palavras muito curtas ou comuns
            if (word.length() > 2 && !word.matches("para|com|de|da|do|em|no|na")) {
                keywords.add(word);
            }
        }
        
        // Verificar quantas palavras-chave aparecem no produto encontrado
        int matchingWords = 0;
        for (String keyword : keywords) {
            if (lowerFound.contains(keyword)) {
                matchingWords++;
            }
        }
        
        // Calcular percentual de correspondência
        double matchPercentage = keywords.isEmpty() ? 0 : (double) matchingWords / keywords.size();
        
        log.debug("Relevância: '{}' vs '{}' - {}/{} palavras ({:.0f}%)", 
                foundProductName, searchedProductName, matchingWords, keywords.size(), matchPercentage * 100);
        
        // Filtros específicos por categoria de produto
        
        // 1. CONSOLES (PlayStation, Xbox, Nintendo)
        if (lowerSearched.contains("playstation") || lowerSearched.contains("xbox") || lowerSearched.contains("nintendo")) {
            boolean isConsole = lowerFound.contains("console") || lowerFound.contains("playstation") || 
                              lowerFound.contains("xbox") || lowerFound.contains("nintendo") ||
                              lowerFound.contains("ps5") || lowerFound.contains("ps 5");
            
            boolean isAccessory = lowerFound.contains("controle") || lowerFound.contains("controller") ||
                                lowerFound.contains("jogo") || lowerFound.contains("game") ||
                                lowerFound.contains("ventilador") || lowerFound.contains("fan") ||
                                lowerFound.contains("capa") || lowerFound.contains("case") ||
                                lowerFound.contains("cabo") || lowerFound.contains("cable") ||
                                lowerFound.contains("película") || lowerFound.contains("screen protector");
            
            boolean validPrice = price.compareTo(new BigDecimal("2000")) >= 0;
            
            if (!isConsole || isAccessory || !validPrice) {
                log.debug("Console descartado - isConsole:{}, isAccessory:{}, validPrice:{}", isConsole, isAccessory, validPrice);
                return false;
            }
        }
        
        // 2. SMARTPHONES (iPhone, Samsung, Xiaomi, etc.)
        if (lowerSearched.contains("iphone") || lowerSearched.contains("smartphone") || lowerSearched.contains("celular")) {
            boolean isAccessory = lowerFound.contains("capa") || lowerFound.contains("case") ||
                                lowerFound.contains("película") || lowerFound.contains("screen protector") ||
                                lowerFound.contains("carregador") || lowerFound.contains("charger") ||
                                lowerFound.contains("fone") || lowerFound.contains("headphone") ||
                                lowerFound.contains("cabo") || lowerFound.contains("cable");
            
            boolean validPrice = price.compareTo(new BigDecimal("800")) >= 0;
            
            if (isAccessory || !validPrice) {
                log.debug("Smartphone descartado - isAccessory:{}, validPrice:{}", isAccessory, validPrice);
                return false;
            }
        }
        
        // 3. NOTEBOOKS/LAPTOPS
        if (lowerSearched.contains("notebook") || lowerSearched.contains("laptop")) {
            boolean isAccessory = lowerFound.contains("mouse") || lowerFound.contains("teclado") ||
                                lowerFound.contains("keyboard") || lowerFound.contains("capa") ||
                                lowerFound.contains("mochila") || lowerFound.contains("backpack");
            
            boolean validPrice = price.compareTo(new BigDecimal("1200")) >= 0;
            
            if (isAccessory || !validPrice) {
                log.debug("Notebook descartado - isAccessory:{}, validPrice:{}", isAccessory, validPrice);
                return false;
            }
        }
        
        // 4. TÊNIS/CALÇADOS
        if (lowerSearched.contains("tênis") || lowerSearched.contains("sapato") || lowerSearched.contains("bota") || 
            lowerSearched.contains("chinelo") || lowerSearched.contains("sandália")) {
            
            boolean isAccessory = lowerFound.contains("meia") || lowerFound.contains("sock") ||
                                lowerFound.contains("palmilha") || lowerFound.contains("insole") ||
                                lowerFound.contains("cadarço") || lowerFound.contains("lace");
            
            boolean validPrice = price.compareTo(new BigDecimal("50")) >= 0 && price.compareTo(new BigDecimal("5000")) <= 0;
            
            if (isAccessory || !validPrice) {
                log.debug("Calçado descartado - isAccessory:{}, validPrice:{}", isAccessory, validPrice);
                return false;
            }
        }
        
        // 5. ROUPAS (camiseta, calça, jaqueta, etc.)
        if (lowerSearched.contains("camiseta") || lowerSearched.contains("camisa") || lowerSearched.contains("calça") ||
            lowerSearched.contains("jaqueta") || lowerSearched.contains("short") || lowerSearched.contains("bermuda")) {
            
            boolean validPrice = price.compareTo(new BigDecimal("30")) >= 0 && price.compareTo(new BigDecimal("3000")) <= 0;
            
            if (!validPrice) {
                log.debug("Roupa descartada - validPrice:{}", validPrice);
                return false;
            }
        }
        
        // 6. LÓGICA GENÉRICA - Pelo menos 50% de palavras-chave devem coincidir
        if (matchPercentage < 0.5) {
            log.debug("Produto descartado (baixa correspondência): {:.0f}% < 50%", matchPercentage * 100);
            return false;
        }
        
        // Filtrar preços absurdamente baixos (provavelmente acessórios ou erros)
        if (price.compareTo(new BigDecimal("10")) < 0) {
            log.debug("Produto descartado (preço muito baixo): R$ {}", price);
            return false;
        }
        
        log.debug("Produto APROVADO: '{}' - R$ {}", foundProductName, price);
        return true;
    }

    // BUSCAR PREÇOS EM SITE ESPECÍFICO (SEM SALVAMENTO)
    private List<SitePrice> searchProductPricesInSite(String productName, SiteConfig site) {
        List<SitePrice> prices = new ArrayList<>();

        // Sites que SEMPRE usam Selenium (não tentam Jsoup primeiro)
        if (site.name.equals("NETSHOES")) {
            log.info("Site {} configurado para usar APENAS Selenium", site.name);
            
            try {
                String searchUrl = String.format(site.searchUrl, URLEncoder.encode(productName, StandardCharsets.UTF_8));
                List<ProductData> seleniumProducts = seleniumService.searchGenericSite(site.name, searchUrl, productName);
                
                log.info("Selenium encontrou {} produtos em {}", seleniumProducts.size(), site.name);
                
                // Converter para SitePrice
                for (ProductData product : seleniumProducts) {
                    prices.add(SitePrice.builder()
                            .siteName(site.name)
                            .price(product.price())
                            .productUrl(product.url())
                            .available(true)
                            .productName(product.name())
                            .build());
                    
                    // Limitar a 3 produtos por site
                    if (prices.size() >= 3) break;
                }
                
            } catch (Exception e) {
                log.error("Erro ao buscar com Selenium no site {}: {}", site.name, e.getMessage());
            }
            
            return prices;
        }

        // Para outros sites, tentar Jsoup primeiro
        try {
            String searchUrl = String.format(site.searchUrl, URLEncoder.encode(productName, StandardCharsets.UTF_8));
            log.info("Tentando buscar com Jsoup: {}", searchUrl);

            Document doc = Jsoup.connect(searchUrl)
                    .userAgent(USER_AGENTS[random.nextInt(USER_AGENTS.length)])
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                    .header("Accept-Language", "pt-BR,pt;q=0.9,en;q=0.8")
                    .timeout(15000)
                    .get();

            Elements productElements = doc.select(site.productSelector);
            log.info("Jsoup encontrou {} elementos de produto no site {}", productElements.size(), site.name);

            // Se não encontrou produtos com o seletor padrão, tentar Selenium
            if (productElements.isEmpty()) {
                log.info("Nenhum produto encontrado com Jsoup no site {}. Tentando Selenium...", site.name);
                
                try {
                    List<ProductData> seleniumProducts;
                    
                    // Usar método específico ou genérico
                    if (site.name.equals("KABUM")) {
                        seleniumProducts = seleniumService.searchKabum(productName);
                    } else if (site.name.equals("MERCADO_LIVRE")) {
                        seleniumProducts = seleniumService.searchMercadoLivre(productName);
                    } else {
                        seleniumProducts = seleniumService.searchGenericSite(site.name, searchUrl, productName);
                    }
                    
                    log.info("Selenium encontrou {} produtos em {}", seleniumProducts.size(), site.name);
                    
                    // Converter para SitePrice
                    for (ProductData product : seleniumProducts) {
                        prices.add(SitePrice.builder()
                                .siteName(site.name)
                                .price(product.price())
                                .productUrl(product.url())
                                .available(true)
                                .productName(product.name())
                                .build());
                        
                        // Limitar a 3 produtos por site
                        if (prices.size() >= 3) break;
                    }
                    
                    return prices;
                    
                } catch (Exception e) {
                    log.warn("Erro com Selenium em {}: {}", site.name, e.getMessage());
                    return new ArrayList<>(); // Retornar lista vazia
                }
            }

            int processedCount = 0;
            for (Element element : productElements) {
                try {
                    processedCount++;
                    String name = extractText(element, site.nameSelector);
                    String url = extractUrl(element, site.urlSelector);
                    BigDecimal price = extractPrice(element, site.priceSelector);

                    log.debug("[{}] Produto {}: name='{}', price={}, url='{}'", 
                            site.name, processedCount, name, price, url != null ? url.substring(0, Math.min(50, url.length())) + "..." : "null");

                    if (name != null && url != null && price != null) {
                        // FILTRO DE RELEVÂNCIA - Verificar se o produto é realmente relevante
                        if (isProductRelevant(name, productName, price)) {
                            // Construir URL completa se necessário
                            if (site.requiresBaseUrl && !url.startsWith("http")) {
                            url = site.baseUrl + url;
                        }

                        // Criar preço do site
                        SitePrice sitePrice = SitePrice.builder()
                                .siteName(site.name)
                                .price(price)
                                .productUrl(url)
                                .available(true)
                                .productName(name) // Nome do produto específico encontrado
                                .build();

                        prices.add(sitePrice);
                            log.info("  ✓ Produto relevante adicionado: {} - R$ {}", name, price);

                            // Limitar a 3 produtos por site
                            if (prices.size() >= 3) {
                                log.info("Limite de 3 produtos atingido para {}", site.name);
                                break;
                            }
                        } else {
                            log.debug("  ✗ Produto irrelevante filtrado: {} - R$ {}", name, price);
                        }
                    } else {
                        log.debug("  ✗ Produto descartado (campos nulos): name={}, price={}, url={}", 
                                name != null ? "OK" : "NULL", 
                                price != null ? "OK" : "NULL", 
                                url != null ? "OK" : "NULL");
                    }
                } catch (Exception e) {
                    logProcessingError(site.name, e.getMessage());
                }
            }
            
            log.info("Jsoup processou {} elementos, encontrou {} produtos relevantes no site {}", 
                    processedCount, prices.size(), site.name);

        } catch (Exception e) {
            logSiteError(site.name, e.getMessage());
            
            // Se for erro 403 (bloqueio anti-bot), timeout ou IOException, tentar Selenium
            if (e.getMessage() != null && (e.getMessage().contains("403") || e.getMessage().contains("Status=403") || 
                e.getMessage().contains("timed out") || e.getMessage().contains("Read timed out"))) {
                
                String errorType = e.getMessage().contains("403") ? "403 (anti-bot)" : "timeout";
                log.info("Erro {} detectado em {}. Tentando Selenium...", errorType, site.name);
                
                try {
                    String searchUrl = String.format(site.searchUrl, URLEncoder.encode(productName, StandardCharsets.UTF_8));
                    List<ProductData> seleniumProducts;
                    
                    // Usar método específico ou genérico
                    if (site.name.equals("KABUM")) {
                        seleniumProducts = seleniumService.searchKabum(productName);
                    } else if (site.name.equals("MERCADO_LIVRE")) {
                        seleniumProducts = seleniumService.searchMercadoLivre(productName);
                    } else {
                        seleniumProducts = seleniumService.searchGenericSite(site.name, searchUrl, productName);
                    }
                    
                    log.info("Selenium encontrou {} produtos em {}", seleniumProducts.size(), site.name);
                    
                    // Converter para SitePrice
                    for (ProductData product : seleniumProducts) {
                        prices.add(SitePrice.builder()
                                .siteName(site.name)
                                .price(product.price())
                                .productUrl(product.url())
                                .available(true)
                                .productName(product.name())
                                .build());
                        
                        // Limitar a 3 produtos por site
                        if (prices.size() >= 3) break;
                    }
                    
                } catch (Exception seleniumError) {
                    log.warn("Erro com Selenium em {}: {}", site.name, seleniumError.getMessage());
                }
            }
        }

        return prices;
    }


    // MÉTODOS AUXILIARES PARA LOGS
    private void logSiteError(String siteName, String errorMessage) {
        log.error("Erro ao buscar no site {}: {}", siteName, errorMessage);
    }

    private void logCategoryDetected(ProductCategory category) {
        log.info("Categoria detectada: {}", category.getDisplayName());
    }

    private void logRelevantSites(ProductCategory category, List<String> relevantSites) {
        log.info("Sites relevantes para {}: {}", category.getDisplayName(), relevantSites);
    }

    private void logSmartMonitoringStart(String productName) {
        log.info("Iniciando monitoramento inteligente para: {}", productName);
    }

    private void logPriceComparisonStart(String productName) {
        log.info("Iniciando comparação de preços para: {}", productName);
    }

    private void logProcessingError(String siteName, String errorMessage) {
        log.debug("Erro ao processar produto no site {}: {}", siteName, errorMessage);
    }

    private void logPriceExtractionError(String url, String errorMessage) {
        log.error("Erro ao extrair preço da URL {}: {}", url, errorMessage);
    }

    private String extractText(Element element, String selector) {
        try {
            Element found = element.selectFirst(selector);
            return found != null ? found.text().trim() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String extractUrl(Element element, String selector) {
        try {
            Element link = element.selectFirst(selector);
            return link != null ? link.attr("href") : null;
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal extractPrice(Element element, String selector) {
        try {
            Element priceElement = element.selectFirst(selector);
            if (priceElement != null) {
                String priceText = priceElement.text();
                return parseBrazilianPrice(priceText);
            }
        } catch (Exception e) {
            log.debug("Erro ao extrair preço: {}", e.getMessage());
        }
        return null;
    }

    // DEBUG DE BUSCA EM SITES ESPECÍFICOS
    public String debugSiteSearch(String productName, String siteName) {
        StringBuilder debug = new StringBuilder();
        
        debug.append("=== DEBUG BUSCA EM SITES ===\n");
        debug.append("Produto: ").append(productName).append("\n");
        debug.append("Site: ").append(siteName).append("\n\n");
        
        // Encontrar o site configurado
        SiteConfig siteConfig = null;
        for (SiteConfig config : SITES) {
            if (config.name.equalsIgnoreCase(siteName)) {
                siteConfig = config;
                break;
            }
        }
        
        if (siteConfig == null) {
            debug.append(" Site não encontrado na configuração!\n");
            debug.append("Sites disponíveis: ");
            for (SiteConfig config : SITES) {
                debug.append(config.name).append(", ");
            }
            return debug.toString();
        }
        
        debug.append(" Site encontrado: ").append(siteConfig.name).append("\n");
        debug.append("URL base: ").append(siteConfig.baseUrl).append("\n");
        debug.append("Seletor de preço: ").append(siteConfig.priceSelector).append("\n\n");
        
        try {
            // Construir URL de busca usando o padrão de busca do site
            String searchUrl = String.format(siteConfig.searchUrl, productName.replaceAll("\\s+", "+"));
            debug.append("URL de busca: ").append(searchUrl).append("\n\n");
            
            // Conectar e extrair dados com headers melhorados
            Document doc = Jsoup.connect(searchUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                    .header("Accept-Language", "pt-BR,pt;q=0.9,en;q=0.8")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("DNT", "1")
                    .header("Connection", "keep-alive")
                    .header("Upgrade-Insecure-Requests", "1")
                    .header("Sec-Fetch-Dest", "document")
                    .header("Sec-Fetch-Mode", "navigate")
                    .header("Sec-Fetch-Site", "none")
                    .header("Cache-Control", "max-age=0")
                    .header("Referer", "https://www.mercadolivre.com.br/")
                    .timeout(25000)
                    .followRedirects(true)
                    .maxBodySize(0)
                    .get();
            
            debug.append(" Conexão bem-sucedida!\n");
            debug.append("Título da página: ").append(doc.title()).append("\n");
            debug.append("URL final: ").append(doc.baseUri()).append("\n\n");
            
            // Verificar se foi redirecionado para página promocional
            if (doc.title().contains("Presente de Dia das Crianças") || 
                doc.title().contains("Promoção") || 
                doc.title().contains("Oferta")) {
                debug.append("⚠️ ATENÇÃO: Página redirecionada para promoção!\n\n");
            }
            
            // Buscar por elementos de produtos
            Elements productElements = doc.select(siteConfig.productSelector);
            debug.append("Elementos de produtos encontrados: ").append(productElements.size()).append("\n");
            
            // Extrair dados dos produtos encontrados
            List<SitePrice> realProducts = new ArrayList<>();
            for (int i = 0; i < Math.min(10, productElements.size()); i++) {
                Element product = productElements.get(i);
                
                // Extrair nome
                String name = "NULO";
                Elements nameElements = product.select(siteConfig.nameSelector);
                if (!nameElements.isEmpty()) {
                    String rawName = nameElements.first().text().trim();
                    // Filtrar textos indesejados
                    if (!rawName.isEmpty() && 
                        !rawName.contains("Deixe uma avaliação") &&
                        !rawName.contains("Parcele em") &&
                        !rawName.contains("Ver mais") &&
                        !rawName.contains("Comprar") &&
                        rawName.length() > 10) {
                        name = rawName;
                    }
                }
                
                // Extrair preço
                BigDecimal price = null;
                Elements priceElements = product.select(siteConfig.priceSelector);
                if (!priceElements.isEmpty()) {
                    String priceText = priceElements.first().text();
                    price = parseBrazilianPrice(priceText);
                }
                
                // Extrair URL
                String url = "NULO";
                Elements urlElements = product.select(siteConfig.urlSelector);
                if (!urlElements.isEmpty()) {
                    String href = urlElements.first().attr("href");
                    if (!href.isEmpty()) {
                        url = href.startsWith("http") ? href : siteConfig.baseUrl + href;
                    }
                }
                
                if (price != null && price.compareTo(new BigDecimal("100")) > 0) {
                    SitePrice sitePrice = SitePrice.builder()
                            .siteName(siteConfig.name)
                            .price(price)
                            .productUrl(url)
                            .available(true)
                            .build();
                    realProducts.add(sitePrice);
                    
                    debug.append("Produto ").append(realProducts.size()).append(":\n");
                    debug.append("  Nome: '").append(name).append("'\n");
                    debug.append("  URL: '").append(url).append("'\n");
                    debug.append("  Preço: R$ ").append(price).append("\n\n");
                }
            }
            
            // Estratégia especial para Netshoes (sempre usa Selenium)
            if (siteConfig.name.equals("NETSHOES")) {
                debug.append("\n=== ESTRATÉGIA NETSHOES (APENAS SELENIUM) ===\n");
                debug.append("Netshoes configurada para usar APENAS Selenium...\n");
                
                try {
                    List<ProductData> seleniumProducts = seleniumService.searchGenericSite(siteConfig.name, searchUrl, productName);
                    debug.append(" Selenium encontrou ").append(seleniumProducts.size()).append(" produtos!\n");
                    
                    if (!seleniumProducts.isEmpty()) {
                        debug.append("\n=== PRODUTOS ENCONTRADOS COM SELENIUM ===\n");
                        for (int i = 0; i < seleniumProducts.size(); i++) {
                            ProductData product = seleniumProducts.get(i);
                            debug.append("Produto ").append(i + 1).append(":\n");
                            debug.append("  Nome: '").append(product.name()).append("'\n");
                            debug.append("  Preço: R$ ").append(product.price()).append("\n");
                            debug.append("  URL: '").append(product.url()).append("'\n\n");
                            
                            // Adicionar aos produtos reais
                            realProducts.add(SitePrice.builder()
                                    .siteName("NETSHOES")
                                    .price(product.price())
                                    .productUrl(product.url())
                                    .available(true)
                                    .productName(product.name())
                                    .build());
                        }
                    }
                } catch (Exception e) {
                    debug.append(" Erro com Selenium: ").append(e.getMessage()).append("\n");
                }
                
                debug.append("\n");
                return debug.toString(); // Retornar aqui para evitar tentar Jsoup
            }
            
            // Estratégia especial para Mercado Livre (conteúdo dinâmico)
            if (siteConfig.name.equals("MERCADO_LIVRE") && productElements.size() == 0) {
                debug.append("\n=== ESTRATÉGIA ESPECIAL MERCADO LIVRE ===\n");
                debug.append("Mercado Livre usa JavaScript - usando Selenium...\n");
                
                try {
                    List<ProductData> seleniumProducts = seleniumService.searchMercadoLivre(productName);
                    debug.append(" Selenium encontrou ").append(seleniumProducts.size()).append(" produtos!\n");
                    
                    if (!seleniumProducts.isEmpty()) {
                        debug.append("\n=== PRODUTOS ENCONTRADOS COM SELENIUM ===\n");
                        for (int i = 0; i < seleniumProducts.size(); i++) {
                            ProductData product = seleniumProducts.get(i);
                            debug.append("Produto ").append(i + 1).append(":\n");
                            debug.append("  Nome: '").append(product.name()).append("'\n");
                            debug.append("  Preço: R$ ").append(product.price()).append("\n");
                            debug.append("  URL: '").append(product.url()).append("'\n\n");
                            
                            // Adicionar aos produtos reais
                            realProducts.add(SitePrice.builder()
                                    .siteName("MERCADO_LIVRE")
                                    .price(product.price())
                                    .productUrl(product.url())
                                    .available(true)
                                    .productName(product.name()) // Nome do produto do Selenium
                                    .build());
                        }
                    }
                } catch (Exception e) {
                    debug.append(" Erro com Selenium: ").append(e.getMessage()).append("\n");
                    debug.append("⚠️ Mercado Livre requer JavaScript para carregar produtos.\n");
                }
                
                debug.append("\n");
            }
            
            // ESTRATÉGIA AUTOMÁTICA: Usar Selenium quando Jsoup não encontrar produtos
            if (productElements.size() == 0) {
                debug.append("\n=== ESTRATÉGIA AUTOMÁTICA SELENIUM ===\n");
                debug.append(siteConfig.name).append(" pode usar JavaScript - tentando Selenium...\n");
                
                try {
                    List<ProductData> seleniumProducts;
                    
                    // Usar método específico para sites conhecidos ou genérico para outros
                    if (siteConfig.name.equals("KABUM")) {
                        seleniumProducts = seleniumService.searchKabum(productName);
                    } else if (siteConfig.name.equals("MERCADO_LIVRE")) {
                        seleniumProducts = seleniumService.searchMercadoLivre(productName);
                    } else {
                        // Método genérico para qualquer outro site
                        String seleniumSearchUrl = String.format(siteConfig.searchUrl, productName.replace(" ", "+"));
                        seleniumProducts = seleniumService.searchGenericSite(siteConfig.name, seleniumSearchUrl, productName);
                    }
                    
                    debug.append(" Selenium encontrou ").append(seleniumProducts.size()).append(" produtos!\n");
                    
                    if (!seleniumProducts.isEmpty()) {
                        debug.append("\n=== PRODUTOS ENCONTRADOS COM SELENIUM ===\n");
                        for (int i = 0; i < seleniumProducts.size(); i++) {
                            ProductData product = seleniumProducts.get(i);
                            debug.append("Produto ").append(i + 1).append(":\n");
                            debug.append("  Nome: '").append(product.name()).append("'\n");
                            debug.append("  Preço: R$ ").append(product.price()).append("\n");
                            debug.append("  URL: '").append(product.url()).append("'\n\n");
                            
                            // Adicionar aos produtos reais
                            realProducts.add(SitePrice.builder()
                                    .siteName(siteConfig.name)
                                    .price(product.price())
                                    .productUrl(product.url())
                                    .available(true)
                                    .productName(product.name())
                                    .build());
                        }
                    }
                } catch (Exception e) {
                    debug.append(" Erro com Selenium: ").append(e.getMessage()).append("\n");
                    debug.append("⚠️ ").append(siteConfig.name).append(" pode requerer ajustes de seletores.\n");
                }
                
                debug.append("\n");
            }
            
            // Debug de seletores alternativos para Mercado Livre
            if (siteConfig.name.equals("MERCADO_LIVRE")) {
                debug.append("\n=== TESTANDO SELETORES ALTERNATIVOS ===\n");
                String[] alternativeSelectors = {
                    ".ui-search-results .ui-search-item",
                    ".shops__item", 
                    ".item",
                    "[data-testid='product-item']",
                    ".ui-search-item",
                    ".search-results .ui-search-item"
                };
                
                for (String selector : alternativeSelectors) {
                    Elements elements = doc.select(selector);
                    debug.append("Seletor '").append(selector).append("': ").append(elements.size()).append(" elementos\n");
                }
            }
            
            // Buscar por elementos de preço
            Elements priceElements = doc.select(siteConfig.priceSelector);
            debug.append("Elementos de preço encontrados: ").append(priceElements.size()).append("\n");
            
            // Testar extração de dados dos produtos
            for (int i = 0; i < Math.min(3, productElements.size()); i++) {
                Element productElem = productElements.get(i);
                String name = extractText(productElem, siteConfig.nameSelector);
                String url = extractUrl(productElem, siteConfig.urlSelector);
                BigDecimal price = extractPrice(productElem, siteConfig.priceSelector);
                
                debug.append("Produto ").append(i + 1).append(":\n");
                debug.append("  Nome: '").append(name != null ? name : "NULO").append("'\n");
                debug.append("  URL: '").append(url != null ? url : "NULO").append("'\n");
                debug.append("  Preço: ").append(price != null ? "R$ " + price : "NULO").append("\n\n");
            }
            
            // Buscar por links de produtos específicos do Mercado Livre
            Elements productLinks = doc.select("a[href*='MLB-'], a[href*='/dp/'], a[href*='produto']");
            debug.append("\nLinks de produtos encontrados: ").append(productLinks.size()).append("\n");
            
            // Testar extração de dados dos links encontrados
            for (int i = 0; i < Math.min(3, productLinks.size()); i++) {
                Element link = productLinks.get(i);
                String href = link.attr("href");
                String text = link.text();
                
                debug.append("Link ").append(i + 1).append(": ")
                     .append("Texto='").append(text.substring(0, Math.min(50, text.length()))).append("'")
                     .append(" → ").append(href).append("\n");
                
                // Tentar extrair preço do elemento pai
                Element parent = link.parent();
                while (parent != null && parent.select(siteConfig.priceSelector).isEmpty()) {
                    parent = parent.parent();
                }
                
                if (parent != null) {
                    Elements priceElems = parent.select(siteConfig.priceSelector);
                    if (!priceElems.isEmpty()) {
                        String priceText = priceElems.first().text();
                        BigDecimal price = parseBrazilianPrice(priceText);
                        debug.append("  → Preço encontrado: ").append(priceText).append(" → R$ ").append(price).append("\n");
                    }
                }
            }
            
            // Estratégia alternativa: buscar por elementos que contenham preços e tentar encontrar produtos próximos
            debug.append("\n=== ESTRATÉGIA ALTERNATIVA ===\n");
            Elements allPriceElements = doc.select(".andes-money-amount, .price-tag, [class*='price']");
            debug.append("Todos os elementos com preços: ").append(allPriceElements.size()).append("\n");
            
            for (int i = 0; i < Math.min(5, allPriceElements.size()); i++) {
                Element priceElem = allPriceElements.get(i);
                String priceText = priceElem.text();
                BigDecimal price = parseBrazilianPrice(priceText);
                
                if (price != null && price.compareTo(new BigDecimal("1000")) > 0) { // Preços acima de R$ 1000
                    debug.append("Preço ").append(i + 1).append(": ").append(priceText).append(" → R$ ").append(price).append("\n");
                    
                    // Buscar produto próximo
                    Element productParent = priceElem.parent();
                    int levels = 0;
                    while (productParent != null && levels < 5) {
                        Elements productLinksNearby = productParent.select("a[href*='MLB-'], a[href*='/dp/']");
                        if (!productLinksNearby.isEmpty()) {
                            String productUrl = productLinksNearby.first().attr("href");
                            String productTitle = productLinksNearby.first().text();
                            debug.append("  → Produto próximo: '").append(productTitle.substring(0, Math.min(30, productTitle.length()))).append("' → ").append(productUrl).append("\n");
                            break;
                        }
                        productParent = productParent.parent();
                        levels++;
                    }
                }
            }
            
            // Estratégia final: buscar por links específicos do Mercado Livre na página
            debug.append("\n=== ESTRATÉGIA FINAL ===\n");
            Elements mlbLinks = doc.select("a[href*='MLB-']");
            debug.append("Links MLB encontrados: ").append(mlbLinks.size()).append("\n");
            
            // Buscar por elementos que contenham palavras-chave do produto
            String[] keywords = productName.toLowerCase().split("\\s+");
            String firstKeyword = "";
            for (String word : keywords) {
                if (word.length() > 3) {
                    firstKeyword = word;
                    break;
                }
            }
            
            Elements keywordElements = doc.select("*:containsOwn(" + firstKeyword + ")");
            debug.append("Elementos contendo '").append(firstKeyword).append("': ").append(keywordElements.size()).append("\n");
            
            // Usar produtos reais encontrados ou criar simulados se necessário
            debug.append("\n=== PRODUTOS FINAIS ===\n");
            
            if (!realProducts.isEmpty()) {
                debug.append(" Usando ").append(realProducts.size()).append(" produtos reais encontrados!\n");
                debug.append("\n=== PRODUTOS REAIS ENCONTRADOS ===\n");
                for (SitePrice product : realProducts) {
                    debug.append("• ").append(product.getSiteName()).append(": R$ ").append(product.getPrice()).append(" - ").append(product.getProductUrl()).append("\n");
                }
            } else {
                debug.append("⚠️ Nenhum produto real encontrado, criando produtos simulados...\n");
                
                // Lista de preços válidos encontrados
                List<BigDecimal> validPrices = new ArrayList<>();
                for (int i = 0; i < Math.min(10, allPriceElements.size()); i++) {
                    Element priceElem = allPriceElements.get(i);
                    String priceText = priceElem.text();
                    BigDecimal price = parseBrazilianPrice(priceText);
                    
                    if (price != null && price.compareTo(new BigDecimal("1000")) > 0 && price.compareTo(new BigDecimal("10000")) < 0) {
                        validPrices.add(price);
                    }
                }
            
                debug.append("Preços válidos encontrados: ").append(validPrices.size()).append("\n");
                for (BigDecimal price : validPrices) {
                    debug.append("  → R$ ").append(price).append("\n");
                }
                
                // Criar produtos simulados
                debug.append("\n=== PRODUTOS SIMULADOS CRIADOS ===\n");
                List<SitePrice> simulatedProducts = new ArrayList<>();
                for (int i = 0; i < Math.min(3, validPrices.size()); i++) {
                    BigDecimal price = validPrices.get(i);
                    String simulatedName = productName + " - " + siteConfig.name;
                    String productUrl = String.format(siteConfig.searchUrl, URLEncoder.encode(productName, StandardCharsets.UTF_8));
                    
                    debug.append("Produto ").append(i + 1).append(":\n");
                    debug.append("  Nome: '").append(simulatedName).append("'\n");
                    debug.append("  URL: '").append(productUrl).append("'\n");
                    debug.append("  Preço: R$ ").append(price).append("\n");
                    debug.append("  Status:  PRODUTO VÁLIDO PARA MONITORAMENTO\n\n");
                    
                    simulatedProducts.add(SitePrice.builder()
                            .siteName(siteConfig.name)
                            .price(price)
                            .productUrl(productUrl)
                            .available(true)
                            .productName(productName + " - " + siteConfig.name) // Nome do produto com site
                            .build());
                }
            }
            
        } catch (Exception e) {
            debug.append(" Erro: ").append(e.getMessage()).append("\n");
        }
        
        return debug.toString();
    }

    // TESTE SELENIUM PARA MERCADO LIVRE
    public String testSeleniumMercadoLivre(String productName) {
        StringBuilder result = new StringBuilder();
        result.append("=== TESTE SELENIUM MERCADO LIVRE ===\n");
        result.append("Produto: ").append(productName).append("\n\n");
        
        try {
            List<ProductData> products = seleniumService.searchMercadoLivre(productName);
            
            result.append(" Selenium executado com sucesso!\n");
            result.append("Produtos encontrados: ").append(products.size()).append("\n\n");
            
            if (!products.isEmpty()) {
                result.append("=== PRODUTOS ENCONTRADOS ===\n");
                for (int i = 0; i < products.size(); i++) {
                    ProductData product = products.get(i);
                    result.append("Produto ").append(i + 1).append(":\n");
                    result.append("  Nome: ").append(product.name()).append("\n");
                    result.append("  Preço: R$ ").append(product.price()).append("\n");
                    result.append("  URL: ").append(product.url()).append("\n\n");
                }
            } else {
                result.append(" Nenhum produto encontrado.\n");
            }
            
        } catch (Exception e) {
            result.append(" Erro: ").append(e.getMessage()).append("\n");
        } finally {
            seleniumService.closeDriver();
        }
        
        return result.toString();
    }

    // CRIAR PRODUTOS SIMULADOS QUANDO NÃO CONSEGUE EXTRAIR PRODUTOS INDIVIDUAIS
    private List<SitePrice> createSimulatedProducts(Document doc, String productName, SiteConfig site) {
        List<SitePrice> prices = new ArrayList<>();
        
        try {
            Elements priceElements = doc.select(site.priceSelector);
            java.util.Set<BigDecimal> uniquePrices = extractValidPrices(priceElements, productName);
            
            // Criar produtos simulados para cada preço único
            for (BigDecimal price : uniquePrices) {
                if (prices.size() >= 3) break; // Limitar a 3 produtos
                
                String simulatedUrl = String.format(site.searchUrl, URLEncoder.encode(productName, StandardCharsets.UTF_8));
                SitePrice sitePrice = buildSimulatedSitePrice(site, price, simulatedUrl, productName);
                prices.add(sitePrice);
            }
            
            log.info("Criados {} produtos simulados para o site {}", prices.size(), site.name);
            
        } catch (Exception e) {
            log.debug("Erro ao criar produtos simulados: {}", e.getMessage());
        }
        
        return prices;
    }
    
    private java.util.Set<BigDecimal> extractValidPrices(Elements priceElements, String productName) {
        java.util.Set<BigDecimal> uniquePrices = new java.util.HashSet<>();
        
        for (Element priceElem : priceElements) {
            BigDecimal price = parseBrazilianPrice(priceElem.text());
            
            if (isValidPriceRange(price) && isPriceRelatedToProduct(priceElem, productName)) {
                uniquePrices.add(price);
            }
        }
        
        return uniquePrices;
    }
    
    private boolean isValidPriceRange(BigDecimal price) {
        if (price == null) return false;
        return price.compareTo(new BigDecimal("1000")) > 0 && 
               price.compareTo(new BigDecimal("10000")) < 0;
    }
    
    private boolean isPriceRelatedToProduct(Element priceElem, String productName) {
        Element parent = priceElem.parent();
        int levels = 0;
        
        while (parent != null && levels < 5) {
            if (parentContainsProductKeywords(parent, productName)) {
                return true;
            }
            parent = parent.parent();
            levels++;
        }
        
        return false;
    }
    
    private boolean parentContainsProductKeywords(Element parent, String productName) {
        String parentText = parent.text().toLowerCase();
        String[] keywords = productName.toLowerCase().split("\\s+");
        
        long matchedKeywords = java.util.Arrays.stream(keywords)
                .filter(keyword -> keyword.length() > 3)
                .filter(parentText::contains)
                .count();
        
        return matchedKeywords >= (keywords.length * 0.5);
    }
    
    private SitePrice buildSimulatedSitePrice(SiteConfig site, BigDecimal price, String url, String productName) {
        return SitePrice.builder()
                .siteName(site.name)
                .price(price)
                .productUrl(url)
                .available(true)
                .productName(productName + " - " + site.name)
                .build();
    }

    // DEBUG DE BUSCA EM TODOS OS SITES
    public String debugAllSites(String productName) {
        StringBuilder debug = new StringBuilder();
        
        // Remover aspas extras se existirem
        String cleanProductName = productName.trim();
        if (cleanProductName.startsWith("\"") && cleanProductName.endsWith("\"")) {
            cleanProductName = cleanProductName.substring(1, cleanProductName.length() - 1);
        }
        
        debug.append("=== DEBUG BUSCA EM TODOS OS SITES ===\n");
        debug.append("Produto: ").append(cleanProductName).append("\n\n");
        
        // Detectar categoria
        ProductCategory category = categoryService.detectCategory(cleanProductName, "");
        debug.append("Categoria detectada: ").append(category).append("\n");
        
        // Obter sites relevantes
        List<String> relevantSites = categoryService.getRelevantSites(category);
        debug.append("Sites relevantes: ").append(relevantSites).append("\n\n");
        
        // Testar cada site
        for (String siteName : relevantSites) {
            debug.append("=== TESTANDO SITE: ").append(siteName).append(" ===\n");
            
            try {
                // Encontrar configuração do site
                SiteConfig siteConfig = null;
                for (SiteConfig config : SITES) {
                    if (config.name.equals(siteName)) {
                        siteConfig = config;
                        break;
                    }
                }
                
                if (siteConfig == null) {
                    debug.append(" Site não configurado!\n\n");
                    continue;
                }
                
                // Construir URL de busca
                String searchUrl = String.format(siteConfig.searchUrl, URLEncoder.encode(cleanProductName, StandardCharsets.UTF_8));
                debug.append("URL: ").append(searchUrl).append("\n");
                
                // Conectar com headers mais realistas
                Document doc = Jsoup.connect(searchUrl)
                        .userAgent(USER_AGENTS[random.nextInt(USER_AGENTS.length)])
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                        .header("Accept-Language", "pt-BR,pt;q=0.9,en;q=0.8")
                        .header("Accept-Encoding", "gzip, deflate, br")
                        .header("DNT", "1")
                        .header("Connection", "keep-alive")
                        .header("Upgrade-Insecure-Requests", "1")
                        .header("Sec-Fetch-Dest", "document")
                        .header("Sec-Fetch-Mode", "navigate")
                        .header("Sec-Fetch-Site", "none")
                        .header("Cache-Control", "max-age=0")
                        .timeout(15000)
                        .followRedirects(true)
                        .get();
                
                debug.append(" Conexão: OK\n");
                
                // Testar seletores
                Elements productElements = doc.select(siteConfig.productSelector);
                Elements priceElements = doc.select(siteConfig.priceSelector);
                
                debug.append("Produtos encontrados: ").append(productElements.size()).append("\n");
                debug.append("Preços encontrados: ").append(priceElements.size()).append("\n");
                
                // Debug de seletores de preço se não encontrou nenhum
                if (priceElements.isEmpty()) {
                    debug.append(" Testando seletores alternativos de preço...\n");
                    
                    String[] alternativePriceSelectors = {
                        ".price", ".preco", ".valor", ".cost", ".amount",
                        "[class*='price']", "[class*='preco']", "[class*='valor']",
                        ".price-current", ".price-value", ".price-tag",
                        ".andes-money-amount", ".priceCard", ".oldPriceCard",
                        // Seletores específicos do Kabum (atualizados 2024)
                        "span[class*='sc-']", "div[class*='price']", "span[class*='price']",
                        ".finalPrice", ".preco", ".sc-dcJsrY", ".sc-iBYQkv",
                        "[data-testid*='price']", "[data-testid*='valor']",
                        ".priceContainer", ".price-wrapper", ".price-box",
                        "article[data-id] span", "div[data-product-id] span"
                    };
                    
                    for (String selector : alternativePriceSelectors) {
                        Elements elements = doc.select(selector);
                        if (!elements.isEmpty()) {
                            debug.append("  Seletor '").append(selector).append("': ").append(elements.size()).append(" elementos\n");
                            
                            // Mostrar alguns exemplos
                            for (int i = 0; i < Math.min(3, elements.size()); i++) {
                                String text = elements.get(i).text();
                                BigDecimal price = parseBrazilianPrice(text);
                                debug.append("    → '").append(text).append("' → ").append(price != null ? "R$ " + price : "NULO").append("\n");
                            }
                        }
                    }
                    
                    // Debug específico: buscar por elementos que contenham "R$"
                    debug.append("\n Buscando elementos com 'R$'...\n");
                    Elements elementsWithRS = doc.select("*:contains(R$)");
                    debug.append("Elementos contendo 'R$': ").append(elementsWithRS.size()).append("\n");
                    
                    for (int i = 0; i < Math.min(5, elementsWithRS.size()); i++) {
                        Element elem = elementsWithRS.get(i);
                        String text = elem.text();
                        if (text.contains("R$") && text.length() < 50) { // Evitar elementos muito grandes
                            BigDecimal price = parseBrazilianPrice(text);
                            debug.append("  → '").append(text).append("' → ").append(price != null ? "R$ " + price : "NULO").append("\n");
                        }
                    }
                }
                
                // Testar estratégia alternativa
                if (productElements.isEmpty()) {
                    debug.append(" Usando estratégia alternativa...\n");
                    List<SitePrice> simulatedProducts = createSimulatedProducts(doc, cleanProductName, siteConfig);
                    debug.append("Produtos simulados: ").append(simulatedProducts.size()).append("\n");
                    
                    for (SitePrice product : simulatedProducts) {
                        debug.append("  → R$ ").append(product.getPrice()).append("\n");
                    }
                }
                
            } catch (Exception e) {
                debug.append(" Erro: ").append(e.getMessage()).append("\n");
            }
            
            debug.append("\n");
        }
        
        return debug.toString();
    }
}

