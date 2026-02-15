# Elasticsearch Adapter

Elasticsearch adapter implementation of the `SearchPort` interface for full-text search capabilities.

## Features

- **Document Indexing**: Single and bulk document indexing operations
- **Full-Text Search**: Multi-field search with various query types (MATCH, TERM, PHRASE, PREFIX, WILDCARD, FUZZY)
- **Aggregations**: Support for bucket aggregations (TERMS, DATE_HISTOGRAM, RANGE) and metric aggregations (AVG, SUM, MIN, MAX, CARDINALITY, STATS)
- **CRUD Operations**: Get, update, and delete documents by ID
- **Index Management**: Create, delete, check existence, and refresh indexes
- **Cluster Support**: Multi-node Elasticsearch cluster configuration
- **Authentication**: Basic auth (username/password) or API key authentication
- **SSL/TLS**: Configurable SSL with certificate verification
- **Connection Pooling**: Optimized connection management with configurable limits
- **Error Handling**: Comprehensive error mapping using Result pattern

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.marcusprado02</groupId>
    <artifactId>commons-adapters-search-elasticsearch</artifactId>
    <version>${commons.version}</version>
</dependency>
```

## Configuration

### Development Configuration

Simple configuration for local development:

```java
ElasticsearchConfiguration config = ElasticsearchConfiguration.forDevelopment(
    "http://localhost:9200",
    "elastic",
    "changeme"
);
```

This creates a configuration with:
- No SSL/TLS
- 30-second connection timeout
- 60-second socket timeout
- 10 max connections
- No certificate verification

### Production Configuration

Secure configuration for production with cluster support:

```java
List<String> nodes = List.of(
    "https://es-node1.example.com:9200",
    "https://es-node2.example.com:9200",
    "https://es-node3.example.com:9200"
);

ElasticsearchConfiguration config = ElasticsearchConfiguration.forProduction(
    nodes,
    "elastic",
    "strongpassword"
);
```

This creates a configuration with:
- SSL/TLS enabled
- Certificate verification enabled
- 10-second connection timeout
- 30-second socket timeout
- 50 max connections

### API Key Authentication

For cloud deployments using API keys:

```java
ElasticsearchConfiguration config = ElasticsearchConfiguration.withApiKey(
    "https://my-cluster.elastic-cloud.com:9200",
    "VnVhQ2ZHY0JDZGJrUW0tZTVhT3g6dWkybHAyYXhUTm1zeWFrdzl0dk5udw=="
);
```

### Custom Configuration

For fine-grained control using the builder:

```java
ElasticsearchConfiguration config = ElasticsearchConfiguration.builder()
    .serverUrl("https://elasticsearch.example.com:9200")
    .username("search_user")
    .password("secret")
    .connectionTimeout(Duration.ofSeconds(20))
    .socketTimeout(Duration.ofSeconds(45))
    .maxConnections(30)
    .enableSsl(true)
    .verifySslCertificates(false) // For self-signed certificates
    .build();
```

## Usage

### Create the Adapter

```java
ElasticsearchConfiguration config = ElasticsearchConfiguration.forDevelopment(
    "http://localhost:9200",
    "elastic",
    "changeme"
);

try (ElasticsearchAdapter adapter = new ElasticsearchAdapter(config)) {
    // Use the adapter
}
```

### Index a Document

```java
Map<String, Object> document = Map.of(
    "title", "Introduction to Elasticsearch",
    "content", "Elasticsearch is a powerful search engine...",
    "author", "John Doe",
    "publishedAt", Instant.now(),
    "views", 1250,
    "tags", List.of("search", "elasticsearch", "tutorial")
);

Document doc = Document.of("article-123", document);

Result<String> result = adapter.index("articles", doc);

if (result.isSuccess()) {
    System.out.println("Document indexed with ID: " + result.value());
} else {
    System.err.println("Indexing failed: " + result.problem().message());
}
```

### Bulk Index Documents

```java
List<Document> documents = List.of(
    Document.of("1", Map.of("title", "First Article", "views", 100)),
    Document.of("2", Map.of("title", "Second Article", "views", 250)),
    Document.of("3", Map.of("title", "Third Article", "views", 500))
);

Result<SearchPort.BulkIndexResult> result = adapter.bulkIndex("articles", documents);

if (result.isSuccess()) {
    SearchPort.BulkIndexResult bulkResult = result.value();
    System.out.printf("Indexed %d/%d documents successfully%n",
        bulkResult.successCount(), bulkResult.totalDocuments());
    
    if (!bulkResult.errors().isEmpty()) {
        System.out.println("Errors: " + bulkResult.errors());
    }
}
```

### Search Documents

#### Simple Match Query

```java
SearchQuery query = SearchQuery.builder()
    .query("elasticsearch tutorial")
    .queryType(SearchQuery.QueryType.MATCH)
    .fields(List.of("title", "content"))
    .size(20)
    .build();

Result<SearchResult> result = adapter.search("articles", query);

