/**
 * Port interfaces for service discovery and registration.
 *
 * <p>This package provides framework-agnostic abstractions for service discovery patterns, enabling
 * microservices to find and communicate with each other without hardcoded endpoints.
 *
 * <h2>Core Abstractions</h2>
 *
 * <ul>
 *   <li>{@link com.marcusprado02.commons.ports.servicediscovery.ServiceRegistry} - Main interface
 *       for service registration and discovery
 *   <li>{@link com.marcusprado02.commons.ports.servicediscovery.ServiceInstance} - Represents a
 *       running service instance
 *   <li>{@link com.marcusprado02.commons.ports.servicediscovery.HealthCheck} - Health check
 *       configuration
 * </ul>
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * // Register a service
 * ServiceInstance instance = ServiceInstance.builder()
 *     .serviceId("payment-service")
 *     .instanceId("payment-01")
 *     .host("192.168.1.100")
 *     .port(8080)
 *     .secure(true)
 *     .addMetadata("version", "1.2.0")
 *     .addMetadata("region", "us-east")
 *     .build();
 *
 * HealthCheck check = HealthCheck.http("https://192.168.1.100:8080/health")
 *     .interval(Duration.ofSeconds(10))
 *     .timeout(Duration.ofSeconds(5))
 *     .deregisterAfter(Duration.ofMinutes(1))
 *     .build();
 *
 * Result<Void> result = registry.register(instance, check);
 *
 * // Discover services
 * Result<List<ServiceInstance>> instances = registry.discover("payment-service");
 * instances.ifSuccess(list -> {
 *     ServiceInstance first = list.get(0);
 *     String url = first.getUri(); // https://192.168.1.100:8080
 * });
 *
 * // Send heartbeat (TTL checks)
 * registry.heartbeat("payment-01");
 *
 * // Deregister
 * registry.deregister("payment-01");
 * }</pre>
 *
 * <h2>Health Check Types</h2>
 *
 * <ul>
 *   <li><b>HTTP</b>: Registry polls HTTP endpoint (e.g., /health)
 *   <li><b>TCP</b>: Registry attempts TCP connection
 *   <li><b>TTL</b>: Service must send periodic heartbeats
 * </ul>
 *
 * <h2>Implementations</h2>
 *
 * <ul>
 *   <li>commons-adapters-service-discovery-consul - Consul implementation
 *   <li>commons-adapters-service-discovery-eureka - Netflix Eureka implementation (planned)
 * </ul>
 *
 * @since 1.0.0
 * @author Marcus Prado
 */
package com.marcusprado02.commons.ports.servicediscovery;
