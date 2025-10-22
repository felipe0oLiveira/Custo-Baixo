# 🛒 Compra Certa - Sistema Inteligente de Comparação de Preços

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-12+-blue.svg)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

> **Encontre o melhor preço em segundos. Automatize sua pesquisa em dezenas de e-commerces brasileiros.**

---

## 🎯 O Que É o Compra Certa?

**Compra Certa** é uma plataforma automatizada de comparação de preços que **elimina o trabalho manual** de visitar múltiplos sites de e-commerce para encontrar a melhor oferta. 

### 📱 Cenário Real

Imagine que você quer comprar um **iPhone 15 Pro 256GB**. Tradicionalmente, você teria que:

1. ✋ Abrir Amazon → Buscar → Filtrar resultados → Anotar preço
2. ✋ Abrir Mercado Livre → Buscar → Filtrar → Anotar preço
3. ✋ Abrir Magazine Luiza → Buscar → Filtrar → Anotar preço
4. ✋ Abrir mais 5-10 sites → Repetir o processo
5. 🧮 Comparar manualmente todos os preços anotados
6. ⏱️ **Tempo gasto: 15-30 minutos**

### ✨ Com o Compra Certa

```json
// Você envia uma simples requisição:
{
  "productName": "iPhone 15 Pro 256GB"
}

// Em 5-10 segundos, recebe:
{
  "bestPrice": {
    "siteName": "MERCADO_LIVRE",
    "price": 6899.00,
    "url": "https://..."
  },
  "savings": 400.00,
  "allPrices": [
    { "site": "MERCADO_LIVRE", "price": 6899.00 },
    { "site": "AMAZON", "price": 7299.00 },
    { "site": "KABUM", "price": 7199.00 }
  ]
}
```

⏱️ **Tempo gasto: 10 segundos** | 💰 **Economia: R$ 400,00**

---

## 💡 Por Que Compra Certa Existe?

### O Problema

O e-commerce brasileiro cresceu exponencialmente, mas isso trouxe um **desafio para o consumidor**:

- 📊 **Mais de 50 grandes marketplaces** ativos no Brasil
- 💸 **Variação de até 40%** no preço do mesmo produto entre sites
- 🔍 **Dificuldade em encontrar** o melhor custo-benefício
- ⏰ **Tempo escasso** para pesquisar em todos os sites
- 🎭 **Ofertas falsas** e produtos irrelevantes nos resultados

### A Solução

O Compra Certa atua como seu **assistente pessoal de compras**, fazendo o trabalho pesado para você:

```
┌──────────────────────────────────────────────────┐
│                                                  │
│  "Quero comprar um Tênis Nike Air Max 2024"     │
│                                                  │
└──────────────────┬───────────────────────────────┘
                   │
                   ▼
        ┌──────────────────────┐
        │  COMPRA CERTA        │
        │  (Robô Inteligente)  │
        └──────────┬───────────┘
                   │
        ┌──────────┴──────────┐
        │                     │
        ▼                     ▼
   🤖 Busca em          🎯 Filtra resultados
   4 sites              irrelevantes
   simultaneamente      (remove capinhas,
                        acessórios, etc)
        │                     │
        └──────────┬──────────┘
                   │
                   ▼
        ┌──────────────────────┐
        │  Resultado Limpo:    │
        │                      │
        │  NETSHOES: R$ 549,90 │
        │  AMAZON:   R$ 599,90 │
        │                      │
        │  💰 Melhor: R$ 549,90│
        │  ✅ Economia: R$ 50  │
        └──────────────────────┘
```

---

## 🎯 Funcionalidades Principais

### 1. 🔍 Busca Inteligente Multi-Site

Ao invés de você visitar cada site manualmente, o sistema:
- Acessa **4 e-commerces simultaneamente**
- Executa a busca do produto em **paralelo**
- Coleta todos os resultados em **poucos segundos**

### 2. 🧠 Filtros de Relevância Contextuais

