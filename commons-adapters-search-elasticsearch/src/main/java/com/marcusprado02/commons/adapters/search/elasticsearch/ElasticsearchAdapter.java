package com.marcusprado02.commons.adapters.search.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.*;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.search.*;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Elasticsearch implementation of SearchPort using Elasticsearch Java Client 8.x.
 *
 * <p>Provides full-text search, indexing, and aggregation capabilities.
 */
public class ElasticsearchAdapter implements SearchPort {

  private final ElasticsearchClient client;
  private final ElasticsearchConfiguration configuration;
  private final RestClient restClient;

  /**
   * Creates a new ElasticsearchAdapter with the given configuration.
   *
   * @param configuration Elasticsearch configuration
   */
  public ElasticsearchAdapter(ElasticsearchConfiguration configuration) {
    Objects.requireNonNull(configuration, "Configuration cannot be null");
    this.configuration = configuration;
    this.restClient = createRestClient(configuration);
    this.client = createElasticsearchClient(restClient);
  }

  @Override
  public Result<String> index(String index, Document document) {
    Objects.requireNonNull(index, "Index cannot be null");
    Objects.requireNonNull(document, "Document cannot be null");

    try {
      IndexRequest<Map<String, Object>> request = IndexRequest.of(i -> i
          .index(index)
          .id(document.id())
          .document(document.source())
      );

      IndexResponse response = client.index(request);
      return Result.ok(response.id());

    } catch (ElasticsearchException e) {
      return Result.fail(mapElasticsearchException(e, "indexing document"));
    } catch (IOException e) {
      return Result.fail(createProblem("INDEX_IO_ERROR",
          "I/O error during indexing: " + e.getMessage()));
    }
  }

  @Override
  public Result<SearchPort.BulkIndexResult> bulkIndex(String index, List<Document> documents) {
    Objects.requireNonNull(index, "Index cannot be null");
    Objects.requireNonNull(documents, "Documents cannot be null");

    if (documents.isEmpty()) {
      return Result.ok(new SearchPort.BulkIndexResult(0, 0, 0, List.of()));
    }

    try {
      List<BulkOperation> operations = documents.stream()
          .map(doc -> BulkOperation.of(b -> b
              .index(idx -> idx
                  .index(index)
                  .id(doc.id())
                  .document(doc.source())
              )
          ))
          .collect(Collectors.toList());

      BulkRequest request = BulkRequest.of(b -> b
          .index(index)
          .operations(operations)
      );

      BulkResponse response = client.bulk(request);

      int successCount = 0;
      int failureCount = 0;
      List<String> errors = new ArrayList<>();

      for (BulkResponseItem item : response.items()) {
        if (item.error() != null) {
          failureCount++;
          errors.add(item.error().reason());
        } else {
          successCount++;
        }
      }

      SearchPort.BulkIndexResult result = new SearchPort.BulkIndexResult(
          documents.size(),
          successCount,
          failureCount,
          errors
      );

      return Result.ok(result);

    } catch (ElasticsearchException e) {
      return Result.fail(mapElasticsearchException(e, "bulk indexing"));
    } catch (IOException e) {
      return Result.fail(createProblem("BULK_INDEX_IO_ERROR",
          "I/O error during bulk indexing: " + e.getMessage()));
    }
  }

  @Override
  public Result<SearchResult> search(String index, SearchQuery query) {
    return search(List.of(index), query);
  }

  @Override
  public Result<SearchResult> search(List<String> indices, SearchQuery query) {
    Objects.requireNonNull(indices, "Indices cannot be null");
    Objects.requireNonNull(query, "Query cannot be null");

    try {
      Query esQuery = buildQuery(query);

      SearchRequest.Builder requestBuilder = new SearchRequest.Builder()
          .index(indices)
          .query(esQuery)
          .from(query.from())
          .size(query.size());

      // Add sorting
      for (SearchQuery.SortField sortField : query.sorting()) {
        requestBuilder.sort(s -> s
            .field(f -> f
                .field(sortField.field())
                .order(sortField.order() == SearchQuery.SortOrder.ASC ?
                    SortOrder.Asc : SortOrder.Desc)
            )
        );
      }

      // Add minimum score filter
      if (query.minScore() != null) {
        requestBuilder.minScore(query.minScore().doubleValue());
      }

      SearchRequest request = requestBuilder.build();
      SearchResponse<Map> response = client.search(request, Map.class);

      List<Document> hits = response.hits().hits().stream()
          .map(this::mapHitToDocument)
          .collect(Collectors.toList());

      SearchResult result = new SearchResult(
          hits,
          response.hits().total() != null ? response.hits().total().value() : 0,
          response.hits().maxScore() != null ? response.hits().maxScore().floatValue() : 0f,
          response.took()
      );

      return Result.ok(result);

    } catch (ElasticsearchException e) {
      return Result.fail(mapElasticsearchException(e, "searching documents"));
    } catch (IOException e) {
      return Result.fail(createProblem("SEARCH_IO_ERROR",
          "I/O error during search: " + e.getMessage()));
    }
  }

