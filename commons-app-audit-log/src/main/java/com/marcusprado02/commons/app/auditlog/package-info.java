/**
 * Audit logging system for the commons library.
 *
 * <p>This module provides comprehensive audit logging capabilities with AOP-based automatic
 * capturing and flexible storage options.
 *
 * <h2>Core Concepts</h2>
 *
 * <ul>
 *   <li>{@link com.marcusprado02.commons.app.auditlog.AuditEvent} - Audit event model
 *   <li>{@link com.marcusprado02.commons.app.auditlog.AuditService} - Service for recording events
 *   <li>{@link com.marcusprado02.commons.app.auditlog.AuditRepository} - Storage abstraction
 *   <li>{@link com.marcusprado02.commons.app.auditlog.Audited} - Annotation for automatic auditing
 *   <li>{@link com.marcusprado02.commons.app.auditlog.aop.AuditAspect} - AOP interceptor
 *   <li>{@link com.marcusprado02.commons.app.auditlog.AuditQuery} - Query builder for searching
 * </ul>
 *
 * <h2>Quick Start</h2>
 *
 * <h3>Manual Audit Logging</h3>
 *
 * <pre>{@code
 * @Service
 * public class UserService {
 *     private final AuditService auditService;
 *
 *     public void createUser(User user) {
 *         // Create user...
 *
 *         auditService.audit(
 *             AuditEvent.builder()
 *                 .eventType("USER_CREATED")
 *                 .actor(getCurrentUser())
 *                 .action("create")
 *                 .resourceType("User")
 *                 .resourceId(user.getId())
 *                 .metadata(Map.of("email", user.getEmail()))
 *                 .build()
 *         );
 *     }
 * }
 * }</pre>
 *
 * <h3>Automatic Audit Logging with AOP</h3>
 *
 * <pre>{@code
 * @Service
 * public class UserService {
 *
 *     @Audited(
 *         eventType = "USER_CREATED",
 *         action = "create",
 *         resourceType = "User",
 *         resourceIdExpression = "#result.id"
 *     )
 *     public User createUser(User user) {
 *         // Method implementation
 *         return user;
 *     }
 *
 *     @Audited(
 *         eventType = "USER_UPDATED",
 *         action = "update",
 *         resourceType = "User",
 *         resourceIdParam = "userId",
 *         includeParameters = true
 *     )
 *     public User updateUser(String userId, UserUpdateDto dto) {
 *         // Method implementation
 *         return user;
 *     }
 * }
 * }</pre>
 *
 * <h3>Querying Audit Events</h3>
 *
 * <pre>{@code
 * AuditQuery query = AuditQuery.builder()
 *     .actor("user123")
 *     .eventType("USER_UPDATED")
 *     .from(Instant.now().minus(Duration.ofDays(7)))
 *     .to(Instant.now())
 *     .limit(100)
 *     .build();
 *
 * Result<List<AuditEvent>> events = repository.query(query);
 * }</pre>
 *
 * <h2>Spring Configuration</h2>
 *
 * <pre>{@code
 * @Configuration
 * @EnableAspectJAutoProxy
 * public class AuditConfig {
 *
 *     @Bean
 *     public AuditRepository auditRepository() {
 *         return new InMemoryAuditRepository(); // or database implementation
 *     }
 *
 *     @Bean
 *     public AuditService auditService(AuditRepository repository) {
 *         return new DefaultAuditService(repository);
 *     }
 *
 *     @Bean
 *     public ActorProvider actorProvider() {
 *         return () -> {
 *             Authentication auth = SecurityContextHolder.getContext().getAuthentication();
 *             return auth != null ? auth.getName() : "anonymous";
 *         };
 *     }
 *
 *     @Bean
 *     public AuditAspect auditAspect(AuditService auditService, ActorProvider actorProvider) {
 *         return new AuditAspect(auditService, actorProvider);
 *     }
 * }
 * }</pre>
 *
 * <h2>Storage Implementations</h2>
 *
 * <p>The module includes an in-memory repository for testing:
 *
 * <ul>
 *   <li>{@link com.marcusprado02.commons.app.auditlog.inmemory.InMemoryAuditRepository} -
 *       Thread-safe in-memory storage
 * </ul>
 *
 * <p>Custom implementations can be created for:
 *
 * <ul>
 *   <li>Relational databases (JPA, JDBC)
 *   <li>NoSQL databases (MongoDB, DynamoDB)
 *   <li>Search engines (Elasticsearch, OpenSearch)
 *   <li>Message queues (for async processing)
 * </ul>
 *
 * @see com.marcusprado02.commons.app.auditlog.AuditEvent
 * @see com.marcusprado02.commons.app.auditlog.AuditService
 * @see com.marcusprado02.commons.app.auditlog.Audited
 */
package com.marcusprado02.commons.app.auditlog;
