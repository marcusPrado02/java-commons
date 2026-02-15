# Commons Adapters :: Search :: OpenSearch

Adapter OpenSearch para o `SearchPort`, fornecendo busca full-text, indexação e agregações.

## Instalação

```xml
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-adapters-search-opensearch</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Visão Geral

Este adapter implementa o `SearchPort` usando o **OpenSearch Java Client 2.x**, fornecendo:

- **Indexação**: Documentos únicos e em lote (bulk)
- **Busca full-text**: Match, term, phrase, prefix, wildcard, fuzzy
- **Agregações**: Buckets (terms, date histograms) e métricas (avg, sum, min, max, stats)
- **Gerenciamento de índices**: Criação, exclusão, verificação, refresh
- **Paginação e ordenação**: Controle completo de resultados
- **Autenticação**: Basic auth e API key

## Quick Start

### 1. Configuração para Desenvolvimento

```java
OpenSearchConfiguration config = OpenSearchConfiguration.forDevelopment();
var adapter = new OpenSearchAdapter(config);

// Criar índice
adapter.createIndex("products");

// Indexar documento
Document product = Document.builder()
    .id("1")
    .field("name", "Laptop")
    .field("price", 999.99)
    .field("category", "Electronics")
    .build();

Result<String> result = adapter.index("products", product);
```

### 2. Configuração para Produção

```java
OpenSearchConfiguration config = OpenSearchConfiguration.forProduction()
    .addUrl("https://opensearch-1.example.com:9200")
    .addUrl("https://opensearch-2.example.com:9200") // Cluster
    .username("admin")
    .password(System.getenv("OPENSEARCH_PASSWORD"))
    .enableSsl(true)
    .build();

var adapter = new OpenSearchAdapter(config);
```

### 3. Busca Full-Text

```java
SearchQuery query = SearchQuery.builder()
    .query("laptop gaming")
    .type(QueryType.MATCH)
    .field("name")
    .from(0)
    .size(10)
    .build();

Result<SearchResult> result = adapter.search("products", query);

result.ifOk(searchResult -> {
    searchResult.hits().forEach(hit -> {
        String name = hit.getString("name");
        Double price = hit.getDouble("price");
        System.out.println(name + ": $" + price);
    });
});
```

## Configuração Completa

### Builder Pattern

```java
OpenSearchConfiguration config = OpenSearchConfiguration.builder()
    .addUrl("https://opensearch.example.com:9200")
    .username("admin")
    .password("password")
    .connectionTimeout(Duration.ofSeconds(10))
    .socketTimeout(Duration.ofSeconds(60))
    .maxConnections(50)
    .enableSsl(true)
    .verifySslCertificates(true)
    .build();
```

### Factory Methods

```java
// Desenvolvimento (localhost:9200, sem autenticação)
OpenSearchConfiguration dev = OpenSearchConfiguration.forDevelopment();

// Produção (requer configuração de URLs e autenticação)
OpenSearchConfiguration prod = OpenSearchConfiguration.forProduction()
    .addUrl("https://opensearch.example.com:9200")
    .username("user")
    .password("password")
    .build();

// API Key
OpenSearchConfiguration apiKey = OpenSearchConfiguration.withApiKey(
    "https://opensearch.example.com:9200",
    "key-id",
    "key-secret"
);
```

### Configuração por Properties

```yaml
opensearch:
  urls:
    - https://opensearch-1.example.com:9200
    - https://opensearch-2.example.com:9200
  username: ${OPENSEARCH_USER}
  password: ${OPENSEARCH_PASSWORD}
  connection-timeout: 10s
  socket-timeout: 60s
  max-connections: 50
  enable-ssl: true
  verify-ssl-certificates: true
```

## Operações

### Indexação

#### Documento Único

```java
Document doc = Document.builder()
    .id("123")
    .field("title", "Getting Started")
    .field("content", "OpenSearch is a powerful search engine")
    .field("tags", List.of("search", "opensearch", "tutorial"))
    .field("createdAt", Instant.now().toString())
    .build();

