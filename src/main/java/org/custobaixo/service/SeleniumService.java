package org.custobaixo.service;

import lombok.extern.slf4j.Slf4j;
import org.custobaixo.model.ProductData;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class SeleniumService {

    private WebDriver driver;

    public void initDriver() {
        if (driver == null) {
            log.info("Inicializando WebDriver do Selenium...");
            WebDriverManager.chromedriver().setup();
            
            ChromeOptions options = createChromeOptions();
            driver = new ChromeDriver(options);
            
            // ANTI-DETECÇÃO: Executar script para remover navigator.webdriver
            try {
                JavascriptExecutor js = (JavascriptExecutor) driver;
                js.executeScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");
                log.info("WebDriver oculto com sucesso!");
            } catch (Exception e) {
                log.warn("Não foi possível ocultar webdriver: {}", e.getMessage());
            }
            
            log.info("WebDriver inicializado com sucesso!");
        }
    }

    private ChromeOptions createChromeOptions() {
        ChromeOptions options = new ChromeOptions();
        
        // ANTI-DETECÇÃO: Configurações básicas
        options.addArguments("--headless=new"); // Headless mais moderno
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--start-maximized");
        
        // ANTI-DETECÇÃO: Desabilitar recursos de automação
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--disable-infobars");
        options.addArguments("--disable-notifications");
        
        // ANTI-DETECÇÃO: Fingir ser um navegador real
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-plugins-discovery");
        options.addArguments("--disable-web-security");
        options.addArguments("--allow-running-insecure-content");
        
        // ANTI-DETECÇÃO: User-Agent realista (Chrome mais recente)
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36");
        
        // ANTI-DETECÇÃO: Remover propriedades que indicam automação
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation", "enable-logging"});
        options.setExperimentalOption("useAutomationExtension", false);
        
        // ANTI-DETECÇÃO: Adicionar preferências realistas
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        prefs.put("profile.default_content_setting_values.notifications", 2);
        prefs.put("profile.default_content_settings.popups", 0);
        options.setExperimentalOption("prefs", prefs);
        
        return options;
    }

    public void closeDriver() {
        if (driver != null) {
            log.info("Fechando WebDriver...");
            driver.quit();
            driver = null;
        }
    }

    public List<ProductData> searchKabum(String productName) {
        List<ProductData> products = new ArrayList<>();
        
        try {
            initDriver();
            
            log.info("Acessando página principal do Kabum...");
            driver.get("https://www.kabum.com.br");
            Thread.sleep(2000);
            
            String searchUrl = "https://www.kabum.com.br/busca/" + productName.replaceAll("\\s+", "-");
            log.info("Acessando busca no Kabum: {}", searchUrl);
            
            driver.get(searchUrl);
            Thread.sleep(3000);
            
            log.info("Título da página: '{}'", driver.getTitle());
            log.info("URL atual: '{}'", driver.getCurrentUrl());
            
            // Aguardar produtos carregarem
            try {
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("article[data-id], .productCard, main")));
                log.info("Elementos carregados!");
            } catch (Exception e) {
                log.warn("Timeout aguardando produtos: {}", e.getMessage());
            }
            
            // Tentar múltiplos seletores de produtos
            String[] productSelectors = {
                "article[data-id]",
                ".productCard",
                "div[data-product-id]",
                "a[href*='/produto/']"
            };
            
            List<WebElement> productElements = new ArrayList<>();
            for (String selector : productSelectors) {
                try {
                    List<WebElement> elements = driver.findElements(By.cssSelector(selector));
                    if (!elements.isEmpty()) {
                        log.info("Encontrados {} produtos com seletor: {}", elements.size(), selector);
                        productElements = elements;
                        break;
                    }
                } catch (Exception e) {
                    log.debug("Erro com seletor {}: {}", selector, e.getMessage());
                }
            }
            
            log.info("Total de produtos encontrados: {}", productElements.size());
            
            // Extrair dados de cada produto (máximo 20)
            for (int i = 0; i < Math.min(productElements.size(), 20); i++) {
                try {
                    WebElement product = productElements.get(i);
                    
                    // Extrair nome
                    String name = "";
                    String[] nameSelectors = {
                        ".nameCard",
                        "h2",
                        ".sc-iBYQkv",
                        "span.nameCard",
                        "[class*='name']"
                    };
                    
                    for (String selector : nameSelectors) {
                        try {
                            WebElement nameElement = product.findElement(By.cssSelector(selector));
                            String extractedName = nameElement.getText().trim();
                            if (extractedName.length() > 5) {
                                name = extractedName;
                                break;
                            }
                        } catch (Exception e) {
                            // Continuar para próximo seletor
                        }
                    }
                    
                    // Extrair preço
                    BigDecimal price = null;
                    String[] priceSelectors = {
                        ".priceCard",
                        ".finalPrice",
                        "span[class*='price']",
                        "[data-testid='price']",
                        ".sc-dcJsrY"
                    };
                    
                    for (String selector : priceSelectors) {
                        try {
                            WebElement priceElement = product.findElement(By.cssSelector(selector));
                            String priceText = priceElement.getText().trim();
                            BigDecimal parsedPrice = parseBrazilianPrice(priceText);
                            if (parsedPrice != null) {
                                price = parsedPrice;
                                break;
                            }
                        } catch (Exception e) {
                            // Continuar para próximo seletor
                        }
                    }
                    
                    // Extrair URL
                    String url = "";
                    try {
                        WebElement linkElement = product.findElement(By.tagName("a"));
                        url = linkElement.getDomAttribute("href");
                        if (url != null && !url.startsWith("http")) {
                            url = "https://www.kabum.com.br" + url;
                        }
                    } catch (Exception e) {
                        log.debug("Erro extraindo URL: {}", e.getMessage());
                    }
                    
                    // Validar e adicionar produto
                    if (!name.isEmpty() && price != null && url != null && !url.isEmpty()) {
                        // Validação básica: pelo menos deve conter alguma palavra do produto pesquisado
                        String lowerName = name.toLowerCase();
                        String lowerSearch = productName.toLowerCase();
                        boolean relevant = true;
                        
                        // Verificar se contém pelo menos 1 palavra relevante
                        String[] searchWords = lowerSearch.split("\\s+");
                        int matches = 0;
                        for (String word : searchWords) {
                            if (word.length() > 2 && lowerName.contains(word)) {
                                matches++;
                            }
                        }
                        
                        if (matches == 0) {
                            relevant = false;
                        }
                        
                        if (relevant) {
                            products.add(new ProductData(name, price, url));
                            log.info("Produto adicionado: {} - R$ {} - {}", name, price, url);
                        } else {
                            log.debug("Produto descartado (não relevante): {}", name);
                        }
                    }
                    
                } catch (Exception e) {
                    log.debug("Erro processando produto {}: {}", i, e.getMessage());
                }
            }
            
        } catch (Exception e) {
            log.error("Erro durante busca no Kabum: {}", e.getMessage(), e);
        }
        
        return products;
    }

    public List<ProductData> searchMercadoLivre(String productName) {
        List<ProductData> products = new ArrayList<>();
        
        try {
            initDriver();
            
            // Primeiro acessar a página principal para estabelecer sessão
            log.info("Acessando página principal do Mercado Livre...");
            driver.get("https://www.mercadolivre.com.br");
            Thread.sleep(2000);
            
            String searchUrl = "https://lista.mercadolivre.com.br/" + productName.replaceAll("\\s+", "-");
            log.info("Acessando busca no Mercado Livre: {}", searchUrl);
            
            driver.get(searchUrl);
            
            // Debug: aguardar um pouco e verificar o que carregou
            Thread.sleep(3000);
            log.info("Título da página: '{}'", driver.getTitle());
            log.info("URL atual: '{}'", driver.getCurrentUrl());
            
            // Verificar se foi redirecionado ou bloqueado
            String pageSource = driver.getPageSource();
            log.info("Tamanho da página: {} caracteres", pageSource != null ? pageSource.length() : 0);
            // Verificar se a página contém conteúdo do produto
            boolean hasProductKeyword = false;
            if (pageSource != null && productName != null) {
                String[] keywords = productName.toLowerCase().split("\\s+");
                for (String keyword : keywords) {
                    if (keyword.length() > 3 && pageSource.toLowerCase().contains(keyword)) {
                        hasProductKeyword = true;
                        break;
                    }
                }
            }
            log.info("Contém palavras-chave do produto: {}", hasProductKeyword);
            log.info("Contém 'MLB': {}", pageSource != null && pageSource.contains("MLB"));
            log.info("Contém 'produto': {}", pageSource != null && pageSource.contains("produto"));
            
            // Verificar se há captcha ou bloqueio
            if (pageSource != null && (pageSource.contains("captcha") || pageSource.contains("robot") || pageSource.contains("verificação"))) {
                log.warn("Página pode ter captcha ou bloqueio anti-bot!");
            }
            
            // Aguardar carregamento da página
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            
            // Tentar múltiplos seletores para aguardar carregamento
            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".ui-search-results")));
            } catch (Exception e) {
                log.warn("Seletor .ui-search-results não encontrado, tentando alternativos...");
                try {
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("[data-testid='results-list']")));
                } catch (Exception e2) {
                    log.warn("Seletor [data-testid='results-list'] não encontrado, aguardando qualquer elemento...");
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("body")));
                }
            }
            
            log.info("Página carregada, buscando produtos...");
            
            // Buscar elementos de produtos com múltiplos seletores
            List<WebElement> productElements = new ArrayList<>();
            String[] selectors = {
                ".ui-search-results .ui-search-item",
                "[data-testid='results-list'] .ui-search-item",
                ".ui-search-item",
                ".shops__item",
                "[data-testid='product-item']",
                ".ui-search-results > li",
                ".ui-search-results li",
                ".results-item",
                ".item"
            };
            
            for (String selector : selectors) {
                productElements = driver.findElements(By.cssSelector(selector));
                if (!productElements.isEmpty()) {
                    log.info("Encontrados {} produtos com seletor: {}", productElements.size(), selector);
                    break;
                }
            }
            
            // Se ainda não encontrou, tentar buscar especificamente pelo nome do produto
            if (productElements.isEmpty() && productName != null && !productName.isEmpty()) {
                // Extrair primeira palavra-chave do produto (ignorar palavras muito comuns)
                String[] words = productName.toLowerCase().split("\\s+");
                String keyword = "";
                for (String word : words) {
                    if (word.length() > 3 && !word.equals("para") && !word.equals("com") && !word.equals("produto")) {
                        keyword = word;
                        break;
                    }
                }
                
                if (!keyword.isEmpty()) {
                    log.info("Tentando estratégia alternativa: buscar especificamente por '{}'...", keyword);
                    
                    // Buscar por elementos que contenham a palavra-chave no texto
                    List<WebElement> keywordElements = driver.findElements(By.xpath("//*[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '" + keyword + "')]"));
                    log.info("Encontrados {} elementos contendo '{}'", keywordElements.size(), keyword);
                    
                    // Para cada elemento que contém a palavra-chave, tentar encontrar o produto pai
                    for (WebElement keywordElement : keywordElements) {
                        try {
                            // Subir na hierarquia até encontrar um container de produto
                            WebElement current = keywordElement;
                        for (int level = 0; level < 5; level++) {
                            current = current.findElement(By.xpath("./.."));
                            String tagName = current.getTagName();
                            String className = current.getDomAttribute("class");
                            
                            // Verificar se parece um container de produto
                            if (className != null && (className.contains("item") || className.contains("product") || 
                                className.contains("search") || tagName.equals("li") || tagName.equals("div"))) {
                                productElements.add(current);
                                break;
                            }
                        }
                    } catch (Exception e) {
                        // Ignorar se não conseguir subir na hierarquia
                    }
                }
                
                log.info("Total de elementos de produto encontrados após estratégia alternativa: {}", productElements.size());
                }
            }
            
            log.info("Total de elementos de produto encontrados: {}", productElements.size());
            
            // Debug: verificar se há elementos na página
            if (productElements.isEmpty()) {
                log.warn("Nenhum produto encontrado. Verificando estrutura da página...");
                log.warn("Título da página: '{}'", driver.getTitle());
                log.warn("URL atual: '{}'", driver.getCurrentUrl());
                
                // Tentar estratégia alternativa: buscar por qualquer elemento que contenha palavras-chave do produto
                if (productName != null && !productName.isEmpty()) {
                    String[] keywords = productName.toLowerCase().split("\\s+");
                    for (String keyword : keywords) {
                        if (keyword.length() > 3) {
                            try {
                                List<WebElement> allElements = driver.findElements(By.xpath("//*[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '" + keyword + "')]"));
                                log.info("Elementos contendo '{}': {}", keyword, allElements.size());
                                
                                if (!allElements.isEmpty()) {
                                    log.info("Primeiro elemento encontrado: '{}'", allElements.get(0).getText());
                                    break;
                                }
                            } catch (Exception e) {
                                log.debug("Erro ao buscar elementos com '{}': {}", keyword, e.getMessage());
                            }
                        }
                    }
                }
                
                // Tentar buscar por links MLB
                try {
                    List<WebElement> mlbLinks = driver.findElements(By.cssSelector("a[href*='MLB-']"));
                    log.info("Links MLB encontrados: {}", mlbLinks.size());
                } catch (Exception e) {
                    log.debug("Erro ao buscar links MLB: {}", e.getMessage());
                }
            }
            
            log.info("Processando {} produtos...", productElements.size());
            log.info("Vamos processar os primeiros {} produtos", Math.min(10, productElements.size()));
            
            for (int i = 0; i < Math.min(10, productElements.size()); i++) {
                try {
                    WebElement product = productElements.get(i);
                    log.info("=== PROCESSANDO PRODUTO {} ===", i + 1);
                    
                    // Extrair nome com múltiplos seletores
                    String name = "";
                    String[] nameSelectors = {
                        ".ui-search-item__title",
                        ".shops__item-title", 
                        ".item__title",
                        "h2 a span",
                        ".ui-search-item__title a",
                        "h2", "h3", "h4",
                        ".title", ".product-title",
                        "a[href*='/produto/']",
                        "a[href*='MLB-']"
                    };
                    
                    log.info("Tentando extrair nome...");
                    for (String selector : nameSelectors) {
                        try {
                            WebElement nameElement = product.findElement(By.cssSelector(selector));
                            String extractedName = nameElement.getText().trim();
                            log.debug("  Seletor '{}': '{}'", selector, extractedName);
                            if (extractedName.length() > 5) {
                                name = extractedName;
                                break;
                            }
                        } catch (Exception e) {
                            log.debug("  Seletor '{}': erro - {}", selector, e.getMessage());
                        }
                    }
                    
                    // Se ainda não encontrou nome, tentar buscar por texto que contenha palavras-chave do produto
                    if (name.isEmpty() && productName != null && !productName.isEmpty()) {
                        String[] keywords = productName.toLowerCase().split("\\s+");
                        for (String keyword : keywords) {
                            if (keyword.length() > 3) {
                                log.info("Nome vazio, tentando buscar por '{}'...", keyword);
                                try {
                                    WebElement keywordElement = product.findElement(By.xpath(".//*[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '" + keyword + "')]"));
                                    name = keywordElement.getText().trim();
                                    log.info("  Encontrado via '{}': '{}'", keyword, name);
                                    break;
                                } catch (Exception e) {
                                    log.debug("  Não encontrou elemento com '{}': {}", keyword, e.getMessage());
                                }
                            }
                        }
                    }
                    
                    log.info("Nome final: '{}'", name);
                    
                    // Extrair preço com múltiplos seletores
                    BigDecimal price = null;
                    String[] priceSelectors = {
                        ".andes-money-amount__fraction",
                        ".price-tag-fraction",
                        ".andes-money-amount",
                        ".ui-search-price__part",
                        ".price-tag",
                        ".price",
                        "[class*='price']",
                        "[class*='money']",
                        ".amount"
                    };
                    
                    log.info("Tentando extrair preço...");
                    for (String selector : priceSelectors) {
                        try {
                            WebElement priceElement = product.findElement(By.cssSelector(selector));
                            String priceText = priceElement.getText().trim();
                            log.debug("  Seletor '{}': '{}'", selector, priceText);
                            BigDecimal parsedPrice = parseBrazilianPrice(priceText);
                            if (parsedPrice != null) {
                                price = parsedPrice;
                                log.info("  Preço parseado: R$ {}", price);
                                break;
                            }
                        } catch (Exception e) {
                            log.debug("  Seletor '{}': erro - {}", selector, e.getMessage());
                        }
                    }
                    
                    // Se não encontrou preço com seletores, tentar buscar por texto que contenha "R$"
                    if (price == null) {
                        log.info("Preço nulo, tentando buscar por R$...");
                        try {
                            WebElement priceElement = product.findElement(By.xpath(".//*[contains(text(), 'R$')]"));
                            String priceText = priceElement.getText().trim();
                            log.info("  Encontrado via R$: '{}'", priceText);
                            price = parseBrazilianPrice(priceText);
                            if (price != null) {
                                log.info("  Preço parseado via R$: R$ {}", price);
                            }
                        } catch (Exception e) {
                            log.debug("  Não encontrou elemento com R$: {}", e.getMessage());
                        }
                    }
                    
                    log.info("Preço final: {}", price);
                    
                    // Extrair URL com múltiplos seletores
                    String url = "";
                    String[] urlSelectors = {
                        ".ui-search-item__title a",
                        ".shops__item-title a",
                        ".item__title a",
                        "h2 a", "h3 a",
                        "a[href*='MLB-']",
                        "a[href*='/produto/']",
                        "a[href*='/item/']",
                        "a"
                    };
                    
                    for (String selector : urlSelectors) {
                        try {
                            WebElement linkElement = product.findElement(By.cssSelector(selector));
                            url = linkElement.getDomAttribute("href");
                            if (url != null && !url.isEmpty() && (url.contains("MLB-") || url.contains("/produto/") || url.contains("/item/"))) break;
                        } catch (Exception e) {
                            // Continuar tentando outros seletores
                        }
                    }
                    
                    // DEBUG: Mostrar todos os produtos encontrados
                    log.info("=== PRODUTO {} ENCONTRADO ===", i + 1);
                    log.info("Nome: '{}' (tamanho: {})", name, name.length());
                    log.info("Preço: {}", price);
                    log.info("URL: '{}'", url);
                    
                    // Filtrar produtos relevantes (genérico)
                    if (name.length() > 5 && price != null && price.compareTo(new BigDecimal("10")) > 0) {
                        boolean isNotRedirectLink = url != null && !url.contains("click1.mercadolivre.com.br") &&
                                                  !url.contains("external");
                        
                        if (isNotRedirectLink) {
                            products.add(new ProductData(name, price, url));
                            log.info(" Produto relevante {}: {} - R$ {}", products.size(), name, price);
                        } else {
                            log.info(" Produto descartado (link de redirecionamento): {} - R$ {}", name, price);
                        }
                    } else {
                        log.info(" Produto descartado (nome muito curto ou preço muito baixo): '{}' - R$ {}", name, price);
                    }
                    
                } catch (Exception e) {
                    log.debug("Erro ao processar produto {}: {}", i + 1, e.getMessage());
                }
            }
            
            log.info("Total de produtos válidos extraídos: {}", products.size());
            
        } catch (Exception e) {
            log.error("Erro ao buscar produtos no Mercado Livre: {}", e.getMessage());
        }
        
        return products;
    }

    private BigDecimal parseBrazilianPrice(String priceText) {
        try {
            log.info("Parseando preço original: '{}'", priceText);
            
            // Remover caracteres não numéricos exceto vírgula e ponto
            String cleanPrice = priceText.replaceAll("[^0-9.,]", "");
            log.info("Preço limpo: '{}'", cleanPrice);
            
            // Se tem ponto e vírgula, ponto é milhares e vírgula é decimal
            if (cleanPrice.contains(".") && cleanPrice.contains(",")) {
                cleanPrice = cleanPrice.replace(".", "").replace(",", ".");
                log.info("Formato ponto + vírgula, resultado: '{}'", cleanPrice);
            }
            // Se só tem vírgula, pode ser decimal
            else if (cleanPrice.contains(",") && !cleanPrice.contains(".")) {
                // Verificar se a vírgula é separador de milhares ou decimal
                String[] parts = cleanPrice.split(",");
                if (parts.length == 2 && parts[1].length() <= 2) {
                    // Vírgula como decimal (ex: 123,45)
                    cleanPrice = cleanPrice.replace(",", ".");
                    log.info("Vírgula como decimal, resultado: '{}'", cleanPrice);
                } else {
                    // Vírgula como separador de milhares (ex: 1.234,56)
                    cleanPrice = cleanPrice.replace(",", ".");
                    log.info("Vírgula como milhares, resultado: '{}'", cleanPrice);
                }
            }
            // Se só tem ponto, pode ser milhares ou decimal
            else if (cleanPrice.contains(".") && !cleanPrice.contains(",")) {
                // Verificar se o ponto é separador de milhares ou decimal
                String[] parts = cleanPrice.split("\\.");
                if (parts.length == 2 && parts[1].length() <= 2) {
                    // Ponto como decimal (ex: 123.45) - formato americano
                    log.info("Ponto como decimal, mantendo: '{}'", cleanPrice);
                } else {
                    // Ponto como separador de milhares (ex: 8.095 = 8095) - formato brasileiro
                    cleanPrice = cleanPrice.replace(".", "");
                    log.info("Ponto como milhares, resultado: '{}'", cleanPrice);
                }
            }
            
            BigDecimal result = new BigDecimal(cleanPrice);
            log.info("Preço final parseado: {}", result);
            return result;
        } catch (Exception e) {
            log.error("Erro ao parsear preço '{}': {}", priceText, e.getMessage());
            return null;
        }
    }

    /**
     * Método genérico para buscar produtos em qualquer site usando Selenium
     * COM TÉCNICAS ANTI-DETECÇÃO
     */
    public List<ProductData> searchGenericSite(String siteName, String searchUrl, String productName) {
        List<ProductData> products = new ArrayList<>();
        
        try {
            initDriver();
            
            log.info("Acessando {} com Selenium (modo stealth)...", siteName);
            log.info("URL: {}", searchUrl);
            
            // ANTI-DETECÇÃO: Visitar página inicial primeiro (simular navegação humana)
            String baseUrl = searchUrl.substring(0, searchUrl.indexOf("/", 8));
            log.info("Visitando página inicial: {}", baseUrl);
            driver.get(baseUrl);
            Thread.sleep(2000 + (int)(Math.random() * 1000)); // 2-3 segundos aleatórios
            
            // ANTI-DETECÇÃO: Agora acessar a busca
            log.info("Navegando para busca...");
            driver.get(searchUrl);
            Thread.sleep(3000 + (int)(Math.random() * 2000)); // 3-5 segundos aleatórios
            
            log.info("Título da página: '{}'", driver.getTitle());
            
            // Aguardar produtos carregarem
            try {
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("body")));
                log.info("Página carregada!");
            } catch (Exception e) {
                log.warn("Timeout aguardando página: {}", e.getMessage());
            }
            
            // Seletores genéricos para produtos (incluindo Nike, Adidas, Netshoes)
            String[] productSelectors = {
                "article[data-id]", ".product-card", ".product-item", ".product",
                "[data-product-id]", ".item-card", ".grid-item",
                "[data-testid='product-card']", "[data-auto-id='product-card']",
                "div[class*='product-card']", "div[class*='product-item']",
                "article[class*='product']", "li[class*='product']",
                "a[href*='/produto/']", "a[href*='/product/']",
                ".gl-product-card", ".product-tile"
            };
            
            List<WebElement> productElements = new ArrayList<>();
            for (String selector : productSelectors) {
                try {
                    List<WebElement> elements = driver.findElements(By.cssSelector(selector));
                    if (!elements.isEmpty()) {
                        log.info("Encontrados {} elementos com seletor: {}", elements.size(), selector);
                        productElements = elements;
                        break;
                    }
                } catch (Exception e) {
                    log.debug("Erro com seletor {}: {}", selector, e.getMessage());
                }
            }
            
            log.info("Total de elementos encontrados: {}", productElements.size());
            
            // Extrair dados (máximo 20)
            int maxElements = Math.min(productElements.size(), 20);
            for (int i = 0; i < maxElements; i++) {
                processProductElement(productElements.get(i), i + 1, maxElements, productName, products);
            }
            
            log.info("Total de produtos válidos extraídos de {}: {}", siteName, products.size());
            
        } catch (Exception e) {
            log.error("Erro durante busca genérica em {}: {}", siteName, e.getMessage(), e);
        }
        
        return products;
    }
    
    private void processProductElement(WebElement element, int index, int total, String productName, List<ProductData> products) {
        try {
            log.info("=== Processando elemento {} de {} ===", index, total);
            
            String name = extractProductName(element);
            BigDecimal price = extractPriceFromElement(element);
            String url = extractUrlFromElement(element);
            
            logExtractedData(name, price, url);
            
            if (isValidProduct(name, price, url, productName)) {
                products.add(new ProductData(name, price, url));
                log.info("  ✓ Produto adicionado: {} - R$ {}", name, price);
            }
            
        } catch (Exception e) {
            log.debug("Erro processando elemento {}: {}", index, e.getMessage());
        }
    }
    
    private String extractProductName(WebElement element) {
        return extractTextFromElement(element, 
            "h2", "h3", "h4", ".product-name", ".product-title", 
            ".item-card__title", "[class*='title']", "[class*='name']",
            "a", "span", ".description", "[class*='description']");
    }
    
    private void logExtractedData(String name, BigDecimal price, String url) {
        log.info("  Nome extraído: '{}'", name.isEmpty() ? "VAZIO" : name);
        log.info("  Preço extraído: {}", price);
        String urlPreview = (url != null && !url.isEmpty()) 
            ? url.substring(0, Math.min(50, url.length())) + "..." 
            : "VAZIA";
        log.info("  URL extraída: '{}'", urlPreview);
    }
    
    private boolean isValidProduct(String name, BigDecimal price, String url, String productName) {
        if (name.isEmpty() || price == null || url == null || url.isEmpty()) {
            log.info("  ✗ Produto descartado - nome vazio: {}, preço nulo: {}, url vazia: {}", 
                    name.isEmpty(), price == null, url == null || url.isEmpty());
            return false;
        }
        
        if (!isBasicRelevant(name, productName)) {
            log.info("  ✗ Produto descartado (não relevante): {}", name);
            return false;
        }
        
        return true;
    }
    
    private String extractTextFromElement(WebElement element, String... selectors) {
        for (String selector : selectors) {
            try {
                WebElement textElement = element.findElement(By.cssSelector(selector));
                String text = textElement.getText().trim();
                if (text.length() > 5) {
                    return text;
                }
            } catch (Exception e) {
                // Continuar para próximo seletor
            }
        }
        return "";
    }
    
    private BigDecimal extractPriceFromElement(WebElement element) {
        String[] priceSelectors = {
            ".price", ".preco", "[class*='price']", "[class*='preco']",
            ".item-card__price", ".product-price", "[data-testid*='price']"
        };
        
        for (String selector : priceSelectors) {
            try {
                WebElement priceElement = element.findElement(By.cssSelector(selector));
                String priceText = priceElement.getText().trim();
                BigDecimal price = parseBrazilianPrice(priceText);
                if (price != null) {
                    return price;
                }
            } catch (Exception e) {
                // Continuar
            }
        }
        
        // Tentar buscar por R$
        try {
            WebElement priceElement = element.findElement(By.xpath(".//*[contains(text(), 'R$')]"));
            String priceText = priceElement.getText().trim();
            return parseBrazilianPrice(priceText);
        } catch (Exception e) {
            return null;
        }
    }
    
    private String extractUrlFromElement(WebElement element) {
        try {
            WebElement linkElement = element.findElement(By.tagName("a"));
            return linkElement.getDomAttribute("href");
        } catch (Exception e) {
            return "";
        }
    }
    
    private boolean isBasicRelevant(String foundName, String searchedName) {
        String lowerFound = foundName.toLowerCase();
        String lowerSearch = searchedName.toLowerCase();
        
        String[] searchWords = lowerSearch.split("\\s+");
        int matches = 0;
        
        for (String word : searchWords) {
            if (word.length() > 2 && lowerFound.contains(word)) {
                matches++;
            }
        }
        
        return matches > 0;
    }
}