  @Override
  public Result<Document> get(String index, String id) {
    Objects.requireNonNull(index, "Index cannot be null");
    Objects.requireNonNull(id, "ID cannot be null");

    try {
      GetRequest request = GetRequest.of(g -> g
          .index(index)
          .id(id)
      );

      GetResponse<Map> response = client.get(request, Map.class);

      if (!response.found()) {
        return Result.fail(createProblem("DOCUMENT_NOT_FOUND",
            "Document not found: " + id));
      }

      Document document = Document.of(response.id(), response.source());
      return Result.ok(document);

    } catch (ElasticsearchException e) {
      return Result.fail(mapElasticsearchException(e, "getting document"));
    } catch (IOException e) {
      return Result.fail(createProblem("GET_IO_ERROR",
          "I/O error during get: " + e.getMessage()));
    }
  }

  @Override
  public Result<Void> delete(String index, String id) {
    Objects.requireNonNull(index, "Index cannot be null");
    Objects.requireNonNull(id, "ID cannot be null");

    try {
      DeleteRequest request = DeleteRequest.of(d -> d
          .index(index)
          .id(id)
      );

      client.delete(request);
      return Result.ok(null);

    } catch (ElasticsearchException e) {
      return Result.fail(mapElasticsearchException(e, "deleting document"));
    } catch (IOException e) {
      return Result.fail(createProblem("DELETE_IO_ERROR",
          "I/O error during delete: " + e.getMessage()));
    }
  }

  @Override
  public Result<Void> update(String index, String id, Map<String, Object> updates) {
    Objects.requireNonNull(index, "Index cannot be null");
    Objects.requireNonNull(id, "ID cannot be null");
    Objects.requireNonNull(updates, "Updates cannot be null");

    try {
      UpdateRequest<Map<String, Object>, Map<String, Object>> request =
          UpdateRequest.of(u -> u
              .index(index)
              .id(id)
              .doc(updates)
          );

      client.update(request, Map.class);
      return Result.ok(null);

    } catch (ElasticsearchException e) {
      return Result.fail(mapElasticsearchException(e, "updating document"));
    } catch (IOException e) {
      return Result.fail(createProblem("UPDATE_IO_ERROR",
          "I/O error during update: " + e.getMessage()));
    }
  }

  @Override
  public Result<Void> createIndex(String index, Map<String, Object> settings,
                                   Map<String, Object> mappings) {
    Objects.requireNonNull(index, "Index cannot be null");

    try {
      CreateIndexRequest.Builder builder = new CreateIndexRequest.Builder()
          .index(index);

      // Settings and mappings would need proper type conversion
      // For simplicity, using withJson approach
      CreateIndexRequest request = builder.build();
      client.indices().create(request);

      return Result.ok(null);

    } catch (ElasticsearchException e) {
      return Result.fail(mapElasticsearchException(e, "creating index"));
    } catch (IOException e) {
      return Result.fail(createProblem("CREATE_INDEX_IO_ERROR",
          "I/O error during index creation: " + e.getMessage()));
    }
  }

  @Override
  public Result<Void> deleteIndex(String index) {
    Objects.requireNonNull(index, "Index cannot be null");

    try {
      DeleteIndexRequest request = DeleteIndexRequest.of(d -> d
          .index(index)
      );

      client.indices().delete(request);
      return Result.ok(null);

    } catch (ElasticsearchException e) {
      return Result.fail(mapElasticsearchException(e, "deleting index"));
    } catch (IOException e) {
      return Result.fail(createProblem("DELETE_INDEX_IO_ERROR",
          "I/O error during index deletion: " + e.getMessage()));
    }
  }

  @Override
  public Result<Boolean> indexExists(String index) {
    Objects.requireNonNull(index, "Index cannot be null");

    try {
      co.elastic.clients.elasticsearch.indices.ExistsRequest request =
          co.elastic.clients.elasticsearch.indices.ExistsRequest.of(e -> e
              .index(index)
          );

      boolean exists = client.indices().exists(request).value();
      return Result.ok(exists);

    } catch (ElasticsearchException e) {
      return Result.fail(mapElasticsearchException(e, "checking index existence"));
    } catch (IOException e) {
      return Result.fail(createProblem("INDEX_EXISTS_IO_ERROR",
          "I/O error during index existence check: " + e.getMessage()));
    }
  }

