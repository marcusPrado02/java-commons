# Port: Search

## Visão Geral

`commons-ports-search` define contratos para busca full-text e analytics, abstraindo engines como Elasticsearch e OpenSearch.

**Quando usar:**
- Busca full-text em produtos/documentos
- Autocomplete e suggestions
- Faceted search (filtros)
- Analytics e aggregations
- Relevance scoring

**Implementações disponíveis:**
- `commons-adapters-search-elasticsearch` - Elasticsearch
- `commons-adapters-search-opensearch` - OpenSearch

---

## 📦 Instalação

```xml
<!-- Port (interface) -->
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-ports-search</artifactId>
    <version>${commons.version}</version>
</dependency>

<!-- Adapter -->
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-adapters-search-elasticsearch</artifactId>
    <version>${commons.version}</version>
</dependency>
```

---

## 🔍 SearchEngine Interface

### Core Methods

```java
public interface SearchEngine {
    
    /**
     * Indexa documento.
     */
    Result<IndexResponse> index(
        String indexName,
        String documentId,
        Map<String, Object> document
    );
    
    /**
     * Busca documentos.
     */
    Result<SearchResponse> search(SearchRequest request);
    
    /**
     * Busca com aggregations.
     */
    Result<SearchResponse> searchWithAggregations(
        SearchRequest request,
        List<Aggregation> aggregations
    );
    
    /**
     * Deleta documento.
     */
    Result<Void> delete(String indexName, String documentId);
    
    /**
     * Bulk operations.
     */
    Result<BulkResponse> bulk(List<BulkOperation> operations);
}
```

### Search Request Model

```java
public record SearchRequest(
    String indexName,
    Query query,
    List<SortField> sort,
    int from,
    int size,
    List<String> fields
) {
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        public Builder index(String indexName);
        public Builder query(Query query);
        public Builder sort(String field, SortOrder order);
        public Builder from(int from);
        public Builder size(int size);
        public Builder fields(String... fields);
        public SearchRequest build();
    }
}
```

### Query Types

```java
public sealed interface Query permits
    MatchAllQuery,
    MatchQuery,
    MultiMatchQuery,
    TermQuery,
    RangeQuery,
    BoolQuery,
    FuzzyQuery {}

// Match all documents
public record MatchAllQuery() implements Query {}

// Full-text match
public record MatchQuery(String field, String text) implements Query {}

// Match across multiple fields
public record MultiMatchQuery(List<String> fields, String text) implements Query {}

// Exact term match
public record TermQuery(String field, String value) implements Query {}

// Range query
public record RangeQuery(
    String field,
    Optional<Object> gte, // Greater than or equal
    Optional<Object> lte, // Less than or equal
    Optional<Object> gt,  // Greater than
    Optional<Object> lt   // Less than
) implements Query {}

// Boolean combination
public record BoolQuery(
    List<Query> must,
    List<Query> should,
    List<Query> mustNot,
    Optional<Integer> minimumShouldMatch
) implements Query {}

// Fuzzy match (typo-tolerant)
public record FuzzyQuery(
    String field,
    String text,
    int fuzziness  // Edit distance (0-2)
) implements Query {}
```

---

## 🛍️ Product Search Example

### Product Index

```java
@Service
public class ProductIndexService {
    
    private final SearchEngine searchEngine;
    private static final String INDEX_NAME = "products";
    
    public Result<Void> indexProduct(Product product) {
        Map<String, Object> document = Map.of(
            "id", product.id().value(),
            "name", product.name(),
            "description", product.description(),
            "category", product.category().name(),
            "price", product.price().amount(),
            "currency", product.price().currency(),
            "inStock", product.inStock(),
            "brand", product.brand(),
            "tags", product.tags(),
            "createdAt", product.createdAt().toString()
        );
        
        return searchEngine.index(INDEX_NAME, product.id().value(), document)
            .andThen(response -> {
                log.info("Product indexed")
                    .field("productId", product.id().value())
                    .field("indexName", INDEX_NAME)
                    .log();
            })
            .mapToVoid();
    }
    
    public Result<Void> deleteProduct(ProductId productId) {
        return searchEngine.delete(INDEX_NAME, productId.value());
    }
}
```

### Product Search Service

