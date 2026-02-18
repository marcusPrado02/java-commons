# MongoDB Adapter Guide

## Overview

This guide covers the **MongoDB adapter** (`commons-adapters-persistence-mongodb`) for document-based NoSQL persistence.

**Key Features:**
- Document modeling
- MongoTemplate operations
- MongoRepository pattern
- Aggregation pipelines
- Indexing strategies
- Change streams
- Transactions

---

## 📦 Installation

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-adapters-persistence-mongodb</artifactId>
    <version>${commons.version}</version>
</dependency>

<!-- Spring Boot starter -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb</artifactId>
</dependency>
```

---

## ⚙️ Configuration

### Application Properties

```yaml
# application.yml
spring:
  data:
    mongodb:
      uri: mongodb://${MONGO_USERNAME}:${MONGO_PASSWORD}@localhost:27017/mydb?authSource=admin
      database: mydb
      
      # Connection pool settings
      auto-index-creation: true
      
      # GridFS (large files)
      gridfs:
        database: files
        bucket: files
```

### Replica Set Configuration

```yaml
# application-prod.yml
spring:
  data:
    mongodb:
      uri: mongodb://user:pass@node1:27017,node2:27017,node3:27017/mydb?replicaSet=rs0&authSource=admin
      database: mydb
```

### MongoDB Configuration Class

```java
@Configuration
@EnableMongoRepositories(basePackages = "com.example.infrastructure.persistence.mongo")
public class MongoConfig {
    
    @Bean
    public MongoCustomConversions customConversions() {
        return new MongoCustomConversions(List.of(
            new LocalDateTimeReadConverter(),
            new LocalDateTimeWriteConverter()
        ));
    }
    
    // Custom converters for LocalDateTime
    static class LocalDateTimeWriteConverter implements Converter<LocalDateTime, Date> {
        @Override
        public Date convert(LocalDateTime source) {
            return Date.from(source.atZone(ZoneId.systemDefault()).toInstant());
        }
    }
    
    static class LocalDateTimeReadConverter implements Converter<Date, LocalDateTime> {
        @Override
        public LocalDateTime convert(Date source) {
            return LocalDateTime.ofInstant(source.toInstant(), ZoneId.systemDefault());
        }
    }
}
```

---

## 🗄️ Document Modeling

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
    private LocalDateTime createdAt;
    
    public Result<Void> activate() {
        if (status == ProductStatus.ACTIVE) {
            return Result.error(Error.of(
                "ALREADY_ACTIVE",
                "Product is already active"
            ));
        }
        
        this.status = ProductStatus.ACTIVE;
        registerEvent(new ProductActivatedEvent(id()));
        
        return Result.ok();
    }
}
```

### MongoDB Document

```java
// Infrastructure layer
@Document(collection = "products")
public class ProductDocument {
    
    @Id
    private String id;
    
    @Field("name")
    @Indexed
    private String name;
    
    @Field("description")
    private String description;
    
    @Field("price")
    private PriceDocument price;
    
    @Field("category")
    @Indexed
    private String category;
    
    @Field("tags")
    @Indexed
    private List<String> tags = new ArrayList<>();
    
    @Field("status")
    @Indexed
    private String status;
    
    @Field("created_at")
    @Indexed
    private LocalDateTime createdAt;
    
    @Field("updated_at")
    private LocalDateTime updatedAt;
    
    @Version
    private Long version;  // Optimistic locking
    
    // Embedded document
    public static class PriceDocument {
        @Field("amount")
        private Double amount;
        
        @Field("currency")
        private String currency;
        
        public PriceDocument(Double amount, String currency) {
            this.amount = amount;
            this.currency = currency;
        }
        
        public Money toDomain() {
            return new Money(amount, currency);
        }
        
        public static PriceDocument from(Money money) {
            return new PriceDocument(money.amount(), money.currency());
        }
    }
    
    // Factory method from domain
    public static ProductDocument from(Product product) {
        ProductDocument doc = new ProductDocument();
        doc.id = product.id().value();
        doc.name = product.name();
        doc.description = product.description();
        doc.price = PriceDocument.from(product.price());
        doc.category = product.category().name();
        doc.tags = new ArrayList<>(product.tags());
        doc.status = product.status().name();
        doc.createdAt = product.createdAt();
        doc.updatedAt = LocalDateTime.now();
        return doc;
    }
    
    // Conversion to domain
    public Product toDomain() {
        return Product.reconstruct(
            ProductId.from(id),
            name,
            description,
            price.toDomain(),
            ProductCategory.valueOf(category),
            tags,
            ProductStatus.valueOf(status),
            createdAt
        );
    }
}
```

