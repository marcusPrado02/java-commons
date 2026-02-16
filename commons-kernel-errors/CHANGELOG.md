# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased] - 

### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
### ✨ Features
- feat(batch): enhance BatchJobError to use ProblemBuilder for error creation with detailed error categorization and severity ([51ae8493360252a]())
- feat: Add OpenSearch adapter for SearchPort with configuration and usage examples ([6684d25a9da656c]())
- feat: add search module with Elasticsearch support including SearchPort, SearchQuery, and related classes ([725f9ae619222fc]())
- feat: add MemcachedCacheAdapter and related tests for caching implementation ([088a8ac20c037f7]())
- feat: add Redis atomic operations and Pub/Sub support for real-time messaging ([7217c12af60f41e]())
- feat: add test dependencies for JUnit and AssertJ ([fa66579a1026056]())
- feat(validation): implement configuration validation framework with metadata and error handling ([a1c8746e36efab7]())
- feat: add Result, AsyncResult, and ValidationResult classes for functional error handling and validation workflows ([d37733f31770ea5]())
- feat: Add CORS configuration support with CorsProperties and CorsConfigurationHelper; implement RequestResponseLoggingFilter for detailed logging; introduce RateLimitFilter with token bucket algorithm for rate limiting; enhance ProblemExceptionHandler for comprehensive error handling ([8d740ef7a1da183]())
- feat: Add commons-testkit-archunit module with reusable architectural tests for DDD/Hexagonal architecture validation ([9a6a93a3b812b02]())
- feat: Add commons-testkit-contracts module with base contract tests for cache, HTTP client, message publisher, and pageable repository implementations ([9f985e158e7f5e5]())
- feat: Add Commons Testkit Core with utilities for testing, including RandomData, TestClock, and Test Data Builders ([5d25772545fd093]())
- feat(outbox): implement DefaultOutboxProcessor with batch processing, retry strategy, and metrics; add OutboxProcessorConfig and ExponentialBackoffStrategy; deprecate OutboxStore ([d7b6b7d51e1a74b]())
- feat(idempotency): add HTTP integration for idempotency key handling with header extraction; include tests for key resolution ([d9952eecb394498]())
- feat(idempotency): implement idempotency service with key validation, expiration handling, and in-memory storage; add comprehensive tests and documentation ([75d8235893b19c3]())
- feat(resilience): implement resilience features with annotations for circuit breaker, rate limiter, bulkhead, and timeout; add actuator support and comprehensive tests ([880478a2151aad7]())
- feat(resilience): enhance resilience policies with caching, rate limiting, and fallback strategies; add comprehensive tests and documentation ([236f76d7efdb799]())
- feat(observability): add Micrometer Tracing support with TracerFacade integration and auto-configuration; includes README updates and tests ([179f4b3b63d878a]())
- feat(observability): add observability framework with structured logging, metrics, health checks, and log sanitization; includes comprehensive tests ([4483dba709ddd4a]())
- feat(otel): add OpenTelemetry decorators for HTTP client, messaging publisher, and consumer; include business metrics support and comprehensive tests ([e0d9976344f23ba]())
- feat(secrets-aws): add AWS Secrets Manager adapter for SecretStorePort with caching support, comprehensive README, and tests ([39915f99858143f]())
- feat(secrets-vault): add HashiCorp Vault adapter for SecretStorePort with support for versioned secrets, dynamic secrets, and secret rotation; includes comprehensive README and tests ([44f7cdadbfc915d]())
- feat(outbox): enhance JpaOutboxRepositoryAdapter with processing and retry capabilities, add tests and documentation ([40f42e3bd326a0f]())
- feat: add MongoDB adapter with pageable repository, filtering, sorting, and tests ([57f1ede69a3aa6b]())
- feat: implement in-memory pageable repository with filtering and sorting capabilities ([3ed8b468e16ccb6]())
- feat: add Azure Service Bus messaging adapter with publisher and consumer implementations ([f46ad1a10d773ae]())
- feat: add RabbitMQ messaging adapter with publisher and consumer implementations ([ceab567738f93e0]())
- feat(http): add support for multipart form data and streaming responses in HTTP clients ([97a5be3ae2ce46b]())
- feat(http): add WebClientHttpClientAdapter and related components ([e26ed95c50d8ef2]())
- feat(http): add OkHttp client adapter and related HTTP components ([3c2220962e46ff4]())
- feat: update dependency management in commons-bom and add comprehensive documentation ([e7a1a2cd1e6dfe2]())
- feat: add example documentation and code structure for Simple User Service ([a8c4f8ffe74e7cb]())
- feat: add C4 architecture diagrams and usage patterns documentation for Java Commons Platform ([0a30635235c9d6c]())
- feat: enhance CI/CD pipeline with static analysis, security scan, and release workflow ([c2ff4cdd331a5fe]())
- feat: add static analysis tools configuration and documentation ([60bb541d8e30af2]())
- feat: add JaCoCo code coverage integration and documentation ([dfb3bee4d41584d]())
- feat: implement framework-agnostic REST query layer for dynamic filtering, pagination, and sorting ([0b1a2d52d58a4a9]())
- feat: enhance PageableRepository with sorting and search capabilities ([38dd1a09a9acce6]())
- feat: add in-memory repository implementation with pageable support ([a584f58e6575e9f]())
- feat: enhance PageableRepository with specification support for dynamic queries ([914d71cb1298cae]())
- feat: implement JPA repository with pagination support and PostgreSQL integration ([14ff69fd9280c20]())
- feat: enhance outbox message entity with indexing and remove unused outbox processor classes ([8f1e57bba3899a8]())
- feat: implement outbox processing with DefaultOutboxProcessor and related components ([4f9bc97965c5ee5]())
- feat: implement outbox pattern with publisher, serializer, and metadata enricher ([a5eb6a0d8ca2dd8]())
- feat: add WebClient and RestTemplate context propagation with outbound headers ([1ce2410fa957761]())
- feat: add context-aware HTTP result mapping and response handling components ([4f42f81ad986eef]())
- feat: add HTTP result and problem mappers with response handling ([1c8e5561ca0e4f7]())
- feat: implement HTTP problem mapping and exception handling with HttpProblemMapper and ProblemExceptionHandler ([ec75bdd40e6a473]())
- feat: introduce context and utility classes for request handling, correlation, and logging ([3409b85f86bf043]())
- feat: add DDD context components including ActorProvider, AuditFactory, and CorrelationProvider with tests ([8a3228f7be9601e]())
- feat: introduce error handling framework with Problem, Result, and Exception classes ([039b0774514dd41]())
- feat: add commons-spring-starter-idempotency and commons-spring-starter-outbox modules with auto-configuration for idempotency and outbox processing ([dbc9bc2a54af530]())
- feat: add commons-adapters-persistence-jpa module with JPA entities and adapters for OutboxRepositoryPort and IdempotencyStorePort ([c4b4d6f156cc663]())
- feat: add idempotency and outbox modules with models, ports, and default processors ([05ca9d9ab234cbd]())
- feat: add resilience framework with Resilience4j implementation and auto-configuration support ([b7ee24496e9a7a8]())
- feat: add OpenTelemetry support with commons-adapters-otel and commons-spring-starter-otel modules ([47cdc42cb3c7bfd]())
- feat: implement error handling and observability features with custom exceptions and filters ([0adfebaa28f5c6c]())
- feat: add commons-adapters-web and commons-adapters-web-spring modules with initial configurations and response classes ([4948346894c5647]())
- feat: add initial ports modules for cache, messaging, http, persistence, and secrets ([eee9638c6cbece0]())
- feat: add initial configuration files for CI, Dependabot, and versioning documentation ([10fc8e23390167d]())
- feat: update .gitignore to include additional file types and directories ([140686c71fc0748]())

### 🐛 Bug Fixes
- fix: add missing newline at end of NamingConventionRules and TestOrganizationRules files ([e9076660ae1c2d3]())


### ♻️ Code Refactoring
### ♻️ Code Refactoring
### ♻️ Code Refactoring
### ♻️ Code Refactoring
### ♻️ Code Refactoring
### ♻️ Code Refactoring
### ♻️ Code Refactoring
- refactor: clean up imports and improve code formatting across multiple classes ([db91cd23929a990]())
- refactor: remove unused persistence classes and introduce pagination and query specifications ([46a5da673def604]())
- refactor: update dependencies in pom.xml for improved test scope and structure ([6b40bed254bf96c]())
- refactor: improve formatting and structure in OutboxMessageEntity and KernelIsolationArchTest ([2e419cd0cd98a50]())
- refactor: clean up formatting in KernelIsolationArchTest for improved readability ([0715c8e4a688fac]())
- refactor: streamline KernelIsolationArchTest structure and improve package import logic ([b83936667d9dbf9]())
- refactor: remove obsolete error handling classes and response structures ([4ffa18a068a87f9]())