```java
@Service
public class ProductSearchService {
    
    private final SearchEngine searchEngine;
    
    public Result<SearchResults<Product>> searchProducts(
        String searchText,
        int page,
        int pageSize
    ) {
        // Multi-field search (name, description, brand)
        MultiMatchQuery query = new MultiMatchQuery(
            List.of("name^2", "description", "brand"),  // ^2 = boost name 2x
            searchText
        );
        
        SearchRequest request = SearchRequest.builder()
            .index("products")
            .query(query)
            .from(page * pageSize)
            .size(pageSize)
            .sort("_score", SortOrder.DESC)  // Relevance score
            .build();
        
        return searchEngine.search(request)
            .map(response -> new SearchResults<>(
                response.hits().stream()
                    .map(this::mapToProduct)
                    .toList(),
                response.total(),
                page,
                pageSize
            ));
    }
    
    private Product mapToProduct(SearchHit hit) {
        Map<String, Object> source = hit.source();
        return Product.reconstruct(
            ProductId.from((String) source.get("id")),
            (String) source.get("name"),
            (String) source.get("description"),
            new Money(
                (Double) source.get("price"),
                (String) source.get("currency")
            ),
            (Boolean) source.get("inStock")
        );
    }
}

public record SearchResults<T>(
    List<T> items,
    long total,
    int page,
    int pageSize
) {
    public int totalPages() {
        return (int) Math.ceil((double) total / pageSize);
    }
    
    public boolean hasNext() {
        return page < totalPages() - 1;
    }
}
```

---

## 🎯 Filtered Search

### Faceted Search

```java
@Service
public class ProductFilterService {
    
    private final SearchEngine searchEngine;
    
    public Result<SearchResults<Product>> searchWithFilters(
        String searchText,
        ProductFilters filters,
        int page,
        int pageSize
    ) {
        // Build boolean query
        BoolQuery query = buildFilterQuery(searchText, filters);
        
        SearchRequest request = SearchRequest.builder()
            .index("products")
            .query(query)
            .from(page * pageSize)
            .size(pageSize)
            .sort("_score", SortOrder.DESC)
            .build();
        
        // Define aggregations for facets
        List<Aggregation> aggregations = List.of(
            new TermsAggregation("categories", "category", 10),
            new TermsAggregation("brands", "brand", 20),
            new RangeAggregation("price_ranges", "price", List.of(
                new Range("0-50", Optional.of(0.0), Optional.of(50.0)),
                new Range("50-100", Optional.of(50.0), Optional.of(100.0)),
                new Range("100-200", Optional.of(100.0), Optional.of(200.0)),
                new Range("200+", Optional.of(200.0), Optional.empty())
            ))
        );
        
        return searchEngine.searchWithAggregations(request, aggregations)
            .map(response -> new SearchResults<>(
                response.hits().stream()
                    .map(this::mapToProduct)
                    .toList(),
                response.total(),
                page,
                pageSize,
                response.aggregations()  // Facets for filtering
            ));
    }
    
    private BoolQuery buildFilterQuery(String searchText, ProductFilters filters) {
        List<Query> must = new ArrayList<>();
        
        // Text search
        if (searchText != null && !searchText.isBlank()) {
            must.add(new MultiMatchQuery(
                List.of("name^2", "description", "brand"),
                searchText
            ));
        }
        
        // Category filter
        if (filters.category().isPresent()) {
            must.add(new TermQuery("category", filters.category().get()));
        }
        
        // Brand filter
        if (!filters.brands().isEmpty()) {
            List<Query> brandQueries = filters.brands().stream()
                .map(brand -> new TermQuery("brand", brand))
                .map(q -> (Query) q)
                .toList();
            must.add(new BoolQuery(List.of(), brandQueries, List.of(), Optional.of(1)));
        }
        
        // Price range
        if (filters.minPrice().isPresent() || filters.maxPrice().isPresent()) {
            must.add(new RangeQuery(
                "price",
                filters.minPrice().map(p -> (Object) p),
                filters.maxPrice().map(p -> (Object) p),
                Optional.empty(),
                Optional.empty()
            ));
        }
        
        // In stock
        if (filters.inStockOnly()) {
            must.add(new TermQuery("inStock", true));
        }
        
        return new BoolQuery(must, List.of(), List.of(), Optional.empty());
    }
}

public record ProductFilters(
    Optional<String> category,
    List<String> brands,
    Optional<Double> minPrice,
    Optional<Double> maxPrice,
    boolean inStockOnly
) {
    public static ProductFilters empty() {
        return new ProductFilters(
            Optional.empty(),
            List.of(),
            Optional.empty(),
            Optional.empty(),
            false
        );
    }
}
```

---

## 🔄 Autocomplete

### Suggestion Service