if (result.isSuccess()) {
    SearchResult searchResult = result.value();
    System.out.printf("Found %d results in %dms%n",
        searchResult.totalHits(), searchResult.tookMillis());
    
    for (Document doc : searchResult.hits()) {
        System.out.printf("- %s (score: %.2f)%n",
            doc.getField("title"), doc.score());
    }
}
```

#### Term Query (Exact Match)

```java
SearchQuery query = SearchQuery.builder()
    .query("published")
    .queryType(SearchQuery.QueryType.TERM)
    .fields(List.of("status"))
    .build();

Result<SearchResult> result = adapter.search("articles", query);
```

#### Phrase Query

```java
SearchQuery query = SearchQuery.builder()
    .query("machine learning algorithms")
    .queryType(SearchQuery.QueryType.PHRASE)
    .fields(List.of("content"))
    .build();
```

#### Prefix Query

```java
SearchQuery query = SearchQuery.builder()
    .query("java")
    .queryType(SearchQuery.QueryType.PREFIX)
    .fields(List.of("tags"))
    .build();
```

#### Wildcard Query

```java
SearchQuery query = SearchQuery.builder()
    .query("ela*c")
    .queryType(SearchQuery.QueryType.WILDCARD)
    .fields(List.of("title"))
    .build();
```

#### Fuzzy Query (Typo Tolerance)

```java
SearchQuery query = SearchQuery.builder()
    .query("elastcsearch") // typo
    .queryType(SearchQuery.QueryType.FUZZY)
    .fields(List.of("title", "content"))
    .build();
```

### Search with Filters, Sorting, and Pagination

```java
Map<String, Object> filters = Map.of(
    "status", "published",
    "category", "technology"
);

SearchQuery query = SearchQuery.builder()
    .query("best practices")
    .filters(filters)
    .sortField(new SearchQuery.SortField("publishedAt", SearchQuery.SortOrder.DESC))
    .sortField(new SearchQuery.SortField("_score", SearchQuery.SortOrder.DESC))
    .from(0)
    .size(10)
    .minScore(0.5) // Only results with score >= 0.5
    .build();

Result<SearchResult> result = adapter.search("articles", query);
```

### Multi-Index Search

```java
List<String> indices = List.of("articles", "blog_posts", "documentation");

SearchQuery query = SearchQuery.of("search term");

Result<SearchResult> result = adapter.search(indices, query);
```

### Get Document by ID

```java
Result<Document> result = adapter.get("articles", "article-123");

if (result.isSuccess()) {
    Document doc = result.value();
    System.out.println("Title: " + doc.getField("title"));
    System.out.println("Views: " + doc.getField("views", Integer.class));
}
```

### Update Document

```java
Map<String, Object> updates = Map.of(
    "views", 1500,
    "lastModified", Instant.now()
);

Result<Void> result = adapter.update("articles", "article-123", updates);
```

### Delete Document

```java
Result<Void> result = adapter.delete("articles", "article-123");
```

### Aggregations

#### Terms Aggregation (Buckets)

```java
Aggregation categoryAgg = Aggregation.terms("by_category", "category")
    .size(20)
    .build();

SearchQuery query = SearchQuery.matchAll();

Result<AggregationResult> result = adapter.aggregate("articles", query, categoryAgg);

if (result.isSuccess()) {
    AggregationResult aggResult = result.value();
    
    for (AggregationResult.Bucket bucket : aggResult.buckets()) {
        System.out.printf("%s: %d documents%n", bucket.key(), bucket.docCount());
    }
}
```

#### Metric Aggregations

```java
// Average views
Aggregation avgViews = Aggregation.avg("avg_views", "views");

// Total revenue
Aggregation totalRevenue = Aggregation.sum("total_revenue", "revenue");

// Min/Max price
Aggregation minPrice = Aggregation.min("min_price", "price");
Aggregation maxPrice = Aggregation.max("max_price", "price");

// Unique users
Aggregation uniqueUsers = Aggregation.cardinality("unique_users", "userId");

SearchQuery query = SearchQuery.matchAll();

Result<AggregationResult> result = adapter.aggregate("articles", query, avgViews);

if (result.isSuccess()) {
    Double avgValue = result.value().getMetric("avg_views");
    System.out.println("Average views: " + avgValue);
}
```

### Index Management

#### Create Index

```java
Map<String, Object> settings = Map.of(
    "number_of_shards", 3,
    "number_of_replicas", 2
);

Map<String, Object> mappings = Map.of(
    "properties", Map.of(
        "title", Map.of("type", "text"),
        "publishedAt", Map.of("type", "date"),
        "views", Map.of("type", "integer")
    )
);

Result<Void> result = adapter.createIndex("articles", settings, mappings);
```

#### Delete Index

```java
Result<Void> result = adapter.deleteIndex("old_articles");
```

#### Check Index Exists

```java
Result<Boolean> result = adapter.indexExists("articles");