Result<String> result = adapter.index("blog", doc);

result.ifOk(id -> System.out.println("Indexed with ID: " + id));
```

#### Bulk Indexing

```java
List<Document> documents = List.of(
    Document.builder().id("1").field("name", "Product 1").build(),
    Document.builder().id("2").field("name", "Product 2").build(),
    Document.builder().id("3").field("name", "Product 3").build()
);

Result<BulkIndexResult> result = adapter.bulkIndex("products", documents);

result.ifOk(bulkResult -> {
    System.out.println("Indexed: " + bulkResult.successCount());
    System.out.println("Failed: " + bulkResult.failureCount());
});
```

### Busca

#### Match Query (Full-Text)

```java
SearchQuery query = SearchQuery.builder()
    .query("search engine")
    .type(QueryType.MATCH)
    .field("content")
    .from(0)
    .size(20)
    .build();

Result<SearchResult> result = adapter.search("blog", query);
```

#### Term Query (Exact Match)

```java
SearchQuery query = SearchQuery.builder()
    .query("Electronics")
    .type(QueryType.TERM)
    .field("category.keyword") // Keyword field for exact match
    .build();

Result<SearchResult> result = adapter.search("products", query);
```

#### Phrase Query

```java
SearchQuery query = SearchQuery.builder()
    .query("quick brown fox")
    .type(QueryType.PHRASE)
    .field("description")
    .build();

Result<SearchResult> result = adapter.search("articles", query);
```

#### Wildcard Query

```java
SearchQuery query = SearchQuery.builder()
    .query("prod*")
    .type(QueryType.WILDCARD)
    .field("name")
    .build();

Result<SearchResult> result = adapter.search("items", query);
```

#### Fuzzy Query (Typo Tolerance)

```java
SearchQuery query = SearchQuery.builder()
    .query("laptoop") // Tolerates typo
    .type(QueryType.FUZZY)
    .field("name")
    .build();

Result<SearchResult> result = adapter.search("products", query);
```

### Paginação e Ordenação

```java
SearchQuery query = SearchQuery.builder()
    .query("laptop")
    .type(QueryType.MATCH)
    .field("name")
    .from(20) // Skip first 20 results
    .size(10) // Return 10 results
    .addSort("price", SortOrder.ASC)
    .addSort("rating", SortOrder.DESC)
    .minScore(0.5) // Filter by relevance score
    .build();

Result<SearchResult> result = adapter.search("products", query);
```

### Filtros

```java
SearchQuery query = SearchQuery.builder()
    .query("laptop")
    .type(QueryType.MATCH)
    .field("name")
    .addFilter("category", "Electronics")
    .addFilter("inStock", true)
    .addFilter("price", 1000.0) // price <= 1000
    .build();

Result<SearchResult> result = adapter.search("products", query);
```

### Agregações

#### Terms Aggregation (Buckets)

```java
Aggregation agg = Aggregation.builder()
    .name("categories")
    .type(AggregationType.TERMS)
    .field("category.keyword")
    .size(10)
    .build();

Result<AggregationResult> result = adapter.aggregate("products", agg);

result.ifOk(aggResult -> {
    aggResult.buckets().forEach(bucket -> {
        System.out.println(bucket.key() + ": " + bucket.docCount());
    });
});
```

#### Metric Aggregations

```java
// Average price
Aggregation avgPrice = Aggregation.builder()
    .name("avg_price")
    .type(AggregationType.AVG)
    .field("price")
    .build();

Result<AggregationResult> result = adapter.aggregate("products", avgPrice);

result.ifOk(aggResult -> {
    Double avg = aggResult.value();
    System.out.println("Average price: $" + avg);
});