```java
@Service
public class ProductSuggestionService {
    
    private final SearchEngine searchEngine;
    
    public Result<List<String>> suggestProducts(String prefix) {
        // Prefix query for autocomplete
        Query query = new PrefixQuery("name", prefix);
        
        SearchRequest request = SearchRequest.builder()
            .index("products")
            .query(query)
            .size(10)
            .fields("name")
            .sort("_score", SortOrder.DESC)
            .build();
        
        return searchEngine.search(request)
            .map(response -> response.hits().stream()
                .map(hit -> (String) hit.source().get("name"))
                .distinct()
                .toList()
            );
    }
    
    public Result<List<String>> fuzzySearch(String searchText) {
        // Fuzzy query for typo tolerance
        FuzzyQuery query = new FuzzyQuery(
            "name",
            searchText,
            2  // Allow up to 2 character differences
        );
        
        SearchRequest request = SearchRequest.builder()
            .index("products")
            .query(query)
            .size(10)
            .fields("name")
            .build();
        
        return searchEngine.search(request)
            .map(response -> response.hits().stream()
                .map(hit -> (String) hit.source().get("name"))
                .toList()
            );
    }
}
```

---

## 📊 Aggregations

### Aggregation Types

```java
public sealed interface Aggregation permits
    TermsAggregation,
    RangeAggregation,
    DateHistogramAggregation,
    StatsAggregation {}

// Count documents per term
public record TermsAggregation(
    String name,
    String field,
    int size
) implements Aggregation {}

// Count documents in ranges
public record RangeAggregation(
    String name,
    String field,
    List<Range> ranges
) implements Aggregation {}

public record Range(
    String key,
    Optional<Double> from,
    Optional<Double> to
) {}

// Time-based histogram
public record DateHistogramAggregation(
    String name,
    String field,
    DateInterval interval  // DAY, WEEK, MONTH, YEAR
) implements Aggregation {}

// Statistics (min, max, avg, sum)
public record StatsAggregation(
    String name,
    String field
) implements Aggregation {}
```

### Analytics Example

```java
@Service
public class ProductAnalyticsService {
    
    private final SearchEngine searchEngine;
    
    public Result<ProductStats> getProductStats() {
        SearchRequest request = SearchRequest.builder()
            .index("products")
            .query(new MatchAllQuery())
            .size(0)  // We only want aggregations, not hits
            .build();
        
        List<Aggregation> aggregations = List.of(
            new TermsAggregation("top_categories", "category", 10),
            new TermsAggregation("top_brands", "brand", 10),
            new StatsAggregation("price_stats", "price"),
            new DateHistogramAggregation("created_over_time", "createdAt", DateInterval.DAY)
        );
        
        return searchEngine.searchWithAggregations(request, aggregations)
            .map(response -> {
                Map<String, AggregationResult> aggs = response.aggregations();
                
                return new ProductStats(
                    extractTerms(aggs.get("top_categories")),
                    extractTerms(aggs.get("top_brands")),
                    extractStats(aggs.get("price_stats")),
                    extractHistogram(aggs.get("created_over_time"))
                );
            });
    }
    
    private List<TermCount> extractTerms(AggregationResult result) {
        return result.buckets().stream()
            .map(bucket -> new TermCount(
                bucket.key(),
                bucket.docCount()
            ))
            .toList();
    }
}

public record ProductStats(
    List<TermCount> topCategories,
    List<TermCount> topBrands,
    PriceStats priceStats,
    List<DateCount> createdOverTime
) {}

public record TermCount(String term, long count) {}
public record PriceStats(double min, double max, double avg, double sum) {}
public record DateCount(LocalDate date, long count) {}
```

---

## 🔄 Bulk Operations

### Bulk Indexing

```java
@Service
public class BulkProductIndexService {
    
    private final SearchEngine searchEngine;
    private static final int BATCH_SIZE = 1000;
    
    public Result<Void> reindexAllProducts(List<Product> products) {
        // Split into batches
        List<List<Product>> batches = Lists.partition(products, BATCH_SIZE);
        
        for (List<Product> batch : batches) {
            Result<BulkResponse> result = indexBatch(batch);
            
            if (result.isError()) {
                return result.mapToVoid();
            }
            
            BulkResponse response = result.get();
            if (response.hasErrors()) {
                log.error("Bulk indexing errors")
                    .field("errors", response.errors().size())
                    .log();
                return Result.error(Error.of(
                    "BULK_INDEX_ERROR",
                    "Failed to index " + response.errors().size() + " products"
                ));
            }
        }
        
        return Result.ok();
    }
    
    private Result<BulkResponse> indexBatch(List<Product> products) {
        List<BulkOperation> operations = products.stream()
            .map(product -> new BulkOperation.Index(
                "products",
                product.id().value(),
                mapToDocument(product)
            ))
            .map(op -> (BulkOperation) op)
            .toList();
        
        return searchEngine.bulk(operations);
    }
}

public sealed interface BulkOperation permits Index, Update, Delete {
    
    record Index(
        String indexName,
        String documentId,
        Map<String, Object> document
    ) implements BulkOperation {}
    
    record Update(
        String indexName,
        String documentId,
        Map<String, Object> updates
    ) implements BulkOperation {}
    
    record Delete(
        String indexName,
        String documentId
    ) implements BulkOperation {}
}
```