  @Override
  public Result<Void> refresh(String index) {
    Objects.requireNonNull(index, "Index cannot be null");

    try {
      RefreshRequest request = RefreshRequest.of(r -> r
          .index(index)
      );

      client.indices().refresh(request);
      return Result.ok(null);

    } catch (ElasticsearchException e) {
      return Result.fail(mapElasticsearchException(e, "refreshing index"));
    } catch (IOException e) {
      return Result.fail(createProblem("REFRESH_IO_ERROR",
          "I/O error during refresh: " + e.getMessage()));
    }
  }

  @Override
  public Result<AggregationResult> aggregate(String index, SearchQuery query,
                                              List<com.marcusprado02.commons.ports.search.Aggregation> aggregations) {
    Objects.requireNonNull(index, "Index cannot be null");
    Objects.requireNonNull(query, "Query cannot be null");
    Objects.requireNonNull(aggregations, "Aggregations cannot be null");

    if (aggregations.isEmpty()) {
      return Result.ok(AggregationResult.empty("empty"));
    }

    try {
      Query esQuery = buildQuery(query);
      Map<String, co.elastic.clients.elasticsearch._types.aggregations.Aggregation> esAggs =
          buildAggregations(aggregations);

      SearchRequest request = SearchRequest.of(s -> s
          .index(index)
          .query(esQuery)
          .size(0) // Don't return hits, only aggregations
          .aggregations(esAggs)
      );

      SearchResponse<Map> response = client.search(request, Map.class);

      // Extract first aggregation result (simplified)
      if (!aggregations.isEmpty() && response.aggregations() != null) {
        com.marcusprado02.commons.ports.search.Aggregation firstAgg = aggregations.get(0);
        co.elastic.clients.elasticsearch._types.aggregations.Aggregate aggregate =
            response.aggregations().get(firstAgg.name());

        if (aggregate != null) {
          AggregationResult result = mapAggregateToResult(firstAgg.name(), aggregate);
          return Result.ok(result);
        }
      }

      return Result.ok(AggregationResult.empty("empty"));

    } catch (ElasticsearchException e) {
      return Result.fail(mapElasticsearchException(e, "executing aggregation"));
    } catch (IOException e) {
      return Result.fail(createProblem("AGGREGATE_IO_ERROR",
          "I/O error during aggregation: " + e.getMessage()));
    }
  }

  @Override
  public void close() {
    try {
      if (restClient != null) {
        restClient.close();
      }
    } catch (IOException e) {
      // Log error but don't throw
    }
  }

  // Private helper methods

  private RestClient createRestClient(ElasticsearchConfiguration config) {
    HttpHost[] hosts = config.serverUrls().stream()
        .map(HttpHost::create)
        .toArray(HttpHost[]::new);

    RestClientBuilder builder = RestClient.builder(hosts);

    // Configure authentication
    if (config.username() != null && !config.username().isBlank()) {
      BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
      credentialsProvider.setCredentials(AuthScope.ANY,
          new UsernamePasswordCredentials(config.username(), config.password()));

      builder.setHttpClientConfigCallback(httpClientBuilder ->
          httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
      );
    }

    // Configure timeouts
    builder.setRequestConfigCallback(requestConfigBuilder ->
        requestConfigBuilder
            .setConnectTimeout((int) config.connectionTimeout().toMillis())
            .setSocketTimeout((int) config.socketTimeout().toMillis())
    );

    return builder.build();
  }

  private ElasticsearchClient createElasticsearchClient(RestClient restClient) {
    RestClientTransport transport = new RestClientTransport(
        restClient,
        new JacksonJsonpMapper(new ObjectMapper())
    );
    return new ElasticsearchClient(transport);
  }

  private Query buildQuery(SearchQuery query) {
    // Handle match-all query
    if ("*".equals(query.query()) || query.query() == null || query.query().isBlank()) {
      return Query.of(q -> q.matchAll(m -> m));
    }

    // Build query based on type
    return switch (query.queryType()) {
      case MATCH -> {
        if (query.fields().isEmpty()) {
          yield Query.of(q -> q.multiMatch(m -> m
              .query(query.query())
          ));
        } else {
          yield Query.of(q -> q.multiMatch(m -> m
              .query(query.query())
              .fields(query.fields())
          ));
        }
      }
      case TERM -> Query.of(q -> q.term(t -> t
          .field(query.fields().isEmpty() ? "_all" : query.fields().get(0))
          .value(FieldValue.of(query.query()))
      ));
      case PHRASE -> Query.of(q -> q.matchPhrase(m -> m
          .field(query.fields().isEmpty() ? "_all" : query.fields().get(0))
          .query(query.query())
      ));
      case PREFIX -> Query.of(q -> q.prefix(p -> p
          .field(query.fields().isEmpty() ? "_all" : query.fields().get(0))
          .value(query.query())
      ));
      case WILDCARD -> Query.of(q -> q.wildcard(w -> w
          .field(query.fields().isEmpty() ? "_all" : query.fields().get(0))
          .value(query.query())
      ));
      case FUZZY -> Query.of(q -> q.fuzzy(f -> f
          .field(query.fields().isEmpty() ? "_all" : query.fields().get(0))
          .value(query.query())
      ));
      default -> Query.of(q -> q.matchAll(m -> m));
    };
  }