O maior diferencial do Compra Certa é sua **inteligência de filtragem**:

#### Sem Filtros (Busca Normal)
Buscar "iPhone 15 Pro" retorna:
- ❌ Capinhas para iPhone
- ❌ Películas de vidro
- ❌ Carregadores
- ❌ Fones de ouvido
- ✅ iPhone 15 Pro (perdido no meio do lixo)

#### Com Compra Certa
Buscar "iPhone 15 Pro" retorna:
- ✅ iPhone 15 Pro 128GB - R$ 6.499
- ✅ iPhone 15 Pro 256GB - R$ 6.899
- ✅ iPhone 15 Pro 512GB - R$ 7.999
- ❌ **Tudo que não for iPhone é automaticamente descartado**

**Como funciona:**
```java
// O sistema detecta que é um smartphone
// Aplica filtros específicos:
- Remove acessórios (capa, película, carregador)
- Valida preço mínimo (R$ 800+)
- Exige 50%+ de match nas palavras-chave
- Resultado: Apenas iPhones reais
```

### 3. 📊 Comparação Automática

- Organiza todos os preços encontrados
- Identifica o **melhor preço**
- Calcula a **economia** em relação aos outros
- Mostra o **percentual de desconto**

### 4. 🏷️ Detecção Automática de Categoria

O sistema **entende o que você está buscando**:

| Você busca | Sistema detecta | Filtros aplicados |
|-----------|-----------------|-------------------|
| iPhone 15 Pro | 📱 Smartphone | Remove acessórios, valida R$ 800+ |
| PlayStation 5 | 🎮 Console | Remove jogos/controles, valida R$ 2.000+ |
| Tênis Nike | 👟 Calçado | Remove meias/palmilhas, valida R$ 50-5.000 |
| Notebook Dell | 💻 Notebook | Remove periféricos, valida R$ 1.200+ |

### 5. 🕵️ Técnicas Anti-Detecção

Muitos sites bloqueiam robôs. O Compra Certa usa:

- **Simulação de comportamento humano**: Visita home antes de buscar
- **Delays aleatórios**: Espera 2-5 segundos entre ações
- **User-Agent rotation**: Alterna entre navegadores reais
- **Modo headless**: Chrome invisível no background

---

## 🌐 Sites Suportados

| Site | Categoria | Status | Método |
|------|-----------|--------|--------|
| **Amazon** | Generalista | ✅ Ativo | Jsoup + Selenium |
| **Mercado Livre** | Generalista | ✅ Ativo | Selenium Direct |
| **KaBuM!** | Eletrônicos | ✅ Ativo | Jsoup + Selenium |
| **Netshoes** | Esportes/Moda | ✅ Ativo | Selenium Stealth |

**Em desenvolvimento:** Magazine Luiza, Americanas, Casas Bahia

---

## 🛠️ Tecnologias Utilizadas

```
Backend Principal:
├── Java 17 (Linguagem)
├── Spring Boot 3.5.6 (Framework)
├── Spring Data JPA (Persistência)
└── PostgreSQL (Banco de Dados)

Web Scraping:
├── Jsoup 1.15.4 (HTML estático - rápido)
├── Selenium WebDriver 4.x (JavaScript dinâmico)
└── ChromeDriver (Automação do Chrome)

Utilitários:
├── Lombok (Menos código boilerplate)
├── SLF4J + Logback (Logs estruturados)
└── Maven (Gerenciamento de dependências)
```

---

## 📋 Índice Completo

