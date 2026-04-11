package com.marcusprado02.commons.adapters.search.opensearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.search.AggregationResult;
import com.marcusprado02.commons.ports.search.Document;
import com.marcusprado02.commons.ports.search.SearchPort;
import com.marcusprado02.commons.ports.search.SearchQuery;
import com.marcusprado02.commons.ports.search.SearchResult;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.StringTermsAggregate;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.BulkOperation;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.BulkResponseItem;
import org.opensearch.client.opensearch.core.DeleteRequest;
import org.opensearch.client.opensearch.core.ExistsRequest;
import org.opensearch.client.opensearch.core.GetRequest;
import org.opensearch.client.opensearch.core.GetResponse;
import org.opensearch.client.opensearch.core.Hit;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.IndexResponse;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.SearchResult;
import org.opensearch.client.opensearch.core.UpdateRequest;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.core.bulk.BulkResponseItem;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.DeleteIndexRequest;
import org.opensearch.client.opensearch.indices.ExistsRequest;
import org.opensearch.client.opensearch.indices.RefreshRequest;
import org.opensearch.client.transport.rest_client.RestClientTransport;

/**
 * OpenSearch implementation of SearchPort using the OpenSearch Java client 2.x.
 *
 * <p>Provides full-text search, indexing, and aggregation capabilities. The OpenSearch client API
 * is compatible with Elasticsearch 7.x semantics.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * var config = OpenSearchConfiguration.forDevelopment();
 * var adapter = new OpenSearchAdapter(config);
 *
 * // Index a document
 * adapter.index("products", Document.of("1", Map.of("name", "Widget")));
 *
 * // Search
 * var query = SearchQuery.builder().query("Widget").build();
 * adapter.search("products", query).peek(result -> ...);
 * }</pre>
 */
public class OpenSearchAdapter implements SearchPort {

  private final OpenSearchClient client;
  private final RestClient restClient;

  /**
   * Creates a new OpenSearchAdapter with the given configuration.
   *
   * @param configuration OpenSearch configuration
   */
  public OpenSearchAdapter(OpenSearchConfiguration configuration) {
    Objects.requireNonNull(configuration, "Configuration cannot be null");
    this.restClient = createRestClient(configuration);
    var transport = new RestClientTransport(restClient, new JacksonJsonpMapper(new ObjectMapper()));
    this.client = new OpenSearchClient(transport);
  }

