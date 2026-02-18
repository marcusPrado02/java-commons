# Elasticsearch Adapter Guide

## Overview

This guide covers the **Elasticsearch adapter** (`commons-adapters-search-elasticsearch`) for full-text search and analytics.

**Key Features:**
- Full-text search
- Document indexing
- Aggregations
- Query DSL
- Bulk operations
- Index management
- Highlighting

---

## 📦 Installation

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-adapters-search-elasticsearch</artifactId>
    <version>${commons.version}</version>
</dependency>

<!-- Spring Data Elasticsearch -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
</dependency>
```

---

## ⚙️ Configuration

### Application Properties

```yaml
# application.yml
spring:
  elasticsearch:
    uris: http://localhost:9200
    username: ${ES_USERNAME:elastic}
    password: ${ES_PASSWORD:changeme}
    connection-timeout: 10s
    socket-timeout: 60s
    
  data:
    elasticsearch:
      repositories:
        enabled: true
```

### Elasticsearch Configuration

```java
@Configuration
@EnableElasticsearchRepositories(
    basePackages = "com.example.infrastructure.search.elasticsearch"
)
public class ElasticsearchConfig {
    
    @Value("${spring.elasticsearch.uris}")
    private String elasticsearchUri;
    
    @Bean
    public RestClient restClient() {
        return RestClient.builder(HttpHost.create(elasticsearchUri))
            .setRequestConfigCallback(builder -> builder
                .setConnectTimeout(10000)
                .setSocketTimeout(60000)
            )
            .build();
    }
    
    @Bean
    public ElasticsearchClient elasticsearchClient(RestClient restClient) {
        return new ElasticsearchClient(
            new RestClientTransport(
                restClient,
                new JacksonJsonpMapper()
            )
        );
    }
}
```

---

## 🗃️ Document Mapping

### Domain Entity

```java
// Domain layer
public class Product extends AggregateRoot<ProductId> {
    
    private String name;
    private String description;
    private Money price;
    private ProductCategory category;
    private List<String> tags;
    private ProductStatus status;
    private int stockQuantity;
    private double rating;
    private int reviewCount;
    private LocalDateTime createdAt;
}
```

### Elasticsearch Document

```java
// Infrastructure layer
@Document(indexName = "products")
@Setting(
    replicas = 1,
    shards = 3,
    refreshInterval = "1s"
)
public class ProductDocument {
    
    @Id
    private String id;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String name;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;
    
    @Field(type = FieldType.Double)
    private Double price;
    
    @Field(type = FieldType.Keyword)
    private String category;
    
    @Field(type = FieldType.Keyword)
    private List<String> tags;
    
    @Field(type = FieldType.Keyword)
    private String status;
    
    @Field(type = FieldType.Integer)
    private Integer stockQuantity;
    
    @Field(type = FieldType.Double)
    private Double rating;
    
    @Field(type = FieldType.Integer)
    private Integer reviewCount;
    
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime createdAt;
    
    // Nested object
    @Field(type = FieldType.Nested)
    private List<ReviewDocument> reviews;
    
    public static class ReviewDocument {
        @Field(type = FieldType.Keyword)
        private String userId;
        
        @Field(type = FieldType.Double)
        private Double rating;
        
        @Field(type = FieldType.Text)
        private String comment;
        
        @Field(type = FieldType.Date)
        private LocalDateTime createdAt;
    }
    
    // Factory method from domain
    public static ProductDocument from(Product product) {
        ProductDocument doc = new ProductDocument();
        doc.id = product.id().value();
        doc.name = product.name();
        doc.description = product.description();
        doc.price = product.price().amount();
        doc.category = product.category().name();
        doc.tags = product.tags();
        doc.status = product.status().name();
        doc.stockQuantity = product.stockQuantity();
        doc.rating = product.rating();
        doc.reviewCount = product.reviewCount();
        doc.createdAt = product.createdAt();
        return doc;
    }
    
    // Conversion to domain
    public Product toDomain() {
        return Product.reconstruct(
            ProductId.from(id),
            name,
            description,
            new Money(price, "USD"),
            ProductCategory.valueOf(category),
            tags,
            ProductStatus.valueOf(status),
            stockQuantity,
            rating,
            reviewCount,
            createdAt
        );
    }
}
```

---

## 📊 Repository Implementation

### Elasticsearch Repository

```java
@Repository
public interface ProductSearchRepository extends 
    ElasticsearchRepository<ProductDocument, String> {
    
    // Query methods
    List<ProductDocument> findByCategory(String category);
    
    List<ProductDocument> findByStatus(String status);
    
    List<ProductDocument> findByTagsContaining(String tag);
    
    List<ProductDocument> findByPriceBetween(Double minPrice, Double maxPrice);
    
    @Query("{\"bool\": {\"must\": [{\"match\": {\"name\": \"?0\"}}]}}")
    List<ProductDocument> searchByName(String name);
}
```

### Domain Search Adapter

```java
@Component
public class ElasticsearchProductSearchService implements ProductSearchService {
    