---

## 📊 Repository Implementation

### MongoRepository

```java
@Repository
public interface ProductMongoRepository extends MongoRepository<ProductDocument, String> {
    
    // Query methods
    List<ProductDocument> findByCategory(String category);
    
    List<ProductDocument> findByStatus(String status);
    
    List<ProductDocument> findByTagsContaining(String tag);
    
    @Query("{ 'price.amount': { $gte: ?0, $lte: ?1 } }")
    List<ProductDocument> findByPriceRange(Double minPrice, Double maxPrice);
    
    @Query("{ 'name': { $regex: ?0, $options: 'i' } }")
    List<ProductDocument> searchByName(String namePattern);
}
```

### Domain Repository Adapter

```java
@Component
public class MongoProductRepository implements ProductRepository {
    
    private final ProductMongoRepository mongoRepository;
    private final MongoTemplate mongoTemplate;
    
    @Override
    public Result<Product> save(Product product) {
        try {
            ProductDocument doc = ProductDocument.from(product);
            ProductDocument saved = mongoRepository.save(doc);
            
            return Result.ok(saved.toDomain());
            
        } catch (Exception e) {
            log.error("Failed to save product")
                .exception(e)
                .field("productId", product.id().value())
                .log();
            
            return Result.error(Error.of("SAVE_ERROR", e.getMessage()));
        }
    }
    
    @Override
    public Optional<Product> findById(ProductId productId) {
        return mongoRepository.findById(productId.value())
            .map(ProductDocument::toDomain);
    }
    
    @Override
    public List<Product> findByCategory(ProductCategory category) {
        return mongoRepository.findByCategory(category.name()).stream()
            .map(ProductDocument::toDomain)
            .toList();
    }
    
    @Override
    public Result<Void> delete(ProductId productId) {
        try {
            mongoRepository.deleteById(productId.value());
            return Result.ok();
        } catch (Exception e) {
            return Result.error(Error.of("DELETE_ERROR", e.getMessage()));
        }
    }
}
```

---

## 🔍 MongoTemplate Operations

### Basic Operations

```java
@Service
public class ProductQueryService {
    
    private final MongoTemplate mongoTemplate;
    
    public List<Product> findActiveProducts() {
        Query query = Query.query(
            Criteria.where("status").is("ACTIVE")
        );
        
        List<ProductDocument> docs = mongoTemplate.find(query, ProductDocument.class);
        
        return docs.stream()
            .map(ProductDocument::toDomain)
            .toList();
    }
    
    public Optional<Product> findByName(String name) {
        Query query = Query.query(
            Criteria.where("name").is(name)
        );
        
        ProductDocument doc = mongoTemplate.findOne(query, ProductDocument.class);
        
        return Optional.ofNullable(doc)
            .map(ProductDocument::toDomain);
    }
    
    public List<Product> findByPriceRange(double minPrice, double maxPrice) {
        Query query = Query.query(
            Criteria.where("price.amount")
                .gte(minPrice)
                .lte(maxPrice)
        );
        
        query.with(Sort.by(Sort.Direction.ASC, "price.amount"));
        
        return mongoTemplate.find(query, ProductDocument.class).stream()
            .map(ProductDocument::toDomain)
            .toList();
    }
}
```

### Complex Queries

