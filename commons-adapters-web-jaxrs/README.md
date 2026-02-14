# Commons Adapters - Web JAX-RS

Adaptadores JAX-RS para aplicações REST com suporte a Jersey, RESTEasy e outras implementações JAX-RS compatíveis.

## 📦 Instalação

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-adapters-web-jaxrs</artifactId>
    <version>${commons.version}</version>
</dependency>

<!-- Adicione uma implementação JAX-RS (Jersey ou RESTEasy) -->
<dependency>
    <groupId>org.glassfish.jersey.core</groupId>
    <artifactId>jersey-server</artifactId>
</dependency>
```

## 🎯 Funcionalidades

### 1. Exception Mappers (RFC 7807)

Mappers de exceções JAX-RS com suporte a **Problem Details (RFC 7807)**.

#### Exception Mappers Disponíveis

- **DomainExceptionMapper**: Mapeia `DomainException` para HTTP responses
- **IllegalArgumentExceptionMapper**: Mapeia `IllegalArgumentException` para 400 Bad Request
- **GenericExceptionMapper**: Fallback para exceções não tratadas (500 Internal Server Error)

#### Configuração

##### Jersey (ResourceConfig)

```java
@ApplicationPath("/api")
public class RestApplication extends ResourceConfig {
    
    public RestApplication() {
        // Resources
        packages("com.example.resources");
        
        // Exception Mappers
        HttpProblemMapper problemMapper = new DefaultHttpProblemMapper();
        register(new DomainExceptionMapper(problemMapper));
        register(IllegalArgumentExceptionMapper.class);
        register(GenericExceptionMapper.class);
    }
}
```

##### Jersey (Application)

```java
@ApplicationPath("/api")
public class RestApplication extends Application {
    
    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(
            // Resources
            UserResource.class,
            
            // Exception Mappers
            IllegalArgumentExceptionMapper.class,
            GenericExceptionMapper.class
        );
    }
    
    @Override
    public Set<Object> getSingletons() {
        HttpProblemMapper mapper = new DefaultHttpProblemMapper();
        return Set.of(new DomainExceptionMapper(mapper));
    }
}
```

##### RESTEasy (web.xml)

```xml
<context-param>
    <param-name>resteasy.providers</param-name>
    <param-value>
        com.marcusprado02.commons.adapters.web.jaxrs.exception.DomainExceptionMapper,
        com.marcusprado02.commons.adapters.web.jaxrs.exception.IllegalArgumentExceptionMapper,
        com.marcusprado02.commons.adapters.web.jaxrs.exception.GenericExceptionMapper
    </param-value>
</context-param>
```

#### Exemplo de Resposta

```json
{
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Invalid user data",
  "details": {},
  "meta": {
    "correlationId": "abc-123",
    "timestamp": "2026-02-13T20:00:00Z"
  }
}
```

---

### 2. Request/Response Filters

#### Correlation ID Filter

Gera ou propaga `X-Correlation-Id` automaticamente.

**Configuração:**

```java
@ApplicationPath("/api")
public class RestApplication extends ResourceConfig {
    
    public RestApplication() {
        packages("com.example.resources");
        
        // Filters
        register(CorrelationIdFilter.class);
    }
}
```

**Funcionalidades:**
- Gera novo correlation ID se não existir no request
- Propaga correlation ID existente
- Adiciona correlation ID ao MDC (SLF4J)
- Retorna correlation ID no response header

#### Request/Response Logging Filter

Logging detalhado de requisições e respostas.

**Configuração:**

```java
@ApplicationPath("/api")
public class RestApplication extends ResourceConfig {
    
    public RestApplication() {
        packages("com.example.resources");
        
        // Filters (order matters)
        register(CorrelationIdFilter.class);
        register(RequestResponseLoggingFilter.class);
    }
}
```

**application.properties:**

```properties
# Ativar logs DEBUG para ver requisições/respostas
logging.level.com.marcusprado02.commons.adapters.web.jaxrs.filter.RequestResponseLoggingFilter=DEBUG
```

**Exemplo de Log:**

```
>>> JAX-RS Request: GET /api/users?page=0&size=20 | Headers: X-Correlation-Id=abc-123, Content-Type=application/json
<<< JAX-RS Response: 200 | Duration: 45ms | Headers: X-Correlation-Id=abc-123
```

---

### 3. Context Propagation (Client Filter)

Propaga headers de contexto em chamadas downstream.

#### Configuração

```java
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;

// Criar client com context propagation
Client client = ClientBuilder.newClient();
client.register(ContextPropagationFilter.class);

// Fazer chamadas
Response response = client.target("http://downstream-service/api/users")
    .request()
    .get();
