# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- gRPC server and client adapters with logging, metrics, and auth interceptors
- Memcached cache adapter with Testcontainers integration tests
- SQS queue adapter with LocalStack integration tests

### Fixed
- gRPC `ErrorMapper.mapToStatus()` now includes exception message in the status description
- gRPC `AuthInterceptor` catches exceptions thrown by the token validator
- gRPC server port validation allows port 0 (OS-assigned random port)
- gRPC client retry validation requires `maxRetries > 0` when retry is enabled
- Memcached TTL=0 treated as "never expires" instead of using `currentTimeMillis()`
- `ScheduledFuture` import missing in AWS and Azure secrets `CachedSecretStorePort`
- S3 adapter error message aligned with test contract (`"File Not Found"`)
- SQS adapter accepts explicit `AwsCredentialsProvider` for LocalStack tests

---

## [0.1.0-SNAPSHOT] — initial development

### Added
- `commons-kernel-result` — `Result<T>` and `AsyncResult<T>` for functional error handling
- `commons-kernel-errors` — `Problem`, `ErrorCode`, `ErrorCategory`, `Severity`
- `commons-kernel-ddd` — DDD building blocks (AggregateRoot, Entity, ValueObject, DomainEvent)
- `commons-ports-*` — hexagonal port interfaces (cache, HTTP, messaging, persistence, queue, secrets, files, email, SMS, PDF, Excel, compression, serialization, search, blockchain, notification, template)
- `commons-adapters-cache-redis` — Redis adapter with atomic operations and Pub/Sub
- `commons-adapters-cache-memcached` — Memcached adapter
- `commons-adapters-persistence-jpa` — JPA adapter with pageable repository and outbox entity
- `commons-adapters-persistence-mongodb` — MongoDB adapter with filtering and sorting
- `commons-adapters-persistence-inmemory` — In-memory pageable repository for tests
- `commons-adapters-messaging-kafka` — Kafka publisher and consumer
- `commons-adapters-messaging-rabbitmq` — RabbitMQ publisher and consumer
- `commons-adapters-messaging-azureservicebus` — Azure Service Bus adapter
- `commons-adapters-queue-sqs` — AWS SQS queue adapter
- `commons-adapters-queue-azure-storage-queue` — Azure Storage Queue adapter
- `commons-adapters-http-okhttp` — OkHttp client adapter
- `commons-adapters-http-webclient` — Spring WebClient adapter
- `commons-adapters-web-spring` — Spring MVC exception handling, CORS, rate limiting, logging filter
- `commons-adapters-web-jaxrs` — JAX-RS filter and exception handling
- `commons-adapters-email-smtp` — SMTP email adapter with Thymeleaf templates
- `commons-adapters-email-sendgrid` — SendGrid email adapter
- `commons-adapters-email-ses` — AWS SES email adapter
- `commons-adapters-files-s3` — AWS S3 file store adapter
- `commons-adapters-files-azure-blob` — Azure Blob Storage adapter
- `commons-adapters-files-gcs` — Google Cloud Storage adapter
- `commons-adapters-secrets-aws-secretsmanager` — AWS Secrets Manager adapter with caching
- `commons-adapters-secrets-azure-keyvault` — Azure Key Vault adapter with caching
- `commons-adapters-secrets-vault` — HashiCorp Vault adapter
- `commons-adapters-search-elasticsearch` — Elasticsearch search adapter
- `commons-adapters-search-opensearch` — OpenSearch search adapter
- `commons-adapters-metrics-prometheus` — Prometheus metrics adapter
- `commons-adapters-tracing-jaeger` — Jaeger tracing adapter
- `commons-adapters-otel` — OpenTelemetry decorators
- `commons-adapters-grpc-server` — gRPC server with auth, logging, and metrics interceptors
- `commons-adapters-grpc-client` — gRPC client with logging and metrics interceptors
- `commons-adapters-graphql-server` — GraphQL server adapter
- `commons-adapters-resilience4j` — Resilience4j circuit breaker, rate limiter, bulkhead
- `commons-adapters-payment-stripe` — Stripe payment adapter
- `commons-adapters-pdf-itext` — iText PDF adapter
- `commons-adapters-excel-poi` — Apache POI Excel adapter
- `commons-adapters-compression` — Multi-algorithm compression adapter
- `commons-adapters-serialization-protobuf` — Protocol Buffers serialization adapter
- `commons-adapters-sms-twilio` — Twilio SMS adapter
- `commons-adapters-sms-aws-sns` — AWS SNS SMS adapter
- `commons-adapters-notification-fcm` — Firebase Cloud Messaging adapter
- `commons-adapters-notification-apns` — Apple Push Notification Service adapter
- `commons-adapters-template-thymeleaf` — Thymeleaf template renderer
- `commons-adapters-blockchain-web3j` — Web3j Ethereum adapter
- `commons-adapters-ml-tensorflow` — TensorFlow model inference adapter
- `commons-adapters-service-discovery-consul` — Consul service discovery adapter
- `commons-adapters-service-discovery-eureka` — Eureka service discovery adapter
- `commons-app-outbox` — Transactional outbox pattern implementation
- `commons-app-idempotency` — Idempotency key handling
- `commons-app-resilience` — Resilience policies (circuit breaker, retry, rate limit, fallback)
- `commons-app-observability` — Structured logging, metrics, health checks, log sanitization
- `commons-app-domain-events` — Domain event dispatcher
- `commons-app-configuration` — Configuration provider abstraction (AWS AppConfig, Azure, Consul, etcd)
- `commons-app-feature-flags` — Feature flag abstraction (LaunchDarkly, Unleash, in-memory)
- `commons-app-multi-tenancy` — Multi-tenancy support with schema isolation
- `commons-app-scheduler` — Scheduler abstraction
- `commons-app-batch` — Batch job abstraction
- `commons-app-i18n` — Internationalisation utilities
- `commons-app-audit-log` — Audit logging aspect
- `commons-app-rate-limiting` — Rate limiter abstraction
- `commons-app-health-checks` — Health check indicators
- `commons-app-api-gateway` — API gateway with load balancing and filters
- `commons-app-api-versioning` — API versioning support
- `commons-app-webhooks` — Webhook dispatcher
- `commons-app-data-validation` — Validation framework
- `commons-app-data-migration` — Data migration (Flyway, Liquibase, custom)
- `commons-app-workflow-engine` — Workflow engine
- `commons-app-backup-restore` — Backup and restore utilities
- `commons-spring-starter-cache` — Spring Boot auto-configuration for caching
- `commons-spring-starter-idempotency` — Spring Boot auto-configuration for idempotency
- `commons-spring-starter-observability` — Spring Boot auto-configuration for observability
- `commons-spring-starter-otel` — Spring Boot auto-configuration for OpenTelemetry
- `commons-spring-starter-outbox` — Spring Boot auto-configuration for outbox
- `commons-spring-starter-resilience` — Spring Boot auto-configuration for resilience
- `commons-spring-starter-secrets` — Spring Boot auto-configuration for secrets
- `commons-testkit-core` — Test utilities (RandomData, TestClock, data builders)
- `commons-testkit-contracts` — Contract tests for cache, HTTP client, message publisher, pageable repository
- `commons-testkit-archunit` — ArchUnit rules for DDD/hexagonal architecture
- `commons-archunit` — Architecture enforcement rules
- `commons-platform-archetype` — Maven archetype for new projects
- `commons-benchmarks` — JMH benchmarks
- `commons-bom` — Bill of Materials for dependency management