---

## 🧪 Testing

### Mock Search Engine

```java
public class MockSearchEngine implements SearchEngine {
    
    private final Map<String, Map<String, Map<String, Object>>> indices = new HashMap<>();
    
    @Override
    public Result<IndexResponse> index(
        String indexName,
        String documentId,
        Map<String, Object> document
    ) {
        indices
            .computeIfAbsent(indexName, k -> new HashMap<>())
            .put(documentId, document);
        
        return Result.ok(new IndexResponse(documentId));
    }
    
    @Override
    public Result<SearchResponse> search(SearchRequest request) {
        Map<String, Map<String, Object>> index = indices.get(request.indexName());
        
        if (index == null) {
            return Result.ok(SearchResponse.empty());
        }
        
        // Simple in-memory matching
        List<SearchHit> hits = index.entrySet().stream()
            .filter(entry -> matches(entry.getValue(), request.query()))
            .map(entry -> new SearchHit(entry.getKey(), entry.getValue(), 1.0))
            .toList();
        
        return Result.ok(new SearchResponse(hits, hits.size(), Map.of()));
    }
    
    private boolean matches(Map<String, Object> document, Query query) {
        return switch (query) {
            case MatchAllQuery q -> true;
            case MatchQuery q -> {
                Object value = document.get(q.field());
                yield value != null && value.toString().contains(q.text());
            }
            case TermQuery q -> {
                Object value = document.get(q.field());
                yield value != null && value.equals(q.value());
            }
            default -> true;
        };
    }
}
```

### Test Example

```java
class ProductSearchServiceTest {
    
    private MockSearchEngine searchEngine;
    private ProductSearchService searchService;
    
    @BeforeEach
    void setUp() {
        searchEngine = new MockSearchEngine();
        searchService = new ProductSearchService(searchEngine);
        
        // Index test products
        indexProduct("1", "Apple iPhone 15", "Electronics", 999.0);
        indexProduct("2", "Samsung Galaxy S24", "Electronics", 899.0);
        indexProduct("3", "Sony Headphones", "Electronics", 299.0);
    }
    
    @Test
    void shouldSearchProducts() {
        // When
        Result<SearchResults<Product>> result = searchService.searchProducts(
            "iPhone",
            0,
            10
        );
        
        // Then
        assertThat(result.isOk()).isTrue();
        
        SearchResults<Product> results = result.get();
        assertThat(results.total()).isEqualTo(1);
        assertThat(results.items().get(0).name()).isEqualTo("Apple iPhone 15");
    }
}
```

---

## Best Practices

### ✅ DO

```java
// ✅ Use multi-field search com boost
new MultiMatchQuery(List.of("name^2", "description"), text);

// ✅ Pagine resultados
.from(page * pageSize).size(pageSize)

// ✅ Use aggregations para facets
searchEngine.searchWithAggregations(request, aggregations);

// ✅ Bulk operations para performance
searchEngine.bulk(operations);

// ✅ Fuzzy search para typos
new FuzzyQuery("name", text, 2);
```

### ❌ DON'T

```java
// ❌ NÃO busque sem limit
.size(10000);  // ❌ Performance!

// ❌ NÃO ignore aggregations
SearchResponse response = engine.search(request);  // ❌ No facets!

// ❌ NÃO faça index individual em loop
for (product : products) {
    engine.index(index, id, doc);  // ❌ Use bulk!
}

// ❌ NÃO use exact match para texto
new TermQuery("description", text);  // ❌ Use MatchQuery!

// ❌ NÃO exponha IDs internos
return hit.source().get("_id");  // ❌ Use domain ID!
```

---

## Ver Também

- [Elasticsearch Adapter](../../../commons-adapters-search-elasticsearch/) - Full implementation
- [Persistence](./persistence.md) - Database operations
- [Cache](./cache.md) - Performance optimization