    private final ProductSearchRepository searchRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    
    @Override
    public Result<Void> index(Product product) {
        try {
            ProductDocument doc = ProductDocument.from(product);
            searchRepository.save(doc);
            
            log.info("Product indexed")
                .field("productId", product.id().value())
                .log();
            
            return Result.ok();
            
        } catch (Exception e) {
            log.error("Failed to index product")
                .exception(e)
                .log();
            
            return Result.error(Error.of("INDEX_ERROR", e.getMessage()));
        }
    }
    
    @Override
    public List<Product> search(String query) {
        Criteria criteria = new Criteria("name")
            .matches(query)
            .or("description")
            .matches(query);
        
        Query searchQuery = new CriteriaQuery(criteria);
        
        SearchHits<ProductDocument> hits = elasticsearchOperations.search(
            searchQuery,
            ProductDocument.class
        );
        
        return hits.stream()
            .map(SearchHit::getContent)
            .map(ProductDocument::toDomain)
            .toList();
    }
    
    @Override
    public void delete(ProductId productId) {
        searchRepository.deleteById(productId.value());
    }
}
```

---

## 🔍 Search Queries

### Full-Text Search

```java
@Service
public class ProductSearchService {
    
    private final ElasticsearchOperations operations;
    
    public List<Product> fullTextSearch(String searchTerm) {
        Query query = NativeQuery.builder()
            .withQuery(q -> q
                .multiMatch(m -> m
                    .query(searchTerm)
                    .fields("name^3", "description^2", "tags")  // Boosting
                    .type(TextQueryType.BestFields)
                    .fuzziness("AUTO")
                )
            )
            .build();
        
        SearchHits<ProductDocument> hits = operations.search(
            query,
            ProductDocument.class
        );
        
        return hits.stream()
            .map(SearchHit::getContent)
            .map(ProductDocument::toDomain)
            .toList();
    }
}
```

### Boolean Queries

```java
@Service
public class ComplexSearchService {
    
    private final ElasticsearchOperations operations;
    
    public List<Product> advancedSearch(SearchCriteria criteria) {
        Query query = NativeQuery.builder()
            .withQuery(q -> q
                .bool(b -> {
                    // Must match
                    if (criteria.category() != null) {
                        b.must(m -> m
                            .term(t -> t
                                .field("category")
                                .value(criteria.category())
                            )
                        );
                    }
                    
                    // Should match (at least one)
                    if (criteria.searchTerm() != null) {
                        b.should(s -> s
                            .match(m -> m
                                .field("name")
                                .query(criteria.searchTerm())
                            )
                        );
                        b.should(s -> s
                            .match(m -> m
                                .field("description")
                                .query(criteria.searchTerm())
                            )
                        );
                    }
                    
                    // Filter (no scoring)
                    if (criteria.minPrice() != null) {
                        b.filter(f -> f
                            .range(r -> r
                                .field("price")
                                .gte(JsonData.of(criteria.minPrice()))
                            )
                        );
                    }
                    
                    if (criteria.maxPrice() != null) {
                        b.filter(f -> f
                            .range(r -> r
                                .field("price")
                                .lte(JsonData.of(criteria.maxPrice()))
                            )
                        );
                    }
                    
                    // Must not
                    if (criteria.excludeOutOfStock()) {
                        b.mustNot(mn -> mn
                            .term(t -> t
                                .field("stockQuantity")
                                .value(0)
                            )
                        );
                    }
                    
                    return b;
                })
            )
            .withSort(Sort.by(Sort.Direction.DESC, "rating"))
            .withPageable(PageRequest.of(0, 20))
            .build();
        
        SearchHits<ProductDocument> hits = operations.search(
            query,
            ProductDocument.class
        );
        
        return hits.stream()
            .map(SearchHit::getContent)
            .map(ProductDocument::toDomain)
            .toList();
    }
}
```

### Highlighting

```java
@Service
public class HighlightSearchService {
    
    private final ElasticsearchOperations operations;
    
    public List<ProductSearchResult> searchWithHighlights(String searchTerm) {
        Query query = NativeQuery.builder()
            .withQuery(q -> q
                .multiMatch(m -> m
                    .query(searchTerm)
                    .fields("name", "description")
                )
            )
            .withHighlightQuery(
                new HighlightQuery(
                    new Highlight(
                        HighlightParameters.builder()
                            .withPreTags("<mark>")
                            .withPostTags("</mark>")
                            .build(),
                        List.of(
                            new HighlightField("name"),
                            new HighlightField("description")
                        )
                    ),
                    ProductDocument.class
                )
            )
            .build();
        
        SearchHits<ProductDocument> hits = operations.search(
            query,
            ProductDocument.class
        );
        
        return hits.stream()
            .map(hit -> {
                ProductDocument doc = hit.getContent();
                Map<String, List<String>> highlights = hit.getHighlightFields();
                
                return new ProductSearchResult(
                    doc.toDomain(),
                    highlights.getOrDefault("name", List.of()),
                    highlights.getOrDefault("description", List.of())
                );
            })
            .toList();
    }
}
```

---

## 📈 Aggregations

### Simple Aggregations

```java
@Service
public class ProductAnalyticsService {
    