  @Override
  public Result<String> index(String index, Document document) {
    Objects.requireNonNull(index, "Index cannot be null");
    Objects.requireNonNull(document, "Document cannot be null");

    try {
      IndexRequest<Map<String, Object>> request =
          IndexRequest.of(i -> i.index(index).id(document.id()).document(document.source()));

      IndexResponse response = client.index(request);
      return Result.ok(response.id());

    } catch (OpenSearchException e) {
      return Result.fail(mapOpenSearchException(e, "indexing document"));
    } catch (IOException e) {
      return Result.fail(
          createProblem(
              "OPENSEARCH.INDEX_IO_ERROR", "I/O error during indexing: " + e.getMessage()));
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
      List<BulkOperation> operations =
          documents.stream()
              .map(
                  doc ->
                      BulkOperation.of(
                          b ->
                              b.index(idx -> idx.index(index).id(doc.id()).document(doc.source()))))
              .collect(Collectors.toList());

      BulkRequest request = BulkRequest.of(b -> b.index(index).operations(operations));
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

      return Result.ok(
          new SearchPort.BulkIndexResult(documents.size(), successCount, failureCount, errors));

    } catch (OpenSearchException e) {
      return Result.fail(mapOpenSearchException(e, "bulk indexing"));
    } catch (IOException e) {
      return Result.fail(
          createProblem(
              "OPENSEARCH.BULK_INDEX_IO_ERROR",
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
      Query osQuery = buildQuery(query);

      SearchRequest.Builder requestBuilder =
          new SearchRequest.Builder()
              .index(indices)
              .query(osQuery)
              .from(query.from())
              .size(query.size());

      for (SearchQuery.SortField sortField : query.sorting()) {
        requestBuilder.sort(
            s ->
                s.field(
                    f ->
                        f.field(sortField.field())
                            .order(
                                sortField.order() == SearchQuery.SortOrder.ASC
                                    ? SortOrder.Asc
                                    : SortOrder.Desc)));
      }

      if (query.minScore() != null) {
        requestBuilder.minScore(query.minScore().doubleValue());
      }

      SearchResponse<Map> response = client.search(requestBuilder.build(), Map.class);

      List<Document> hits =
          response.hits().hits().stream().map(this::mapHitToDocument).collect(Collectors.toList());

      SearchResult result =
          new SearchResult(
              hits,
              response.hits().total() != null ? response.hits().total().value() : 0,
              response.hits().maxScore() != null ? response.hits().maxScore().floatValue() : 0f,
              response.took());

      return Result.ok(result);

    } catch (OpenSearchException e) {
      return Result.fail(mapOpenSearchException(e, "searching documents"));
    } catch (IOException e) {
      return Result.fail(
          createProblem(
              "OPENSEARCH.SEARCH_IO_ERROR", "I/O error during search: " + e.getMessage()));
    }
  }

  @Override
  public Result<Document> get(String index, String id) {
    Objects.requireNonNull(index, "Index cannot be null");
    Objects.requireNonNull(id, "ID cannot be null");

    try {
      GetRequest request = GetRequest.of(g -> g.index(index).id(id));
      GetResponse<Map> response = client.get(request, Map.class);

      if (!response.found()) {
        return Result.fail(
            createProblem("OPENSEARCH.DOCUMENT_NOT_FOUND", "Document not found: " + id));
      }

      return Result.ok(Document.of(response.id(), response.source()));

    } catch (OpenSearchException e) {
      return Result.fail(mapOpenSearchException(e, "getting document"));
    } catch (IOException e) {
      return Result.fail(
          createProblem("OPENSEARCH.GET_IO_ERROR", "I/O error during get: " + e.getMessage()));
    }
  }

  @Override
  public Result<Void> delete(String index, String id) {
    Objects.requireNonNull(index, "Index cannot be null");
    Objects.requireNonNull(id, "ID cannot be null");

    try {
      client.delete(DeleteRequest.of(d -> d.index(index).id(id)));
      return Result.ok(null);

    } catch (OpenSearchException e) {
      return Result.fail(mapOpenSearchException(e, "deleting document"));
    } catch (IOException e) {
      return Result.fail(
          createProblem(
              "OPENSEARCH.DELETE_IO_ERROR", "I/O error during delete: " + e.getMessage()));
    }
  }

  @Override
  public Result<Void> update(String index, String id, Map<String, Object> updates) {
    Objects.requireNonNull(index, "Index cannot be null");
    Objects.requireNonNull(id, "ID cannot be null");
    Objects.requireNonNull(updates, "Updates cannot be null");

    try {
      UpdateRequest<Map<String, Object>, Map<String, Object>> request =
          UpdateRequest.of(u -> u.index(index).id(id).doc(updates));
      client.update(request, Map.class);
      return Result.ok(null);

    } catch (OpenSearchException e) {
      return Result.fail(mapOpenSearchException(e, "updating document"));
    } catch (IOException e) {
      return Result.fail(
          createProblem(
              "OPENSEARCH.UPDATE_IO_ERROR", "I/O error during update: " + e.getMessage()));
    }
  }

  @Override
  public Result<Void> createIndex(
      String index, Map<String, Object> settings, Map<String, Object> mappings) {
    Objects.requireNonNull(index, "Index cannot be null");

    try {
      client.indices().create(CreateIndexRequest.of(c -> c.index(index)));
      return Result.ok(null);

    } catch (OpenSearchException e) {
      return Result.fail(mapOpenSearchException(e, "creating index"));
    } catch (IOException e) {
      return Result.fail(
          createProblem(
              "OPENSEARCH.CREATE_INDEX_IO_ERROR",
              "I/O error during index creation: " + e.getMessage()));
    }
  }

  @Override
  public Result<Void> deleteIndex(String index) {
    Objects.requireNonNull(index, "Index cannot be null");

    try {
      client.indices().delete(DeleteIndexRequest.of(d -> d.index(index)));
      return Result.ok(null);

    } catch (OpenSearchException e) {
      return Result.fail(mapOpenSearchException(e, "deleting index"));
    } catch (IOException e) {
      return Result.fail(
          createProblem(
              "OPENSEARCH.DELETE_INDEX_IO_ERROR",
              "I/O error during index deletion: " + e.getMessage()));
    }
  }

  @Override
  public Result<Boolean> indexExists(String index) {
    Objects.requireNonNull(index, "Index cannot be null");

    try {
      boolean exists = client.indices().exists(ExistsRequest.of(e -> e.index(index))).value();
      return Result.ok(exists);

    } catch (OpenSearchException e) {
      return Result.fail(mapOpenSearchException(e, "checking index existence"));
    } catch (IOException e) {
      return Result.fail(
          createProblem(
              "OPENSEARCH.INDEX_EXISTS_IO_ERROR",
              "I/O error during index existence check: " + e.getMessage()));
    }
  }

  @Override
  public Result<Void> refresh(String index) {
    Objects.requireNonNull(index, "Index cannot be null");

    try {
      client.indices().refresh(RefreshRequest.of(r -> r.index(index)));
      return Result.ok(null);

    } catch (OpenSearchException e) {
      return Result.fail(mapOpenSearchException(e, "refreshing index"));
    } catch (IOException e) {
      return Result.fail(
          createProblem(
              "OPENSEARCH.REFRESH_IO_ERROR", "I/O error during refresh: " + e.getMessage()));
    }
  }

  @Override
  public Result<AggregationResult> aggregate(
      String index,
      SearchQuery query,
      List<com.marcusprado02.commons.ports.search.Aggregation> aggregations) {
    Objects.requireNonNull(index, "Index cannot be null");
    Objects.requireNonNull(query, "Query cannot be null");
    Objects.requireNonNull(aggregations, "Aggregations cannot be null");

    if (aggregations.isEmpty()) {
      return Result.ok(AggregationResult.empty("empty"));
    }

    try {
      Query osQuery = buildQuery(query);
      Map<String, org.opensearch.client.opensearch._types.aggregations.Aggregation> osAggs =
          buildAggregations(aggregations);

      SearchRequest request =
          SearchRequest.of(s -> s.index(index).query(osQuery).size(0).aggregations(osAggs));

      SearchResponse<Map> response = client.search(request, Map.class);

      if (response.aggregations() != null && !aggregations.isEmpty()) {
        var firstAgg = aggregations.get(0);
        Aggregate aggregate = response.aggregations().get(firstAgg.name());
        if (aggregate != null) {
          return Result.ok(mapAggregateToResult(firstAgg.name(), aggregate));
        }
      }

      return Result.ok(AggregationResult.empty("empty"));

    } catch (OpenSearchException e) {
      return Result.fail(mapOpenSearchException(e, "executing aggregation"));
    } catch (IOException e) {
      return Result.fail(
          createProblem(
              "OPENSEARCH.AGGREGATE_IO_ERROR", "I/O error during aggregation: " + e.getMessage()));
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

  // -------------------------------------------------------------------------
  // Private helpers
  // -------------------------------------------------------------------------

  private RestClient createRestClient(OpenSearchConfiguration config) {
    HttpHost[] hosts = config.urls().stream().map(HttpHost::create).toArray(HttpHost[]::new);

    RestClientBuilder builder = RestClient.builder(hosts);

    if (config.hasBasicAuth()) {
      var credentialsProvider = new BasicCredentialsProvider();
      credentialsProvider.setCredentials(
          AuthScope.ANY, new UsernamePasswordCredentials(config.username(), config.password()));
      builder.setHttpClientConfigCallback(
          b -> b.setDefaultCredentialsProvider(credentialsProvider));
    }

    builder.setRequestConfigCallback(
        b ->
            b.setConnectTimeout((int) config.connectionTimeout().toMillis())
                .setSocketTimeout((int) config.socketTimeout().toMillis()));

    return builder.build();
  }

  @SuppressWarnings("checkstyle:indentation")
  private Query buildQuery(SearchQuery query) {
    if ("*".equals(query.query()) || query.query() == null || query.query().isBlank()) {
      return Query.of(q -> q.matchAll(m -> m));
    }

    return switch (query.queryType()) {
      case MATCH -> {
        if (query.fields().isEmpty()) {
          yield Query.of(q -> q.multiMatch(m -> m.query(query.query())));
        } else {
          yield Query.of(q -> q.multiMatch(m -> m.query(query.query()).fields(query.fields())));
        }
      }
      case TERM ->
          Query.of(
              q ->
                  q.term(
                      t ->
                          t.field(query.fields().isEmpty() ? "_all" : query.fields().get(0))
                              .value(FieldValue.of(query.query()))));
      case PHRASE ->
          Query.of(
              q ->
                  q.matchPhrase(
                      m ->
                          m.field(query.fields().isEmpty() ? "_all" : query.fields().get(0))
                              .query(query.query())));
      case PREFIX ->
          Query.of(
              q ->
                  q.prefix(
                      p ->
                          p.field(query.fields().isEmpty() ? "_all" : query.fields().get(0))
                              .value(query.query())));
      case WILDCARD ->
          Query.of(
              q ->
                  q.wildcard(
                      w ->
                          w.field(query.fields().isEmpty() ? "_all" : query.fields().get(0))
                              .value(query.query())));
      case FUZZY ->
          Query.of(
              q ->
                  q.fuzzy(
                      f ->
                          f.field(query.fields().isEmpty() ? "_all" : query.fields().get(0))
                              .value(query.query())));
      default -> Query.of(q -> q.matchAll(m -> m));
    };
  }

  @SuppressWarnings("checkstyle:indentation")
  private Map<String, org.opensearch.client.opensearch._types.aggregations.Aggregation>
      buildAggregations(List<com.marcusprado02.commons.ports.search.Aggregation> aggregations) {

    Map<String, org.opensearch.client.opensearch._types.aggregations.Aggregation> result =
        new HashMap<>();

    for (var agg : aggregations) {
      var osAgg =
          switch (agg.type()) {
            case TERMS ->
                org.opensearch.client.opensearch._types.aggregations.Aggregation.of(
                    a ->
                        a.terms(
                            t -> t.field(agg.field()).size(agg.size() != null ? agg.size() : 10)));
            case AVG ->
                org.opensearch.client.opensearch._types.aggregations.Aggregation.of(
                    a -> a.avg(avg -> avg.field(agg.field())));
            case SUM ->
                org.opensearch.client.opensearch._types.aggregations.Aggregation.of(
                    a -> a.sum(sum -> sum.field(agg.field())));
            case MIN ->
                org.opensearch.client.opensearch._types.aggregations.Aggregation.of(
                    a -> a.min(min -> min.field(agg.field())));
            case MAX ->
                org.opensearch.client.opensearch._types.aggregations.Aggregation.of(
                    a -> a.max(max -> max.field(agg.field())));
            case CARDINALITY ->
                org.opensearch.client.opensearch._types.aggregations.Aggregation.of(
                    a -> a.cardinality(c -> c.field(agg.field())));
            default ->
                org.opensearch.client.opensearch._types.aggregations.Aggregation.of(
                    a -> a.terms(t -> t.field(agg.field())));
          };
      result.put(agg.name(), osAgg);
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

  private AggregationResult mapAggregateToResult(String name, Aggregate aggregate) {
    if (aggregate.isSterms()) {
      StringTermsAggregate terms = aggregate.sterms();
      List<AggregationResult.Bucket> buckets =
          terms.buckets().array().stream()
              .map(b -> AggregationResult.Bucket.of(b.key().stringValue(), b.docCount()))
              .collect(Collectors.toList());
      return new AggregationResult(name, buckets, Map.of());
    }

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

  private Problem mapOpenSearchException(OpenSearchException e, String operation) {
    return Problem.of(
        ErrorCode.of("OPENSEARCH.ERROR"),
        ErrorCategory.TECHNICAL,
        Severity.ERROR,
        String.format("OpenSearch error during %s: %s", operation, e.getMessage()));
  }

  private Problem createProblem(String code, String message) {
    return Problem.of(ErrorCode.of(code), ErrorCategory.TECHNICAL, Severity.ERROR, message);
  }
}