// Stats aggregation (min, max, avg, sum, count)
Aggregation stats = Aggregation.builder()
    .name("price_stats")
    .type(AggregationType.STATS)
    .field("price")
    .build();

Result<AggregationResult> result = adapter.aggregate("products", stats);
```

### Gerenciamento de Índices

```java
// Criar índice
Result<Boolean> created = adapter.createIndex("products");

// Verificar se existe
Result<Boolean> exists = adapter.indexExists("products");

// Deletar índice
Result<Boolean> deleted = adapter.deleteIndex("products");

// Refresh (tornar documentos visíveis para busca)
Result<Boolean> refreshed = adapter.refresh("products");
```

### CRUD de Documentos

```java
// Get
Result<Optional<Document>> doc = adapter.get("products", "123");

// Update
Map<String, Object> updates = Map.of(
    "price", 899.99,
    "updatedAt", Instant.now().toString()
);
Result<Void> updated = adapter.update("products", "123", updates);

// Delete
Result<Void> deleted = adapter.delete("products", "123");
```

## Result Pattern

Todas as operações retornam `Result<T>` para tratamento robusto de erros:

```java
Result<SearchResult> result = adapter.search("products", query);

// Verificar sucesso
if (result.isOk()) {
    SearchResult searchResult = result.value();
    // Process results
}

// Tratar erro
if (result.isErr()) {
    Problem problem = result.problem();
    System.err.println(problem.message());
}

// Callbacks
result
    .ifOk(searchResult -> {
        // Success handling
    })
    .ifErr(problem -> {
        // Error handling
    });

// Transformação
Result<List<String>> ids = result.map(searchResult ->
    searchResult.hits().stream()
        .map(Document::id)
        .collect(Collectors.toList())
);
```

## Mapeamento de Documentos

### Type-Safe Field Access

```java
Document doc = /* ... */;

// Strings
String name = doc.getString("name");

// Números
Integer quantity = doc.getInt("quantity");
Long views = doc.getLong("views");
Double price = doc.getDouble("price");

// Booleanos
Boolean inStock = doc.getBoolean("inStock");

// Listas
List<Object> tags = doc.getList("tags");

// Raw source
Map<String, Object> source = doc.source();
```

## Casos de Uso

### E-commerce Product Search

```java
// Busca com filtros e ordenação
SearchQuery query = SearchQuery.builder()
    .query("wireless headphones")
    .type(QueryType.MATCH)
    .field("name")
    .addFilter("category", "Electronics")
    .addFilter("inStock", true)
    .addSort("price", SortOrder.ASC)
    .from(0)
    .size(20)
    .minScore(0.3)
    .build();

Result<SearchResult> result = adapter.search("products", query);

// Agregação por categoria
Aggregation categoryAgg = Aggregation.builder()
    .name("categories")
    .type(AggregationType.TERMS)
    .field("category.keyword")
    .size(20)
    .build();

Result<AggregationResult> categories = adapter.aggregate("products", categoryAgg);
```

### Blog Search

```java
// Busca full-text em título e conteúdo
SearchQuery query = SearchQuery.builder()
    .query("microservices architecture")
    .type(QueryType.MATCH)
    .field("title", "content") // Multi-field search
    .addFilter("status", "published")
    .addSort("publishedAt", SortOrder.DESC)
    .from(0)
    .size(10)
    .build();

Result<SearchResult> result = adapter.search("blog_posts", query);
```

### Analytics Dashboard

```java
// Estatísticas de vendas
Aggregation salesStats = Aggregation.builder()
    .name("sales_stats")
    .type(AggregationType.STATS)
    .field("revenue")
    .build();

Result<AggregationResult> stats = adapter.aggregate("orders", salesStats);