    private final ElasticsearchOperations operations;
    
    public Map<String, Long> countByCategory() {
        Query query = NativeQuery.builder()
            .withAggregation("categories", Aggregation.of(a -> a
                .terms(t -> t
                    .field("category")
                    .size(100)
                )
            ))
            .build();
        
        SearchHits<ProductDocument> hits = operations.search(
            query,
            ProductDocument.class
        );
        
        Aggregations aggregations = hits.getAggregations();
        
        if (aggregations == null) {
            return Map.of();
        }
        
        ElasticsearchAggregation categoriesAgg = 
            (ElasticsearchAggregation) aggregations.get("categories");
        
        StringTermsAggregate terms = categoriesAgg.aggregation()
            .getAggregate()
            .sterms();
        
        return terms.buckets().array().stream()
            .collect(Collectors.toMap(
                b -> b.key().stringValue(),
                b -> b.docCount()
            ));
    }
}
```

### Complex Aggregations

```java
@Service
public class SalesAnalyticsService {
    
    private final ElasticsearchOperations operations;
    
    public PriceStatistics getPriceStatistics(String category) {
        Query query = NativeQuery.builder()
            .withQuery(q -> q
                .term(t -> t
                    .field("category")
                    .value(category)
                )
            )
            .withAggregation("price_stats", Aggregation.of(a -> a
                .stats(s -> s
                    .field("price")
                )
            ))
            .build();
        
        SearchHits<ProductDocument> hits = operations.search(
            query,
            ProductDocument.class
        );
        
        ElasticsearchAggregation statsAgg = 
            (ElasticsearchAggregation) hits.getAggregations().get("price_stats");
        
        StatsAggregate stats = statsAgg.aggregation()
            .getAggregate()
            .stats();
        
        return new PriceStatistics(
            stats.count(),
            stats.min(),
            stats.max(),
            stats.avg(),
            stats.sum()
        );
    }
    
    public List<DateHistogramBucket> getSalesByMonth(int year) {
        Query query = NativeQuery.builder()
            .withQuery(q -> q
                .range(r -> r
                    .field("createdAt")
                    .gte(JsonData.of(year + "-01-01"))
                    .lte(JsonData.of(year + "-12-31"))
                )
            )
            .withAggregation("sales_by_month", Aggregation.of(a -> a
                .dateHistogram(d -> d
                    .field("createdAt")
                    .calendarInterval(CalendarInterval.Month)
                    .format("yyyy-MM")
                )
            ))
            .build();
        
        SearchHits<ProductDocument> hits = operations.search(
            query,
            ProductDocument.class
        );
        
        ElasticsearchAggregation monthlyAgg = 
            (ElasticsearchAggregation) hits.getAggregations().get("sales_by_month");
        
        DateHistogramAggregate histogram = monthlyAgg.aggregation()
            .getAggregate()
            .dateHistogram();
        
        return histogram.buckets().array().stream()
            .map(bucket -> new DateHistogramBucket(
                bucket.keyAsString(),
                bucket.docCount()
            ))
            .toList();
    }
}
```

---

## 🔄 Bulk Operations

### Bulk Indexing

```java
@Service
public class BulkIndexService {
    
    private final ElasticsearchOperations operations;
    
    public Result<Void> bulkIndex(List<Product> products) {
        try {
            List<IndexQuery> queries = products.stream()
                .map(product -> {
                    ProductDocument doc = ProductDocument.from(product);
                    
                    return new IndexQueryBuilder()
                        .withId(doc.getId())
                        .withObject(doc)
                        .build();
                })
                .toList();
            
            operations.bulkIndex(queries, ProductDocument.class);
            
            log.info("Bulk indexed products")
                .field("count", products.size())
                .log();
            
            return Result.ok();
            
        } catch (Exception e) {
            log.error("Bulk index failed")
                .exception(e)
                .log();
            
            return Result.error(Error.of("BULK_INDEX_ERROR", e.getMessage()));
        }
    }
}
```

### Update By Query

```java
@Service
public class BulkUpdateService {
    
    private final ElasticsearchClient client;
    
