# Port: Service Discovery

## Visão Geral

`commons-ports-service-discovery` define contratos para service registry e discovery (Consul, Eureka), essencial para microservices dinâmicos.

**Quando usar:**
- Service-to-service communication
- Load balancing
- Health checking
- Dynamic service locations
- Multi-instance deployments

**Implementações disponíveis:**
- `commons-adapters-service-discovery-consul` - HashiCorp Consul
- `commons-adapters-service-discovery-eureka` - Netflix Eureka

---

## 📦 Instalação

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-ports-service-discovery</artifactId>
    <version>${commons.version}</version>
</dependency>

<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-adapters-service-discovery-consul</artifactId>
    <version>${commons.version}</version>
</dependency>
```

---

## 🔍 ServiceDiscovery Interface

```java
public interface ServiceDiscovery {
    
    /**
     * Registra serviço.
     */
    Result<Void> register(ServiceRegistration registration);
    
    /**
     * Remove registro.
     */
    Result<Void> deregister(String serviceId);
    
    /**
     * Descobre instâncias de serviço.
     */
    Result<List<ServiceInstance>> discover(String serviceName);
    
    /**
     * Busca instância saudável (load balancing).
     */
    Result<ServiceInstance> findHealthyInstance(String serviceName);
}

public record ServiceRegistration(
    String serviceId,
    String serviceName,
    String host,
    int port,
    Map<String, String> metadata,
    HealthCheck healthCheck
) {}

public record ServiceInstance(
    String id,
    String name,
    String host,
    int port,
    boolean healthy,
    Map<String, String> metadata
) {
    public String url() {
        return "http://" + host + ":" + port;
    }
}
```

---

## 💡 Usage Examples

### Service Registration

```java
@Service
public class ServiceRegistrationService {
    
    private final ServiceDiscovery serviceDiscovery;
    
    @EventListener(ApplicationReadyEvent.class)
    public void registerService() {
        ServiceRegistration registration = new ServiceRegistration(
            "order-service-1",
            "order-service",
            "localhost",
            8080,
            Map.of("version", "1.0.0", "environment", "production"),
            new HealthCheck("http://localhost:8080/actuator/health", Duration.ofSeconds(10))
        );
        
        serviceDiscovery.register(registration);
    }
    
    @PreDestroy
    public void deregisterService() {
        serviceDiscovery.deregister("order-service-1");
    }
}
```

### Service-to-Service Call

```java
@Service
public class OrderService {
    
    private final ServiceDiscovery serviceDiscovery;
    private final HttpClient httpClient;
    
    public Result<Payment> createPayment(Order order) {
        // Discover payment service
        Result<ServiceInstance> instanceResult = serviceDiscovery
            .findHealthyInstance("payment-service");
        
        if (instanceResult.isError()) {
            return instanceResult.mapError();
        }
        
        ServiceInstance instance = instanceResult.get();
        
        // Call service
        HttpRequest request = HttpRequest.builder()
            .method(HttpMethod.POST)
            .url(instance.url() + "/api/v1/payments")
            .body(buildPaymentRequest(order))
            .build();
        
        return httpClient.execute(request)
            .andThen(response -> parsePayment(response.body()));
    }
}
```

---

## Ver Também

- [Consul Adapter](../../../commons-adapters-service-discovery-consul/) - Consul implementation
- [Eureka Adapter](../../../commons-adapters-service-discovery-eureka/) - Eureka implementation
- [HTTP](./http.md) - HTTP client