```

#### Headers Propagados

- `X-Correlation-Id` - Correlation ID da requisição
- `X-Tenant-Id` - Tenant ID (se presente no MDC)
- `X-Actor-Id` - Actor ID (se presente no MDC)

---

## 📁 Estrutura do Módulo

```
commons-adapters-web-jaxrs/
├── exception/
│   ├── DomainExceptionMapper.java          # Mapper para DomainException
│   ├── IllegalArgumentExceptionMapper.java # Mapper para IllegalArgumentException
│   └── GenericExceptionMapper.java         # Fallback genérico (500)
├── filter/
│   ├── CorrelationIdFilter.java            # Propagação de correlation ID
│   └── RequestResponseLoggingFilter.java   # Logging de req/res
└── client/
    └── ContextPropagationFilter.java       # Propagação de contexto (client)
```

---

## 🔧 Configuração Completa

### Jersey Application

```java
@ApplicationPath("/api")
public class RestApplication extends ResourceConfig {
    
    public RestApplication() {
        // Scan resources
        packages("com.example.resources");
        
        // Exception Mappers
        HttpProblemMapper problemMapper = new DefaultHttpProblemMapper();
        register(new DomainExceptionMapper(problemMapper));
        register(IllegalArgumentExceptionMapper.class);
        register(GenericExceptionMapper.class);
        
        // Filters
        register(CorrelationIdFilter.class);
        register(RequestResponseLoggingFilter.class);
        
        // JSON support
        register(org.glassfish.jersey.media.json.JsonBindingFeature.class);
    }
}
```

### RESTEasy web.xml

```xml
<web-app>
    <!-- RESTEasy Servlet -->
    <servlet>
        <servlet-name>resteasy-servlet</servlet-name>
        <servlet-class>
            org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher
        </servlet-class>
        <init-param>
            <param-name>jakarta.ws.rs.Application</param-name>
            <param-value>com.example.RestApplication</param-value>
        </init-param>
    </servlet>
    
    <servlet-mapping>
        <servlet-name>resteasy-servlet</servlet-name>
        <url-pattern>/api/*</url-pattern>
    </servlet-mapping>
    
    <!-- Providers -->
    <context-param>
        <param-name>resteasy.providers</param-name>
        <param-value>
            com.marcusprado02.commons.adapters.web.jaxrs.exception.DomainExceptionMapper,
            com.marcusprado02.commons.adapters.web.jaxrs.exception.IllegalArgumentExceptionMapper,
            com.marcusprado02.commons.adapters.web.jaxrs.exception.GenericExceptionMapper,
            com.marcusprado02.commons.adapters.web.jaxrs.filter.CorrelationIdFilter,
            com.marcusprado02.commons.adapters.web.jaxrs.filter.RequestResponseLoggingFilter
        </param-value>
    </context-param>
</web-app>
```

---

## 🧪 Testing

### Jersey Test Framework

```java
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.Test;

class MyResourceTest extends JerseyTest {
    
    @Override
    protected Application configure() {
        return new ResourceConfig(MyResource.class)
            .register(CorrelationIdFilter.class)
            .register(new DomainExceptionMapper(new TestProblemMapper()));
    }
    
    @Test
    void shouldPropagateCorrelationId() {
        Response response = target("/users")
            .request()
            .header("X-Correlation-Id", "test-123")
            .get();
        
        assertEquals(200, response.getStatus());
        assertEquals("test-123", response.getHeaderString("X-Correlation-Id"));
    }
}
```

### Exception Mapper Test

```java
@Test
void shouldMapDomainExceptionToProblemResponse() {
    Response response = target("/users/invalid").request().get();
    
    assertEquals(400, response.getStatus());
    HttpProblemResponse problem = response.readEntity(HttpProblemResponse.class);
    assertEquals("USER_NOT_FOUND", problem.code());
}
```

---

## 🔗 Dependências

- `commons-adapters-web` → Interfaces e DTOs base (HttpProblemMapper, HttpProblemResponse)
- `commons-kernel-errors` → DomainException, Problem
- `commons-app-observability` → CorrelationId, ContextKeys
- Jakarta JAX-RS API → Interfaces JAX-RS padrão
- Jersey (provided) → Implementação de referência
- Jersey Test Framework (test) → Testes integrados

---

## 🆚 Comparação: JAX-RS vs Spring MVC

| Funcionalidade | JAX-RS | Spring MVC |
|---|---|---|
| Exception Handling | `@Provider` ExceptionMapper | `@RestControllerAdvice` |
| Request Filtering | `ContainerRequestFilter` | Servlet Filter / HandlerInterceptor |
| Response Filtering | `ContainerResponseFilter` | Servlet Filter / ResponseBodyAdvice |
| Client Filtering | `ClientRequestFilter` | RestTemplate Interceptor |
| Dependency Injection | CDI / HK2 / Spring | Spring |
| Standards | Jakarta EE | Spring Framework |

---

## 📚 Veja Também

- [commons-adapters-web-spring](../commons-adapters-web-spring) → Spring MVC implementation
- [commons-adapters-web](../commons-adapters-web) → Base interfaces
- [commons-kernel-errors](../commons-kernel-errors) → Error handling DDD

---

## 📝 Licença

MIT License - veja [LICENSE](../LICENSE)