```java
@Service
public class ProductSearchService {
    
    private final MongoTemplate mongoTemplate;
    
    public List<Product> searchProducts(ProductSearchCriteria criteria) {
        Criteria mainCriteria = new Criteria();
        List<Criteria> criteriaList = new ArrayList<>();
        
        // Filter by category
        if (criteria.category() != null) {
            criteriaList.add(Criteria.where("category").is(criteria.category()));
        }
        
        // Filter by tags
        if (!criteria.tags().isEmpty()) {
            criteriaList.add(Criteria.where("tags").in(criteria.tags()));
        }
        
        // Filter by price range
        if (criteria.minPrice() != null && criteria.maxPrice() != null) {
            criteriaList.add(
                Criteria.where("price.amount")
                    .gte(criteria.minPrice())
                    .lte(criteria.maxPrice())
            );
        }
        
        // Text search
        if (criteria.searchTerm() != null) {
            criteriaList.add(
                new Criteria().orOperator(
                    Criteria.where("name").regex(criteria.searchTerm(), "i"),
                    Criteria.where("description").regex(criteria.searchTerm(), "i")
                )
            );
        }
        
        if (!criteriaList.isEmpty()) {
            mainCriteria.andOperator(criteriaList.toArray(new Criteria[0]));
        }
        
        Query query = Query.query(mainCriteria);
        
        // Pagination
        query.skip((long) criteria.page() * criteria.size());
        query.limit(criteria.size());
        
        // Sorting
        query.with(Sort.by(Sort.Direction.DESC, "createdAt"));
        
        return mongoTemplate.find(query, ProductDocument.class).stream()
            .map(ProductDocument::toDomain)
            .toList();
    }
}
```

---

## 📈 Aggregation Pipelines

### Simple Aggregation

```java
@Service
public class ProductAnalyticsService {
    
    private final MongoTemplate mongoTemplate;
    
    public Map<String, Long> countProductsByCategory() {
        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.group("category").count().as("count"),
            Aggregation.sort(Sort.Direction.DESC, "count")
        );
        
        AggregationResults<CategoryCount> results = mongoTemplate.aggregate(
            aggregation,
            "products",
            CategoryCount.class
        );
        
        return results.getMappedResults().stream()
            .collect(Collectors.toMap(
                CategoryCount::getCategory,
                CategoryCount::getCount
            ));
    }
    
    public record CategoryCount(String category, Long count) {
        @Field("_id")
        public String getCategory() {
            return category;
        }
        
        public Long getCount() {
            return count;
        }
    }
}
```

### Complex Aggregation

```java
@Service
public class SalesAnalyticsService {
    
    private final MongoTemplate mongoTemplate;
    
    public List<DailySalesReport> getDailySalesReport(
        LocalDateTime start,
        LocalDateTime end
    ) {
        Aggregation aggregation = Aggregation.newAggregation(
            // Match date range
            Aggregation.match(
                Criteria.where("createdAt").gte(start).lte(end)
            ),
            
            // Group by date
            Aggregation.group(
                Aggregation.project()
                    .andExpression("year(createdAt)").as("year")
                    .andExpression("month(createdAt)").as("month")
                    .andExpression("dayOfMonth(createdAt)").as("day")
            )
                .count().as("totalOrders")
                .sum("total").as("totalRevenue")
                .avg("total").as("avgOrderValue"),
            
            // Sort
            Aggregation.sort(Sort.Direction.ASC, "_id.year", "_id.month", "_id.day"),
            
            // Project final shape
            Aggregation.project()
                .andExpression("_id.year").as("year")
                .andExpression("_id.month").as("month")
                .andExpression("_id.day").as("day")
                .and("totalOrders").as("totalOrders")
                .and("totalRevenue").as("totalRevenue")
                .and("avgOrderValue").as("avgOrderValue")
        );
        
        AggregationResults<DailySalesReport> results = mongoTemplate.aggregate(
            aggregation,
            "orders",
            DailySalesReport.class
        );
        
        return results.getMappedResults();
    }
    
    public record DailySalesReport(
        int year,
        int month,
        int day,
        long totalOrders,
        double totalRevenue,
        double avgOrderValue
    ) {}
}
```

---

## 🔐 Indexing Strategies

### Index Declaration