    public Result<Long> updatePricesByCategory(
        String category,
        double priceMultiplier
    ) {
        try {
            UpdateByQueryResponse response = client.updateByQuery(u -> u
                .index("products")
                .query(q -> q
                    .term(t -> t
                        .field("category")
                        .value(category)
                    )
                )
                .script(s -> s
                    .inline(i -> i
                        .source("ctx._source.price = ctx._source.price * params.multiplier")
                        .params("multiplier", JsonData.of(priceMultiplier))
                    )
                )
            );
            
            log.info("Updated prices")
                .field("category", category)
                .field("updated", response.updated())
                .log();
            
            return Result.ok(response.updated());
            
        } catch (Exception e) {
            log.error("Update by query failed")
                .exception(e)
                .log();
            
            return Result.error(Error.of("UPDATE_ERROR", e.getMessage()));
        }
    }
}
```

---

## 🗂️ Index Management

### Create Index with Mapping

```java
@Service
public class IndexManagementService {
    
    private final ElasticsearchClient client;
    
    public Result<Void> createProductIndex() {
        try {
            client.indices().create(c -> c
                .index("products")
                .settings(s -> s
                    .numberOfShards("3")
                    .numberOfReplicas("1")
                    .refreshInterval(t -> t.time("1s"))
                )
                .mappings(m -> m
                    .properties("name", p -> p
                        .text(t -> t
                            .analyzer("standard")
                            .fields("keyword", f -> f
                                .keyword(k -> k)
                            )
                        )
                    )
                    .properties("description", p -> p
                        .text(t -> t.analyzer("standard"))
                    )
                    .properties("price", p -> p
                        .double_(d -> d)
                    )
                    .properties("category", p -> p
                        .keyword(k -> k)
                    )
                    .properties("tags", p -> p
                        .keyword(k -> k)
                    )
                    .properties("createdAt", p -> p
                        .date(d -> d)
                    )
                )
            );
            
            log.info("Created index: products");
            
            return Result.ok();
            
        } catch (Exception e) {
            log.error("Failed to create index")
                .exception(e)
                .log();
            
            return Result.error(Error.of("CREATE_INDEX_ERROR", e.getMessage()));
        }
    }
    
    public Result<Void> deleteIndex(String indexName) {
        try {
            client.indices().delete(d -> d.index(indexName));
            log.info("Deleted index: {}", indexName);
            return Result.ok();
            
        } catch (Exception e) {
            return Result.error(Error.of("DELETE_INDEX_ERROR", e.getMessage()));
        }
    }
}
```

---

## 🧪 Testing

### Testcontainers

```java
@SpringBootTest
@Testcontainers
class ProductSearchServiceTest {
    
    @Container
    static ElasticsearchContainer elasticsearch = 
        new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.11.0")
            .withEnv("xpack.security.enabled", "false");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.elasticsearch.uris", elasticsearch::getHttpHostAddress);
    }
    
    @Autowired
    private ProductSearchRepository repository;
    
    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }
    
    @Test
    void shouldSearchByName() {
        // Given
        ProductDocument product1 = createProduct("Laptop", "ELECTRONICS");
        ProductDocument product2 = createProduct("Phone", "ELECTRONICS");
        ProductDocument product3 = createProduct("Book", "BOOKS");
        
        repository.saveAll(List.of(product1, product2, product3));
        
        // Wait for indexing
        await().atMost(5, TimeUnit.SECONDS)
            .until(() -> repository.count() == 3);
        
        // When
        List<ProductDocument> results = repository.searchByName("Laptop");
        
        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Laptop");
    }
}
```

---

## Best Practices

### ✅ DO

```java
// ✅ Use multi-field mappings
@Field(type = FieldType.Text, analyzer = "standard")
@Field(name = "keyword", type = FieldType.Keyword)

// ✅ Use bulk operations
operations.bulkIndex(queries, ProductDocument.class);

// ✅ Use filters in bool queries (no scoring)
.filter(f -> f.range(r -> r.field("price").gte(10)))

// ✅ Boost important fields
.multiMatch(m -> m.fields("name^3", "description^2"))

// ✅ Use aggregations for analytics
.withAggregation("categories", terms("category"))
```

### ❌ DON'T

```java
// ❌ NÃO index sem análise
@Field(type = FieldType.Keyword)  // ❌ No full-text!

// ❌ NÃO fetch all documents
repository.findAll();  // ❌ Use pagination!

// ❌ NÃO ignore relevance scoring
// Use boost for important fields

// ❌ NÃO use wildcard queries excessively
.wildcard(w -> w.value("*term*"))  // ❌ Slow!

// ❌ NÃO ignore index management
// Monitor shard health, optimize indices
```

---

## Ver Também

- [Search Port](../api-reference/ports/search.md) - Port interface
- [Full-Text Search](../guides/search-patterns.md) - Search patterns
- [OpenSearch Adapter](./opensearch.md) - Compatible alternative