- [Como Funciona](#como-funciona-o-fluxo-completo)
- [API Endpoints](#-api-endpoints)
- [Arquitetura do Sistema](#-arquitetura)
- [Testes e Qualidade](#-testes)
- [Como Contribuir](#-contribuindo)

---

## 🔄 Como Funciona: O Fluxo Completo

### Passo a Passo Técnico

```
1️⃣ RECEPÇÃO
   └─ Controller recebe requisição HTTP POST
      └─ Valida payload JSON
         └─ Extrai: productName

2️⃣ DETECÇÃO DE CATEGORIA (IA Simplificada)
   └─ Analisa palavras-chave do produto
      ├─ "iphone" → Smartphone
      ├─ "playstation" → Console
      ├─ "tenis" → Calçado
      └─ "notebook" → Laptop

3️⃣ SELEÇÃO DE SITES RELEVANTES
   └─ Categoria: Smartphone
      ├─ ✅ Amazon (vende eletrônicos)
      ├─ ✅ Mercado Livre (vende tudo)
      ├─ ✅ KaBuM! (especialista eletrônicos)
      └─ ❌ Netshoes (só esportes/moda)

4️⃣ BUSCA PARALELA (Threads)
   ├─ Thread 1: Amazon
   │   ├─ Tenta Jsoup (rápido)
   │   └─ Se falhar → Selenium
   │
   ├─ Thread 2: Mercado Livre
   │   └─ Selenium direto (JavaScript pesado)
   │
   ├─ Thread 3: KaBuM!
   │   └─ Jsoup → Selenium (fallback)
   │
   └─ Aguarda todos finalizarem (máx 30s)

5️⃣ FILTRAGEM DE RELEVÂNCIA
   └─ Para cada produto encontrado:
      ├─ Valida nome (50%+ match)
      ├─ Valida preço (R$ 800+ para smartphone)
      ├─ Remove acessórios (capa, película)
      └─ Descarta se não passar

6️⃣ COMPARAÇÃO E ORDENAÇÃO
   └─ Ordena por preço (menor primeiro)
      └─ Calcula economia vs mais caro
         └─ Identifica melhor oferta

7️⃣ RESPOSTA JSON
   └─ Retorna para cliente em <10s
```

### Exemplo Real de Execução

**Input:**
```bash
POST /api/products/compare-prices-simple
{
  "productName": "PlayStation 5 Digital"
}
```

**Processing (5-8 segundos):**
```
[INFO] Detectando categoria... ✓ CONSOLE
[INFO] Sites relevantes: AMAZON, MERCADO_LIVRE, KABUM
[INFO] Buscando em AMAZON... (Jsoup)
[INFO] Buscando em MERCADO_LIVRE... (Selenium)
[INFO] Buscando em KABUM... (Jsoup)
[INFO] AMAZON encontrou 12 produtos
[INFO] Filtrando... 3 relevantes (9 descartados)
[INFO] MERCADO_LIVRE encontrou 8 produtos
[INFO] Filtrando... 5 relevantes (3 descartados)
[INFO] KABUM encontrou 4 produtos
[INFO] Filtrando... 2 relevantes (2 descartados)
[INFO] Melhor preço: KABUM - R$ 3.599,90
```

**Output:**
```json
{
  "productName": "PlayStation 5 Digital",
  "bestPrice": {
    "siteName": "KABUM",
    "price": 3599.90,
    "productUrl": "https://kabum.com.br/..."
  },
  "allPrices": [
    { "site": "KABUM", "price": 3599.90 },
    { "site": "AMAZON", "price": 3799.90 },
    { "site": "MERCADO_LIVRE", "price": 3899.90 }
  ],
  "savings": 300.00,
  "savingsPercentage": 7.69
}
```

---

## 🔌 API Endpoints

### `POST /api/products/compare-prices-simple`

**Descrição:** Busca e compara preços sem salvar no banco de dados.

**Request:**
```json
{
  "productName": "Tênis Nike Air Max 2024"
}
```

**Response:**
```json
{
  "productName": "Tênis Nike Air Max 2024",
  "prices": [
    {
      "siteName": "NETSHOES",
      "price": 549.90,
      "productUrl": "https://netshoes.com.br/...",
      "available": true
    },
    {
      "siteName": "AMAZON",
      "price": 599.90,
      "productUrl": "https://amazon.com.br/...",
      "available": true
    }
  ],
  "bestPrice": {
    "siteName": "NETSHOES",
    "price": 549.90
  },
  "savings": 50.00,
  "savingsPercentage": 8.35,
  "sitesSearched": 4,
  "sitesWithProduct": 2
}
```

---

## 🏗️ Arquitetura

```
┌─────────────────────────────────────────┐
│         CLIENT (Usuário/Frontend)       │
└───────────────┬─────────────────────────┘
                │ HTTP POST /api/products
                ▼
┌───────────────────────────────────────────┐
│     ProductMonitorController.java         │
│     (REST API - Recebe requisições)       │
└───────────────┬───────────────────────────┘
                │
                ▼
┌───────────────────────────────────────────┐
│    SmartProductService.java ⭐            │
│    (Orquestrador Principal)               │
│    • Detecta categoria                    │
│    • Seleciona sites                      │
│    • Coordena busca paralela              │
│    • Aplica filtros                       │
└─────┬───────────────────┬─────────────────┘
      │                   │
      ▼                   ▼
┌─────────────┐    ┌──────────────────┐
│ SmartCategory│    │ SeleniumService  │
│ Service      │    │ WebScrapingService│
│ (Detecta cat)│    │ (Executa scraping)│
└─────────────┘    └──────────────────┘
                           │
                           ▼
                   ┌──────────────┐
                   │ Amazon       │
                   │ Mercado Livre│
                   │ KaBuM!       │
                   │ Netshoes     │
                   └──────────────┘
```

---

## 🧪 Testes

### Estratégia de Qualidade

O projeto mantém **alta cobertura de testes** para garantir que os filtros de relevância e comparações funcionem corretamente.

#### Testes Unitários

**Foco:** Lógica de negócio isolada

**Áreas Críticas Testadas:**

1. **Algoritmos de Relevância**
```java
@Test
void deveDescartarCapinhaAoBuscarIPhone() {
    // Garante que acessórios sejam filtrados
    assertFalse(isRelevant("Capinha iPhone 15", "iPhone 15"));
}

@Test
void deveAceitarIPhoneRealComPrecoValido() {
    // Garante que produtos reais passem
    assertTrue(isRelevant("iPhone 15 Pro 256GB", "iPhone 15", 
                          new BigDecimal("6899")));
}
```

2. **Parsing de Preços Brasileiros**
```java
@Test
void deveConverterPrecoComPontoEVirgula() {
    // "R$ 1.234,56" → 1234.56
    assertEquals(new BigDecimal("1234.56"), 
                 parseBrazilianPrice("R$ 1.234,56"));
}
```

3. **Detecção de Categorias**
```java
@Test
void deveDetectarSmartphone() {
    assertEquals(ProductCategory.ELETRONICOS,
                 detectCategory("iPhone 15 Pro"));
}
```

### Executando os Testes

```bash
# Todos os testes
mvn test

# Com relatório de cobertura
mvn test jacoco:report

# Apenas testes unitários
mvn test -Dtest=*Test
```

---

## 🤝 Contribuindo

Contribuições são muito bem-vindas! 

### Como Contribuir

1. Fork o projeto
2. Crie uma branch (`git checkout -b feature/NovaFuncionalidade`)
3. Commit (`git commit -m 'Add: Nova funcionalidade'`)
4. Push (`git push origin feature/NovaFuncionalidade`)
5. Abra um Pull Request

### Padrões

- ✅ Máximo 3 níveis de aninhamento
- ✅ Métodos com até 30 linhas
- ✅ Testes para novas features
- ✅ JavaDoc em métodos públicos

---

## 📝 Licença

MIT License - Veja [LICENSE](LICENSE)

---

## 📧 Contato

- 💬 Issues: [GitHub Issues](https://github.com/seu-usuario/compra-certa/issues)

---

**Desenvolvido com ❤️ e ☕ para ajudar você a economizar!**