```java
@Document(collection = "products")
@CompoundIndexes({
    @CompoundIndex(
        name = "category_status_idx",
        def = "{'category': 1, 'status': 1}"
    ),
    @CompoundIndex(
        name = "price_category_idx",
        def = "{'price.amount': 1, 'category': 1}"
    )
})
public class ProductDocument {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String sku;
    
    @Indexed
    private String name;
    
    @Indexed
    private String category;
    
    @TextIndexed(weight = 2)  // Text search with weight
    private String description;
    
    // ... other fields
}
```

### Create Indexes Programmatically

```java
@Component
public class MongoIndexInitializer implements ApplicationRunner {
    
    private final MongoTemplate mongoTemplate;
    
    @Override
    public void run(ApplicationArguments args) {
        // Create text index for search
        IndexOperations productIndexOps = mongoTemplate.indexOps(ProductDocument.class);
        
        productIndexOps.ensureIndex(
            new Index()
                .on("name", Sort.Direction.ASC)
                .on("description", Sort.Direction.ASC)
                .named("text_search_idx")
        );
        
        // Create TTL index (auto-delete after 30 days)
        IndexOperations orderIndexOps = mongoTemplate.indexOps(OrderDocument.class);
        
        orderIndexOps.ensureIndex(
            new Index()
                .on("createdAt", Sort.Direction.ASC)
                .expire(Duration.ofDays(30))
                .named("ttl_idx")
        );
        
        log.info("MongoDB indexes created");
    }
}
```

---

## 🔄 Change Streams

### Listen to Changes

```java
@Component
public class ProductChangeListener {
    
    private final MongoTemplate mongoTemplate;
    private final EventPublisher eventPublisher;
    
    @PostConstruct
    public void startListening() {
        ChangeStreamOptions options = ChangeStreamOptions.builder()
            .filter(Aggregation.newAggregation(
                Aggregation.match(Criteria.where("operationType").in("insert", "update"))
            ))
            .build();
        
        Flux<ChangeStreamEvent<ProductDocument>> changeStream =
            mongoTemplate.changeStream(
                "products",
                options,
                ProductDocument.class
            );
        
        changeStream.subscribe(event -> {
            String operationType = event.getOperationType().getValue();
            ProductDocument product = event.getBody();
            
            if (product != null) {
                log.info("Product changed")
                    .field("operationType", operationType)
                    .field("productId", product.getId())
                    .log();
                
                // Publish domain event
                eventPublisher.publish(
                    new ProductChangedEvent(product.toDomain())
                );
            }
        });
    }
}
```

---

## 💼 Transactions

### Multi-Document Transactions

```java
@Service
public class OrderService {
    
    private final MongoTemplate mongoTemplate;
    
    @Transactional
    public Result<Order> createOrderWithInventory(CreateOrderRequest request) {
        try {
            // Deduct inventory
            Query query = Query.query(
                Criteria.where("_id").is(request.productId())
                    .and("stock").gte(request.quantity())
            );
            
            Update update = new Update()
                .inc("stock", -request.quantity());
            
            UpdateResult updateResult = mongoTemplate.updateFirst(
                query,
                update,
                ProductDocument.class
            );
            
            if (updateResult.getModifiedCount() == 0) {
                return Result.error(Error.of(
                    "INSUFFICIENT_STOCK",
                    "Not enough stock available"
                ));
            }
            
            // Create order
            Result<Order> orderResult = Order.create(request);
            
            if (orderResult.isError()) {
                throw new RuntimeException("Order creation failed");
            }
            
            Order order = orderResult.get();
            OrderDocument orderDoc = OrderDocument.from(order);
            
            mongoTemplate.save(orderDoc);
            
            return Result.ok(order);
            
        } catch (Exception e) {
            log.error("Transaction failed").exception(e).log();
            throw e;  // Transaction will rollback
        }
    }
}
```

---

## 🧪 Testing

### Embedded MongoDB Test