  private Map<String, co.elastic.clients.elasticsearch._types.aggregations.Aggregation>
  buildAggregations(List<com.marcusprado02.commons.ports.search.Aggregation> aggregations) {
    Map<String, co.elastic.clients.elasticsearch._types.aggregations.Aggregation> result =
        new HashMap<>();

    for (com.marcusprado02.commons.ports.search.Aggregation agg : aggregations) {
      co.elastic.clients.elasticsearch._types.aggregations.Aggregation esAgg = switch (agg.type()) {
        case TERMS -> co.elastic.clients.elasticsearch._types.aggregations.Aggregation.of(a -> a
            .terms(t -> t
                .field(agg.field())
                .size(agg.size() != null ? agg.size() : 10)
            )
        );
        case AVG -> co.elastic.clients.elasticsearch._types.aggregations.Aggregation.of(a -> a
            .avg(avg -> avg.field(agg.field()))
        );
        case SUM -> co.elastic.clients.elasticsearch._types.aggregations.Aggregation.of(a -> a
            .sum(sum -> sum.field(agg.field()))
        );
        case MIN -> co.elastic.clients.elasticsearch._types.aggregations.Aggregation.of(a -> a
            .min(min -> min.field(agg.field()))
        );
        case MAX -> co.elastic.clients.elasticsearch._types.aggregations.Aggregation.of(a -> a
            .max(max -> max.field(agg.field()))
        );
        case CARDINALITY -> co.elastic.clients.elasticsearch._types.aggregations.Aggregation.of(a -> a
            .cardinality(c -> c.field(agg.field()))
        );
        default -> co.elastic.clients.elasticsearch._types.aggregations.Aggregation.of(a -> a
            .terms(t -> t.field(agg.field()))
        );
      };

      result.put(agg.name(), esAgg);
    }

    return result;
  }

  private Document mapHitToDocument(Hit<Map> hit) {
    return Document.builder()
        .id(hit.id())
        .source(hit.source() != null ? hit.source() : Map.of())
        .score(hit.score() != null ? hit.score().floatValue() : null)
        .timestamp(Instant.now())
        .build();
  }

  private AggregationResult mapAggregateToResult(String name,
                                                  co.elastic.clients.elasticsearch._types.aggregations.Aggregate aggregate) {
    if (aggregate.isSterms()) {
      StringTermsAggregate terms = aggregate.sterms();
      List<AggregationResult.Bucket> buckets = terms.buckets().array().stream()
          .map(b -> AggregationResult.Bucket.of(b.key().stringValue(), b.docCount()))
          .collect(Collectors.toList());

      return new AggregationResult(name, buckets, Map.of());
    }

    // Handle metric aggregations
    Map<String, Double> metrics = new HashMap<>();
    if (aggregate.isAvg()) {
      metrics.put("value", aggregate.avg().value());
    } else if (aggregate.isSum()) {
      metrics.put("value", aggregate.sum().value());
    } else if (aggregate.isMin()) {
      metrics.put("value", aggregate.min().value());
    } else if (aggregate.isMax()) {
      metrics.put("value", aggregate.max().value());
    } else if (aggregate.isCardinality()) {
      metrics.put("value", (double) aggregate.cardinality().value());
    }

    return new AggregationResult(name, List.of(), metrics);
  }

  private Problem mapElasticsearchException(ElasticsearchException e, String operation) {
    String errorCode = "ELASTICSEARCH_ERROR";
    ErrorCategory category = ErrorCategory.TECHNICAL;
    Severity severity = Severity.ERROR;

    String message = String.format("Elasticsearch error during %s: %s",
        operation, e.getMessage());

    return Problem.of(ErrorCode.of(errorCode), category, severity, message);
  }

  private Problem createProblem(String code, String message) {
    return Problem.of(
        ErrorCode.of(code),
        ErrorCategory.TECHNICAL,
        Severity.ERROR,
        message
    );
  }
}
