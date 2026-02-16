/**
 * Consul implementation of service discovery and registration.
 *
 * <p>This package provides a Consul-based adapter for the {@link
 * com.marcusprado02.commons.ports.servicediscovery.ServiceRegistry} port interface. It uses the
 * Consul HTTP API for service registration, health checking, and discovery.
 *
 * <h2>Features</h2>
 *
 * <ul>
 *   <li>Service registration with Consul agent
 *   <li>HTTP, TCP, and TTL health check support
 *   <li>Service discovery with health filtering
 *   <li>Metadata tags for service classification
 *   <li>Automatic service deregistration
 *   <li>TTL heartbeat management
 * </ul>
 *
 * <h2>Setup</h2>
 *
 * <pre>{@code
 * // Start Consul in development mode
 * docker run -d -p 8500:8500 consul:1.15 agent -dev -client=0.0.0.0
 *
 * // Create registry
 * ConsulClient consulClient = new ConsulClient("localhost", 8500);
 * ServiceRegistry registry = new ConsulServiceRegistry(consulClient);
 * }</pre>
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * // Register a service with HTTP health check
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
 * HealthCheck healthCheck = HealthCheck.http("https://192.168.1.100:8080/health")
 *     .interval(Duration.ofSeconds(10))
 *     .timeout(Duration.ofSeconds(5))
 *     .deregisterAfter(Duration.ofMinutes(1))
 *     .build();
 *
 * Result<Void> result = registry.register(instance, healthCheck);
 *
 * // Discover healthy instances
 * Result<List<ServiceInstance>> instances = registry.discover("payment-service");
 * instances.ifSuccess(list -> {
 *     for (ServiceInstance svc : list) {
 *         System.out.println("Found: " + svc.getUri());
 *     }
 * });
 *
 * // TTL heartbeat for TTL checks
 * registry.heartbeat("payment-01");
 *
 * // Deregister on shutdown
 * registry.deregister("payment-01");
 * }</pre>
 *
 * <h2>Health Check Types</h2>
 *
 * <ul>
 *   <li><b>HTTP</b>: Consul polls the HTTP endpoint periodically
 *   <li><b>TCP</b>: Consul attempts to establish a TCP connection
 *   <li><b>TTL</b>: Service must send periodic heartbeats via {@code heartbeat()}
 * </ul>
 *
 * <h2>Consul UI</h2>
 *
 * <p>Access the Consul UI at: {@code http://localhost:8500/ui}
 *
 * <h2>Dependencies</h2>
 *
 * <pre>{@code
 * <dependency>
 *     <groupId>com.ecwid.consul</groupId>
 *     <artifactId>consul-api</artifactId>
 * </dependency>
 * }</pre>
 *
 * @since 1.0.0
 * @author Marcus Prado
 */
package com.marcusprado02.commons.adapters.servicediscovery.consul;