if (result.isSuccess() && result.value()) {
    System.out.println("Index exists");
}
```

#### Refresh Index

Force index refresh to make recent changes immediately searchable:

```java
Result<Void> result = adapter.refresh("articles");
```

## Query Types

| Type | Description | Use Case |
|------|-------------|----------|
| `MATCH` | Full-text search with relevance scoring | General search, multi-field queries |
| `TERM` | Exact term matching (not analyzed) | Status codes, IDs, exact values |
| `PHRASE` | Exact phrase matching in order | "machine learning", "data science" |
| `PREFIX` | Prefix matching | Autocomplete, starts-with searches |
| `WILDCARD` | Pattern matching with * and ? | Flexible pattern searches |
| `FUZZY` | Typo-tolerant searching | User typos, misspellings |

## Aggregation Types

### Bucket Aggregations
- `TERMS`: Group by field values (categories, tags, authors)
- `DATE_HISTOGRAM`: Time-based grouping (daily, monthly stats)
- `RANGE`: Numeric or date ranges (price ranges, age groups)

### Metric Aggregations
- `AVG`: Average value of a numeric field
- `SUM`: Sum of numeric field values
- `MIN`: Minimum value
- `MAX`: Maximum value
- `COUNT`: Document count (implicit in bucket aggregations)
- `CARDINALITY`: Count distinct values (unique users, products)
- `STATS`: Combined min, max, avg, sum, count

## Error Handling

All operations return `Result<T>` for consistent error handling:

```java
Result<SearchResult> result = adapter.search("articles", query);

if (result.isSuccess()) {
    SearchResult searchResult = result.value();
    // Process results
} else {
    Problem problem = result.problem();
    System.err.println("Error: " + problem.message());
    System.err.println("Code: " + problem.errorCode());
    System.err.println("Category: " + problem.category());
    System.err.println("Severity: " + problem.severity());
}
```

Common error scenarios:
- Index not found
- Elasticsearch connection errors
- Authentication failures
- Malformed queries
- Index already exists
- Document not found

## Testing

### Unit Tests

Mock the Elasticsearch client for unit testing:

```java
@Test
void shouldSearchDocuments() throws IOException {
    ElasticsearchClient mockClient = mock(ElasticsearchClient.class);
    // Configure mock behavior
    
    // Test your service using the mocked client
}
```

### Integration Tests with Testcontainers

Use Testcontainers for integration testing with a real Elasticsearch instance:

```java
@Testcontainers
class ElasticsearchIntegrationTest {
    
    @Container
    static ElasticsearchContainer elasticsearch = new ElasticsearchContainer(
        "docker.elastic.co/elasticsearch/elasticsearch:8.11.0"
    );
    
    private ElasticsearchAdapter adapter;
    
    @BeforeEach
    void setUp() {
        String host = "http://" + elasticsearch.getHttpHostAddress();
        
        ElasticsearchConfiguration config = ElasticsearchConfiguration.builder()
            .serverUrl(host)
            .username("elastic")
            .password(elasticsearch.getPassword())
            .enableSsl(false)
            .build();
        
        adapter = new ElasticsearchAdapter(config);
    }
    
    @Test
    void shouldIndexAndSearchDocuments() {
        // Index test document
        Document doc = Document.of("test-1", Map.of("title", "Test Article"));
        adapter.index("test-index", doc);
        
        // Search
        SearchQuery query = SearchQuery.of("Test");
        Result<SearchResult> result = adapter.search("test-index", query);
        
        assertTrue(result.isSuccess());
        assertEquals(1, result.value().totalHits());
    }
}
```

## Production Considerations

### Performance

1. **Connection Pooling**: Adjust `maxConnections` based on load
2. **Timeouts**: Configure appropriate connection and socket timeouts
3. **Bulk Operations**: Use bulk indexing for high-throughput scenarios
4. **Refresh Strategy**: Control when changes become searchable

### Security

1. **Authentication**: Use API keys for cloud deployments
2. **SSL/TLS**: Always enable SSL in production
3. **Certificate Verification**: Enable for production clusters
4. **Credentials**: Store credentials securely (environment variables, secrets manager)

### High Availability

1. **Cluster Configuration**: Configure multiple Elasticsearch nodes
2. **Failover**: Client automatically handles node failures
3. **Replication**: Configure appropriate replica counts
4. **Monitoring**: Monitor cluster health and query performance

### Monitoring

```java
// Track search performance
SearchResult result = adapter.search("articles", query).value();
System.out.println("Query took: " + result.tookMillis() + "ms");
System.out.println("Max score: " + result.maxScore());
System.out.println("Total hits: " + result.totalHits());
```

## Compatibility

- Elasticsearch: 8.x
- Java: 17+
- Elasticsearch Java Client: 8.11.0

## Related Modules

- `commons-ports-search`: Search port interface and domain model
- `commons-kernel-result`: Result pattern for error handling
- `commons-kernel-core`: Core domain building blocks

## License

See the parent project's LICENSE file for licensing information.