```java
@DataMongoTest
@TestPropertySource(properties = {
    "spring.mongodb.embedded.version=4.0.21"
})
class ProductMongoRepositoryTest {
    
    @Autowired
    private ProductMongoRepository repository;
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection(ProductDocument.class);
    }
    
    @Test
    void shouldSaveProduct() {
        // Given
        ProductDocument product = new ProductDocument();
        product.setId(UUID.randomUUID().toString());
        product.setName("Test Product");
        product.setCategory("ELECTRONICS");
        product.setPrice(new ProductDocument.PriceDocument(99.99, "USD"));
        product.setStatus("ACTIVE");
        product.setTags(List.of("test", "electronics"));
        product.setCreatedAt(LocalDateTime.now());
        
        // When
        ProductDocument saved = repository.save(product);
        
        // Then
        Optional<ProductDocument> found = repository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test Product");
    }
    
    @Test
    void shouldFindByCategory() {
        // Given
        ProductDocument product1 = createProduct("Product 1", "ELECTRONICS");
        ProductDocument product2 = createProduct("Product 2", "BOOKS");
        ProductDocument product3 = createProduct("Product 3", "ELECTRONICS");
        
        repository.saveAll(List.of(product1, product2, product3));
        
        // When
        List<ProductDocument> electronics = repository.findByCategory("ELECTRONICS");
        
        // Then
        assertThat(electronics).hasSize(2);
        assertThat(electronics)
            .extracting(ProductDocument::getName)
            .containsExactlyInAnyOrder("Product 1", "Product 3");
    }
    
    private ProductDocument createProduct(String name, String category) {
        ProductDocument doc = new ProductDocument();
        doc.setId(UUID.randomUUID().toString());
        doc.setName(name);
        doc.setCategory(category);
        doc.setPrice(new ProductDocument.PriceDocument(99.99, "USD"));
        doc.setStatus("ACTIVE");
        doc.setCreatedAt(LocalDateTime.now());
        return doc;
    }
}
```

---

## 🚀 Performance Best Practices

### Query Optimization

```java
@Service
public class OptimizedProductService {
    
    private final MongoTemplate mongoTemplate;
    
    // ✅ GOOD: Project only needed fields
    public List<ProductSummary> getProductSummaries() {
        Query query = new Query();
        query.fields()
            .include("name")
            .include("price")
            .include("category");
        
        return mongoTemplate.find(query, ProductDocument.class).stream()
            .map(doc -> new ProductSummary(
                doc.getName(),
                doc.getPrice().getAmount(),
                doc.getCategory()
            ))
            .toList();
    }
    
    // ✅ GOOD: Use covered queries (index-only)
    public List<String> getProductNames() {
        Query query = new Query();
        query.fields().include("name");
        
        return mongoTemplate.find(query, ProductDocument.class).stream()
            .map(ProductDocument::getName)
            .toList();
    }
    
    // ✅ GOOD: Paginate large results
    public Page<Product> findProducts(int page, int size) {
        Query query = new Query();
        query.skip((long) page * size);
        query.limit(size);
        
        long total = mongoTemplate.count(query, ProductDocument.class);
        List<Product> products = mongoTemplate.find(query, ProductDocument.class).stream()
            .map(ProductDocument::toDomain)
            .toList();
        
        return new PageImpl<>(products, PageRequest.of(page, size), total);
    }
}
```

---

## Best Practices

### ✅ DO

```java
// ✅ Use indexes para frequent queries
@Indexed
private String category;

// ✅ Use embedded documents para related data
private Address address;  // Embedded, not referenced

// ✅ Project only needed fields
query.fields().include("name", "price");

// ✅ Use aggregation para complex analytics
Aggregation.newAggregation(group, match, project);

// ✅ Use TTL indexes para temporary data
.expire(Duration.ofDays(30))
```

### ❌ DON'T

```java
// ❌ NÃO fetch large documents unnecessarily
mongoTemplate.findAll(ProductDocument.class);  // ❌ All fields!

// ❌ NÃO use $lookup excessively
// Use embedded documents instead of references

// ❌ NÃO ignore index usage
// Always create indexes for common queries

// ❌ NÃO store huge arrays
private List<Comment> comments;  // ❌ Unbounded growth!

// ❌ NÃO use transactions unnecessarily
@Transactional  // ❌ MongoDB transactions have overhead
```

---

## Ver Também

- [Persistence Port](../api-reference/ports/persistence.md) - Port interface
- [DDD](../api-reference/kernel-ddd.md) - Domain entities
- [NoSQL Patterns](./nosql-patterns.md) - Document modeling
