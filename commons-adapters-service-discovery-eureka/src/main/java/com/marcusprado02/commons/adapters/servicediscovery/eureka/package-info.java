/**
 * Netflix Eureka service discovery adapter.
 *
 * <p>This package provides an implementation of the {@link
 * com.marcusprado02.commons.ports.servicediscovery.ServiceRegistry} interface using Netflix Eureka
 * server for service registration and discovery.
 *
 * <p><strong>Main Components:</strong>
 *
 * <ul>
 *   <li>{@link com.marcusprado02.commons.adapters.servicediscovery.eureka.EurekaServiceRegistry} -
 *       Eureka-based service registry implementation
 * </ul>
 *
 * <p><strong>Features:</strong>
 *
 * <ul>
 *   <li>Service registration with Eureka server
 *   <li>Automatic heartbeat and renewal
 *   <li>Service discovery with status filtering
 *   <li>Instance metadata support
 *   <li>Configurable lease and fetch intervals
 *   <li>Result-based error handling
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 *
 * <pre>{@code
 * // Create Eureka registry
 * EurekaServiceRegistry registry = EurekaServiceRegistry.builder()
 *     .eurekaServerUrl("http://localhost:8761/eureka")
 *     .appName("my-service")
 *     .hostName("localhost")
 *     .port(8080)
 *     .renewalInterval(Duration.ofSeconds(30))
 *     .build();
 *
 * // Register service
 * ServiceInstance instance = ServiceInstance.builder()
 *     .serviceId("my-service")
 *     .instanceId("my-service-01")
 *     .host("localhost")
 *     .port(8080)
 *     .addMetadata("version", "1.0.0")
 *     .build();
 *
 * Result<Void> result = registry.register(instance);
 *
 * // Discover services
 * Result<List<ServiceInstance>> instances = registry.discover("user-service");
 * if (instances.isOk()) {
 *     instances.getOrNull().forEach(inst ->
 *         System.out.println("Found: " + inst.host() + ":" + inst.port())
 *     );
 * }
 *
 * // Shutdown
 * registry.shutdown();
 * }</pre>
 *
 * <p><strong>Eureka Server Setup:</strong>
 *
 * <pre>{@code
 * # Docker Compose
 * version: '3.8'
 * services:
 *   eureka:
 *     image: springcloud/eureka:latest
 *     ports:
 *       - "8761:8761"
 *     environment:
 *       - EUREKA_INSTANCE_HOSTNAME=eureka
 *       - EUREKA_CLIENT_REGISTER_WITH_EUREKA=false
 *       - EUREKA_CLIENT_FETCH_REGISTRY=false
 * }</pre>
 *
 * <p><strong>Configuration Options:</strong>
 *
 * <ul>
 *   <li>{@code eurekaServerUrl} - URL of the Eureka server (default: http://localhost:8761/eureka)
 *   <li>{@code registryFetchInterval} - How often to fetch the registry (default: 30s)
 *   <li>{@code renewalInterval} - How often to send heartbeats (default: 30s)
 *   <li>{@code expirationDuration} - When instances expire without renewal (default: 90s)
 *   <li>{@code replicationInterval} - How often to replicate instance info (default: 30s)
 * </ul>
 *
 * <p><strong>Health Checks:</strong>
 *
 * <p>Eureka uses a heartbeat mechanism where the client sends periodic renewals to the server. If
 * the server doesn't receive a renewal within the expiration duration, the instance is marked as
 * DOWN and eventually removed from the registry.
 *
 * <p><strong>High Availability:</strong>
 *
 * <p>For production deployments, configure multiple Eureka servers for high availability:
 *
 * <pre>{@code
 * EurekaServiceRegistry registry = EurekaServiceRegistry.builder()
 *     .eurekaServerUrl("http://eureka1:8761/eureka,http://eureka2:8761/eureka")
 *     .appName("my-service")
 *     .build();
 * }</pre>
 *
 * <p><strong>Integration with Spring Boot:</strong>
 *
 * <pre>{@code
 * @Configuration
 * public class EurekaConfig {
 *
 *     @Bean
 *     public EurekaServiceRegistry eurekaServiceRegistry(
 *         @Value("${eureka.client.serviceUrl.defaultZone}") String eurekaUrl,
 *         @Value("${spring.application.name}") String appName) {
 *
 *         return EurekaServiceRegistry.builder()
 *             .eurekaServerUrl(eurekaUrl)
 *             .appName(appName)
 *             .build();
 *     }
 * }
 * }</pre>
 *
 * @see com.marcusprado02.commons.ports.servicediscovery.ServiceRegistry
 * @see com.marcusprado02.commons.ports.servicediscovery.ServiceInstance
 * @see com.marcusprado02.commons.adapters.servicediscovery.eureka.EurekaServiceRegistry
 */
package com.marcusprado02.commons.adapters.servicediscovery.eureka;
