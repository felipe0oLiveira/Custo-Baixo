package org.custobaixo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.custobaixo.entity.ProductCategory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmartCategoryService {

    // Palavras-chave para detectar categorias
    private static final Map<ProductCategory, List<String>> CATEGORY_KEYWORDS = Map.of(
            ProductCategory.LIVROS, Arrays.asList(
                    "livro", "book", "ebook", "romance", "ficção", "não-ficção",
                    "biografia", "autobiografia", "história", "literatura", "poesia",
                    "didático", "escolar", "universitário", "enciclopédia", "dicionário",
                    "revista", "quadrinhos", "mangá", "hq", "graphic novel"
            ),
            ProductCategory.SAUDE_E_BELEZA, Arrays.asList(
                    "perfume", "fragrância", "eau de toilette", "eau de parfum",
                    "eau de cologne", "parfum", "colônia", "essência", "aroma",
                    "fragrance", "toilette", "cologne", "perfumaria", "cosmético",
                    "loção", "desodorante", "antitranspirante", "hidratante corporal",
                    "vitamina", "suplemento", "proteína", "whey", "creatina",
                    "omega", "colágeno", "magnésio", "zinco", "ferro", "cálcio",
                    "medicamento", "remédio", "antibiótico", "analgésico",
                    "termômetro", "pressão", "glicose", "teste", "máscara",
                    "álcool gel", "antisséptico", "curativo", "band-aid",
                    "maquiagem", "batom", "base", "pó", "sombra", "rímel",
                    "delineador", "blush", "corretivo", "primer", "iluminador",
                    "esmalte", "unha", "cutícula", "creme", "sérum", "hidratante",
                    "protetor solar", "filtro solar", "esfoliante", "máscara facial",
                    "tônico", "gel", "óleo", "shampoo", "condicionador"
            ),
            ProductCategory.ELETRONICOS, Arrays.asList(
                    "smartphone", "celular", "iphone", "samsung", "xiaomi", "motorola",
                    "notebook", "laptop", "computador", "pc", "desktop", "monitor",
                    "tv", "televisão", "tablet", "ipad", "kindle", "fone", "headphone",
                    "mouse", "teclado", "webcam", "câmera", "camera", "gopro",
                    "console", "playstation", "xbox", "nintendo", "videogame",
                    "processador", "memória", "ram", "ssd", "hd", "placa de vídeo",
                    "gabinete", "fonte", "cooler", "ventilador", "ar condicionado"
            ),
            ProductCategory.ROUPAS, Arrays.asList(
                    "camiseta", "camisa", "calça", "short", "bermuda", "jaqueta",
                    "blusa", "vestido", "saia", "meia", "cueca", "sutiã", "calcinha",
                    "tênis", "sapato", "bota", "chinelo", "sandália", "sapatilha",
                    "óculos", "relógio", "pulseira", "colar", "brinco", "anel",
                    "mochila", "bolsa", "carteira", "cinto", "chapéu", "boné",
                    "casaco", "blazer", "cardigan", "suéter", "moletom", "agasalho",
                    "salto", "rasteirinha", "mocassim", "oxford", "sneaker",
                    "nike", "adidas", "puma", "reebok", "under armour", "mizuno",
                    "asics", "new balance", "vans", "converse", "oakley",
                    "polo", "regata", "legging", "top", "fitness", "esportivo",
                    "corrida", "running", "training", "casual", "streetwear",
                    "jeans", "social", "alfaiataria", "blazer", "terno", "gravata"
            ),
            ProductCategory.CASA_E_JARDIM, Arrays.asList(
                    "sofá", "mesa", "cadeira", "armário", "guarda-roupa", "cama",
                    "colchão", "travesseiro", "lençol", "edredom", "cobertor",
                    "cortina", "tapete", "luminária", "abajur", "quadro", "espelho",
                    "vaso", "planta", "decoração", "ornamento", "candeeiro",
                    "mesa de centro", "estante", "prateleira", "gaveteiro"
            ),
            ProductCategory.ESPORTES, Arrays.asList(
                    "academia", "musculação", "cardio", "esteira", "bicicleta",
                    "halteres", "peso", "kettlebell", "corda", "pular corda",
                    "futebol", "basquete", "vôlei", "tênis", "ping pong", "badminton",
                    "natação", "corrida", "caminhada", "hiking", "trekking",
                    "equipamento", "acessório", "proteção", "capacete", "joelheira"
            ),
            ProductCategory.AUTOMOTIVO, Arrays.asList(
                    "carro", "automóvel", "veículo", "pneu", "bateria", "óleo",
                    "filtro", "vela", "freio", "pastilha", "disco", "amortecedor",
                    "mola", "suspensão", "direção", "motor", "câmbio", "embrague",
                    "escapamento", "catalisador", "muffler", "silencioso",
                    "lâmpada", "farol", "lanterna", "sinalizador", "retrovisor"
            )
    );


    // Sites que vendem TUDO (geralistas)
    private static final Set<String> GENERALIST_SITES = Set.of(
            "AMAZON", "MERCADO_LIVRE"
    );

    // Sites especialistas (apenas eletrônicos)
    private static final Set<String> ELECTRONICS_ONLY_SITES = Set.of(
            "KABUM"
    );

    // Sites especialistas (apenas roupas/moda/esportes)
    private static final Set<String> FASHION_SPORTS_SITES = Set.of(
            "NETSHOES"
    );

    /**
     * Detecta a categoria do produto baseado no nome e URL
     */
    public ProductCategory detectCategory(String productName, String productUrl) {
        log.debug("Detectando categoria para: {} - {}", productName, productUrl);

        String searchText = (productName + " " + productUrl).toLowerCase();
        Map<ProductCategory, Integer> categoryScores = calculateCategoryScores(searchText);

        // Retornar categoria com maior score
        ProductCategory detectedCategory = categoryScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(ProductCategory.ELETRONICOS); // Default para eletrônicos

        log.info("Categoria detectada: {} (score: {})", detectedCategory,
                categoryScores.getOrDefault(detectedCategory, 0));

        return detectedCategory;
    }

    private Map<ProductCategory, Integer> calculateCategoryScores(String searchText) {
        Map<ProductCategory, Integer> categoryScores = new HashMap<>();

        for (Map.Entry<ProductCategory, List<String>> entry : CATEGORY_KEYWORDS.entrySet()) {
            ProductCategory category = entry.getKey();
            List<String> keywords = entry.getValue();

            int score = 0;
            for (String keyword : keywords) {
                if (searchText.contains(keyword.toLowerCase())) {
                    score++;
                }
            }

            if (score > 0) {
                categoryScores.put(category, score);
            }
        }

        return categoryScores;
    }

    /**
     * Retorna lista de sites relevantes para a categoria do produto
     */
    public List<String> getRelevantSites(ProductCategory category) {
        log.debug("Obtendo sites relevantes para categoria: {}", category);

        // Adicionar sites geralistas (vendem tudo)
        Set<String> relevantSites = new HashSet<>(GENERALIST_SITES);

        // Para eletrônicos, adicionar sites especialistas
        if (category == ProductCategory.ELETRONICOS) {
            relevantSites.addAll(ELECTRONICS_ONLY_SITES);
        }

        // Para roupas e esportes, adicionar sites especialistas
        if (category == ProductCategory.ROUPAS || category == ProductCategory.ESPORTES) {
            relevantSites.addAll(FASHION_SPORTS_SITES);
        }

        List<String> result = new ArrayList<>(relevantSites);
        log.info("Sites relevantes para {}: {}", category, result);

        return result;
    }

}