stats.ifOk(aggResult -> {
    System.out.println("Total: $" + aggResult.value("sum"));
    System.out.println("Average: $" + aggResult.value("avg"));
    System.out.println("Min: $" + aggResult.value("min"));
    System.out.println("Max: $" + aggResult.value("max"));
});
```

## Error Handling

### Códigos de Erro

| Código              | Descrição                   |
| ------------------- | --------------------------- |
| `INDEX_NOT_FOUND`   | Índice não existe           |
| `DOCUMENT_NOT_FOUND`| Documento não encontrado    |
| `INVALID_QUERY`     | Query inválida              |
| `INDEX_ERROR`       | Erro ao indexar documento   |
| `SEARCH_ERROR`      | Erro ao executar busca      |
| `AGGREGATION_ERROR` | Erro ao executar agregação  |
| `CONNECTION_ERROR`  | Erro de conexão             |
| `TIMEOUT_ERROR`     | Timeout na operação         |

### Tratamento de Erros

```java
Result<SearchResult> result = adapter.search("products", query);

result.ifErr(problem -> {
    switch (problem.code().value()) {
        case "INDEX_NOT_FOUND":
            // Create index
            adapter.createIndex("products");
            break;
        case "CONNECTION_ERROR":
            // Retry or fallback
            break;
        case "TIMEOUT_ERROR":
            // Increase timeout in configuration
            break;
        default:
            logger.error("Search error: {}", problem.message());
    }
});
```

## Configurações de Produção

### Cluster Setup

```java
OpenSearchConfiguration config = OpenSearchConfiguration.builder()
    .addUrl("https://opensearch-1.example.com:9200")
    .addUrl("https://opensearch-2.example.com:9200")
    .addUrl("https://opensearch-3.example.com:9200")
    .username("admin")
    .password(System.getenv("OPENSEARCH_PASSWORD"))
    .connectionTimeout(Duration.ofSeconds(10))
    .socketTimeout(Duration.ofMinutes(1))
    .maxConnections(100) // Higher for production
    .enableSsl(true)
    .verifySslCertificates(true)
    .build();
```

### Connection Pooling

- **Development**: 10 conexões (default)
- **Production**: 50-100 conexões
- **High-traffic**: 100-200 conexões

Configure via `maxConnections()`:

```java
.maxConnections(100) // Adjust based on load
```

### Timeouts

- **Connection timeout**: Tempo para estabelecer conexão (5-10s)
- **Socket timeout**: Tempo para operação completar (30-60s)

```java
.connectionTimeout(Duration.ofSeconds(10))
.socketTimeout(Duration.ofSeconds(60))
```

### SSL/TLS

Sempre habilite em produção:

```java
.enableSsl(true)
.verifySslCertificates(true) // Verify in production
```

### Autenticação

#### Basic Auth

```java
.username(System.getenv("OPENSEARCH_USER"))
.password(System.getenv("OPENSEARCH_PASSWORD"))
```

#### API Key

```java
.apiKeyId(System.getenv("OPENSEARCH_API_KEY_ID"))
.apiKeySecret(System.getenv("OPENSEARCH_API_KEY_SECRET"))
```

## Boas Práticas

### 1. Index Mapping

Defina mapeamentos explícitos para campos:

```bash
PUT /products
{
  "mappings": {
    "properties": {
      "name": {
        "type": "text",
        "fields": {
          "keyword": {"type": "keyword"}
        }
      },
      "price": {"type": "double"},
      "category": {
        "type": "text",
        "fields": {
          "keyword": {"type": "keyword"}
        }
      },
      "inStock": {"type": "boolean"},
      "createdAt": {"type": "date"}
    }
  }
}
```

### 2. Keyword Fields

Use `.keyword` para exact match e agregações:

```java
// Term query (exact match)
.query("Electronics")
.type(QueryType.TERM)
.field("category.keyword")

// Aggregation
Aggregation.builder()
    .type(AggregationType.TERMS)
    .field("category.keyword") // Not "category"
```

### 3. Paginação Eficiente

Evite `from` muito alto (> 10.000):

```java
// OK
.from(0).size(20)
.from(100).size(20)

// Evitar
.from(10000).size(20) // Use search_after instead
```

### 4. Refresh Strategy

```java
// Não refresh após cada write (lento)
adapter.index("products", doc);
// adapter.refresh("products"); // Avoid

// Bulk index + single refresh (rápido)
adapter.bulkIndex("products", documents);
adapter.refresh("products"); // Once at the end
```

### 5. Match vs Term

```java
// Full-text search (analyzed)
QueryType.MATCH → Use para busca de texto livre
QueryType.PHRASE → Use para frases exatas

// Exact match (not analyzed)
QueryType.TERM → Use para IDs, status, categorias (com .keyword)
```

## Performance

### Bulk Indexing

Use `bulkIndex()` para múltiplos documentos (muito mais rápido):

```java
// Lento (1000 requests)
for (Document doc : documents) {
    adapter.index("products", doc);
}

// Rápido (1 request)
adapter.bulkIndex("products", documents);
```

### Batch Size

Recomendações:

- **Small documents** (< 1KB): 1000-5000 docs por batch
- **Medium documents** (1-10KB): 500-1000 docs por batch
- **Large documents** (> 10KB): 100-500 docs por batch

### Refresh Interval

Em produção, configure refresh interval para 30s-60s:

```bash
PUT /products/_settings
{
  "index": {
    "refresh_interval": "30s"
  }
}
```

## Troubleshooting

### Connection Refused

```
Problem: CONNECTION_ERROR - Connection refused
```

**Soluções**:

1. Verificar se OpenSearch está rodando: `curl http://localhost:9200`
2. Verificar URL na configuração
3. Verificar firewall/security groups

### Authentication Failed

```
Problem: AUTHENTICATION_ERROR - Invalid credentials
```

**Soluções**:

1. Verificar username/password
2. Verificar permissões do usuário
3. Testar credenciais: `curl -u user:pass http://localhost:9200`

### Index Not Found

```
Problem: INDEX_NOT_FOUND - Index does not exist
```

**Soluções**:

```java
// Verificar antes de buscar
if (adapter.indexExists("products").value()) {
    adapter.search("products", query);
} else {
    adapter.createIndex("products");
}
```

### Timeout

```
Problem: TIMEOUT_ERROR - Request timeout
```

**Soluções**:

1. Aumentar timeout:
   ```java
   .socketTimeout(Duration.ofMinutes(2))
   ```
2. Otimizar query (reduzir `size`, adicionar filtros)
3. Adicionar índices/replicas no cluster

## Docker Compose

Exemplo para desenvolvimento local:

```yaml
version: '3.8'
services:
  opensearch:
    image: opensearchproject/opensearch:2.12.0
    environment:
      - discovery.type=single-node
      - OPENSEARCH_JAVA_OPTS=-Xms512m -Xmx512m
      - bootstrap.memory_lock=true
      - plugins.security.disabled=true # Dev only
    ports:
      - "9200:9200"
    volumes:
      - opensearch-data:/usr/share/opensearch/data

  opensearch-dashboards:
    image: opensearchproject/opensearch-dashboards:2.12.0
    ports:
      - "5601:5601"
    environment:
      - OPENSEARCH_HOSTS=["http://opensearch:9200"]
      - DISABLE_SECURITY_DASHBOARDS_PLUGIN=true # Dev only

volumes:
  opensearch-data:
```

Iniciar:

```bash
docker-compose up -d
```

Testar:

```bash
curl http://localhost:9200
```

## Referências

- [OpenSearch Documentation](https://opensearch.org/docs/latest/)
- [OpenSearch Java Client](https://opensearch.org/docs/latest/clients/java/)
- [OpenSearch Query DSL](https://opensearch.org/docs/latest/query-dsl/)
- [OpenSearch Aggregations](https://opensearch.org/docs/latest/aggregations/)

## Licença

Este módulo faz parte do projeto Commons Platform.
