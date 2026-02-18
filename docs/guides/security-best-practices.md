# Security Best Practices Guide

## Overview

This guide covers **security best practices** for building secure microservices with the Commons library.

**Key Topics:**
- Authentication & Authorization
- JWT token management
- OAuth2 flows
- Secrets management
- Encryption at rest/in transit
- OWASP Top 10 mitigations
- API security
- Security testing

---

## 🔐 Authentication & Authorization

### JWT Token Generation

```java
@Service
public class JwtTokenService {
    
    private final String secretKey;
    private final Duration accessTokenExpiration = Duration.ofMinutes(15);
    private final Duration refreshTokenExpiration = Duration.ofDays(7);
    
    public JwtTokenService(@Value("${jwt.secret}") String secretKey) {
        this.secretKey = secretKey;
    }
    
    public Result<String> generateAccessToken(User user) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secretKey);
            
            String token = JWT.create()
                .withIssuer("my-app")
                .withSubject(user.id().value())
                .withClaim("email", user.email())
                .withClaim("roles", user.roles().stream()
                    .map(Role::name)
                    .toList()
                )
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plus(accessTokenExpiration))
                .sign(algorithm);
            
            return Result.ok(token);
            
        } catch (Exception e) {
            log.error("Failed to generate JWT token")
                .exception(e)
                .log();
            
            return Result.error(Error.of("TOKEN_ERROR", e.getMessage()));
        }
    }
    
    public Result<String> generateRefreshToken(User user) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secretKey);
            
            String token = JWT.create()
                .withIssuer("my-app")
                .withSubject(user.id().value())
                .withClaim("type", "refresh")
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plus(refreshTokenExpiration))
                .sign(algorithm);
            
            return Result.ok(token);
            
        } catch (Exception e) {
            return Result.error(Error.of("TOKEN_ERROR", e.getMessage()));
        }
    }
}
```

### JWT Token Validation

```java
@Component
public class JwtTokenValidator {
    
    private final String secretKey;
    private final JWTVerifier verifier;
    
    public JwtTokenValidator(@Value("${jwt.secret}") String secretKey) {
        this.secretKey = secretKey;
        Algorithm algorithm = Algorithm.HMAC256(secretKey);
        this.verifier = JWT.require(algorithm)
            .withIssuer("my-app")
            .build();
    }
    
    public Result<DecodedJWT> validate(String token) {
        try {
            DecodedJWT jwt = verifier.verify(token);
            
            // Check expiration
            if (jwt.getExpiresAt().before(new Date())) {
                return Result.error(Error.of("TOKEN_EXPIRED", "Token has expired"));
            }
            
            return Result.ok(jwt);
            
        } catch (JWTVerificationException e) {
            log.warn("Invalid JWT token")
                .exception(e)
                .log();
            
            return Result.error(Error.of("INVALID_TOKEN", "Invalid token"));
        }
    }
    
    public Result<UserPrincipal> extractPrincipal(String token) {
        Result<DecodedJWT> jwtResult = validate(token);
        
        if (jwtResult.isError()) {
            return Result.error(jwtResult.error());
        }
        
        DecodedJWT jwt = jwtResult.get();
        
        String userId = jwt.getSubject();
        String email = jwt.getClaim("email").asString();
        List<String> roles = jwt.getClaim("roles").asList(String.class);
        
        UserPrincipal principal = new UserPrincipal(
            UserId.from(userId),
            email,
            roles.stream().map(Role::from).toList()
        );
        
        return Result.ok(principal);
    }
}
```

### Spring Security Integration

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    private final JwtTokenValidator tokenValidator;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())  // Disable for API
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/api/auth/register").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(
                jwtAuthenticationFilter(),
                UsernamePasswordAuthenticationFilter.class
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint())
                .accessDeniedHandler(accessDeniedHandler())
            );
        
        return http.build();
    }
    
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(tokenValidator);
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("https://app.example.com"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setExposedHeaders(List.of("X-Total-Count"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}

// JWT Authentication Filter
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtTokenValidator tokenValidator;
    
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        
        String token = extractToken(request);
        
        if (token != null) {
            Result<UserPrincipal> principalResult = tokenValidator.extractPrincipal(token);
            
            if (principalResult.isSuccess()) {
                UserPrincipal principal = principalResult.get();
                
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.authorities()
                    );
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

---

## 🔑 OAuth2 Integration

### OAuth2 Authorization Code Flow

```java
@Configuration
@EnableWebSecurity
public class OAuth2Config {
    
    @Bean
    public SecurityFilterChain oauth2FilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/oauth2/**", "/login/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard")
                .failureUrl("/login?error=true")
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService())
                )
            )
            .oauth2Client(Customizer.withDefaults());
        
        return http.build();
    }
    
    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> customOAuth2UserService() {
        return new CustomOAuth2UserService();
    }
}

// Custom OAuth2 User Service
@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    
    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    private final UserRepository userRepository;
    
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oauth2User = delegate.loadUser(userRequest);
        
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        
        // Find or create user
        Optional<User> existingUser = userRepository.findByEmail(email);
        
        if (existingUser.isEmpty()) {
            Result<User> userResult = User.create(email, name);
            if (userResult.isSuccess()) {
                userRepository.save(userResult.get());
            }
        }
        
        return oauth2User;
    }
}
```

### OAuth2 Client Credentials Flow

```java
@Service
public class OAuth2ClientService {
    
    private final RestClient restClient;
    private final String tokenEndpoint;
    private final String clientId;
    private final String clientSecret;
    
    private String accessToken;
    private Instant tokenExpiration;
    
    public Result<String> getAccessToken() {
        // Check if token is still valid
        if (accessToken != null && Instant.now().isBefore(tokenExpiration)) {
            return Result.ok(accessToken);
        }
        
        // Request new token
        return refreshAccessToken();
    }
    
    private Result<String> refreshAccessToken() {
        try {
            Map<String, String> params = Map.of(
                "grant_type", "client_credentials",
                "client_id", clientId,
                "client_secret", clientSecret,
                "scope", "api.read api.write"
            );
            
            HttpResponse response = restClient.post(tokenEndpoint)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body(encodeFormParams(params))
                .execute();
            
            if (response.status() != 200) {
                return Result.error(Error.of("TOKEN_ERROR", "Failed to get token"));
            }
            
            TokenResponse tokenResponse = objectMapper.readValue(
                response.body(),
                TokenResponse.class
            );
            
            this.accessToken = tokenResponse.accessToken();
            this.tokenExpiration = Instant.now().plusSeconds(tokenResponse.expiresIn() - 60);
            
            return Result.ok(accessToken);
            
        } catch (Exception e) {
            log.error("Failed to refresh OAuth2 token")
                .exception(e)
                .log();
            
            return Result.error(Error.of("TOKEN_ERROR", e.getMessage()));
        }
    }
}
```

---

## 🔒 Secrets Management

### Secrets Provider Integration

```java
@Configuration
public class SecretsConfig {
    
    @Bean
    public SecretsProvider secretsProvider(
        @Value("${secrets.provider}") String provider
    ) {
        return switch (provider) {
            case "aws" -> awsSecretsProvider();
            case "azure" -> azureSecretsProvider();
            case "vault" -> vaultSecretsProvider();
            default -> throw new IllegalArgumentException("Unknown provider: " + provider);
        };
    }
    
    @Bean
    @ConditionalOnProperty(name = "secrets.provider", havingValue = "aws")
    public SecretsProvider awsSecretsProvider() {
        SecretsManagerClient client = SecretsManagerClient.builder()
            .region(Region.US_EAST_1)
            .build();
        
        return new AwsSecretsManagerProvider(client);
    }
    
    @Bean
    @ConditionalOnProperty(name = "secrets.provider", havingValue = "azure")
    public SecretsProvider azureSecretsProvider() {
        SecretClient client = new SecretClientBuilder()
            .vaultUrl("https://my-vault.vault.azure.net")
            .credential(new DefaultAzureCredentialBuilder().build())
            .buildClient();
        
        return new AzureKeyVaultProvider(client);
    }
}

@Service
public class DatabaseCredentialsService {
    
    private final SecretsProvider secretsProvider;
    
    public Result<DatabaseCredentials> getCredentials() {
        Result<String> secretResult = secretsProvider.getSecret("database/credentials");
        
        if (secretResult.isError()) {
            return Result.error(secretResult.error());
        }
        
        try {
            CredentialsJson json = objectMapper.readValue(
                secretResult.get(),
                CredentialsJson.class
            );
            
            DatabaseCredentials credentials = new DatabaseCredentials(
                json.username(),
                json.password(),
                json.host(),
                json.port()
            );
            
            return Result.ok(credentials);
            
        } catch (Exception e) {
            return Result.error(Error.of("PARSE_ERROR", "Failed to parse credentials"));
        }
    }
}
```

### Secret Rotation

```java
@Service
public class SecretRotationService {
    
    private final SecretsProvider secretsProvider;
    private final EventPublisher eventPublisher;
    
    @Scheduled(cron = "0 0 2 * * ?")  // 2 AM daily
    public void rotateSecrets() {
        log.info("Starting secret rotation");
        
        List<String> secretsToRotate = List.of(
            "database/credentials",
            "api-keys/stripe",
            "api-keys/sendgrid"
        );
        
        for (String secretName : secretsToRotate) {
            Result<Void> result = rotateSecret(secretName);
            
            if (result.isError()) {
                log.error("Failed to rotate secret")
                    .field("secretName", secretName)
                    .field("error", result.error().message())
                    .log();
                
                eventPublisher.publish(new SecretRotationFailedEvent(secretName));
            } else {
                log.info("Secret rotated successfully")
                    .field("secretName", secretName)
                    .log();
                
                eventPublisher.publish(new SecretRotatedEvent(secretName));
            }
        }
    }
    
    private Result<Void> rotateSecret(String secretName) {
        // 1. Generate new secret value
        String newValue = generateSecureValue();
        
        // 2. Update in secrets manager
        Result<Void> updateResult = secretsProvider.updateSecret(secretName, newValue);
        
        if (updateResult.isError()) {
            return updateResult;
        }
        
        // 3. Trigger application refresh
        // (Spring Cloud Config, restart pods, etc.)
        
        return Result.ok();
    }
    
    private String generateSecureValue() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
```

---

## 🔐 Encryption

### Data Encryption at Rest

```java
@Service
public class EncryptionService {
    
    private final SecretKey encryptionKey;
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    
    public EncryptionService(@Value("${encryption.key}") String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        this.encryptionKey = new SecretKeySpec(keyBytes, "AES");
    }
    
    public Result<String> encrypt(String plaintext) {
        try {
            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);
            
            // Encrypt
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, spec);
            
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            
            // Combine IV + ciphertext
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
            
            String encoded = Base64.getEncoder().encodeToString(combined);
            
            return Result.ok(encoded);
            
        } catch (Exception e) {
            log.error("Encryption failed")
                .exception(e)
                .log();
            
            return Result.error(Error.of("ENCRYPTION_ERROR", e.getMessage()));
        }
    }
    
    public Result<String> decrypt(String encrypted) {
        try {
            byte[] combined = Base64.getDecoder().decode(encrypted);
            
            // Extract IV and ciphertext
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, ciphertext, 0, ciphertext.length);
            
            // Decrypt
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, spec);
            
            byte[] plaintext = cipher.doFinal(ciphertext);
            
            return Result.ok(new String(plaintext, StandardCharsets.UTF_8));
            
        } catch (Exception e) {
            log.error("Decryption failed")
                .exception(e)
                .log();
            
            return Result.error(Error.of("DECRYPTION_ERROR", e.getMessage()));
        }
    }
}

// JPA Attribute Converter
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {
    
    private final EncryptionService encryptionService;
    
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        
        Result<String> result = encryptionService.encrypt(attribute);
        return result.getOrThrow();
    }
    
    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        
        Result<String> result = encryptionService.decrypt(dbData);
        return result.getOrThrow();
    }
}

// Usage in entity
@Entity
public class CustomerEntity {
    
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "ssn")
    private String ssn;  // Encrypted at rest
    
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "credit_card")
    private String creditCard;  // Encrypted at rest
}
```

### Envelope Encryption

```java
@Service
public class EnvelopeEncryptionService {
    
    private final SecretsProvider secretsProvider;
    
    public Result<EncryptedData> encrypt(String plaintext) {
        try {
            // 1. Generate data encryption key (DEK)
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            SecretKey dek = keyGen.generateKey();
            
            // 2. Encrypt plaintext with DEK
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.ENCRYPT_MODE, dek, spec);
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            
            // 3. Encrypt DEK with master key (from KMS)
            Result<String> masterKeyResult = secretsProvider.getSecret("master-key");
            if (masterKeyResult.isError()) {
                return Result.error(masterKeyResult.error());
            }
            
            byte[] masterKeyBytes = Base64.getDecoder().decode(masterKeyResult.get());
            SecretKey masterKey = new SecretKeySpec(masterKeyBytes, "AES");
            
            Cipher kekCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            kekCipher.init(Cipher.ENCRYPT_MODE, masterKey);
            byte[] encryptedDek = kekCipher.doFinal(dek.getEncoded());
            
            // 4. Return envelope (encrypted DEK + IV + ciphertext)
            EncryptedData envelope = new EncryptedData(
                Base64.getEncoder().encodeToString(encryptedDek),
                Base64.getEncoder().encodeToString(iv),
                Base64.getEncoder().encodeToString(ciphertext)
            );
            
            return Result.ok(envelope);
            
        } catch (Exception e) {
            return Result.error(Error.of("ENCRYPTION_ERROR", e.getMessage()));
        }
    }
    
    public Result<String> decrypt(EncryptedData envelope) {
        try {
            // 1. Get master key from KMS
            Result<String> masterKeyResult = secretsProvider.getSecret("master-key");
            if (masterKeyResult.isError()) {
                return Result.error(masterKeyResult.error());
            }
            
            byte[] masterKeyBytes = Base64.getDecoder().decode(masterKeyResult.get());
            SecretKey masterKey = new SecretKeySpec(masterKeyBytes, "AES");
            
            // 2. Decrypt DEK with master key
            Cipher kekCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            kekCipher.init(Cipher.DECRYPT_MODE, masterKey);
            byte[] dekBytes = kekCipher.doFinal(Base64.getDecoder().decode(envelope.encryptedKey()));
            SecretKey dek = new SecretKeySpec(dekBytes, "AES");
            
            // 3. Decrypt ciphertext with DEK
            byte[] iv = Base64.getDecoder().decode(envelope.iv());
            byte[] ciphertext = Base64.getDecoder().decode(envelope.ciphertext());
            
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.DECRYPT_MODE, dek, spec);
            byte[] plaintext = cipher.doFinal(ciphertext);
            
            return Result.ok(new String(plaintext, StandardCharsets.UTF_8));
            
        } catch (Exception e) {
            return Result.error(Error.of("DECRYPTION_ERROR", e.getMessage()));
        }
    }
}
```

---

## 🛡️ OWASP Top 10 Mitigations

### 1. SQL Injection Prevention

```java
// ❌ BAD: SQL Injection vulnerable
@Repository
public class UserRepository {
    
    public User findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = '" + email + "'";  // ❌ VULNERABLE!
        return jdbcTemplate.queryForObject(sql, userRowMapper);
    }
}

// ✅ GOOD: Parameterized query
@Repository
public class UserRepository {
    
    public User findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        return jdbcTemplate.queryForObject(sql, userRowMapper, email);
    }
}

// ✅ GOOD: JPA with named parameters
@Repository
public interface UserJpaRepository extends JpaRepository<UserEntity, String> {
    
    @Query("SELECT u FROM UserEntity u WHERE u.email = :email")
    Optional<UserEntity> findByEmail(@Param("email") String email);
}
```

### 2. XSS Prevention

```java
@Service
public class HtmlSanitizer {
    
    private final PolicyFactory policy;
    
    public HtmlSanitizer() {
        // Allow only safe HTML tags
        this.policy = new HtmlPolicyBuilder()
            .allowElements("p", "br", "b", "i", "u", "strong", "em")
            .allowAttributes("href").onElements("a")
            .allowStandardUrlProtocols()
            .requireRelNofollowOnLinks()
            .toFactory();
    }
    
    public String sanitize(String untrustedHtml) {
        return policy.sanitize(untrustedHtml);
    }
}

@RestController
public class CommentController {
    
    private final HtmlSanitizer sanitizer;
    
    @PostMapping("/comments")
    public ResponseEntity<CommentDto> createComment(
        @RequestBody @Valid CreateCommentRequest request
    ) {
        // Sanitize user input
        String safeContent = sanitizer.sanitize(request.content());
        
        Comment comment = Comment.create(safeContent);
        // ... save comment
        
        return ResponseEntity.ok(CommentDto.from(comment));
    }
}

// Thymeleaf template (auto-escapes by default)
// <p th:text="${comment.content}"></p>  ✅ Safe (escaped)
// <p th:utext="${comment.content}"></p> ❌ Unsafe (unescaped)
```

### 3. CSRF Protection

```java
@Configuration
public class CsrfConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
            )
            .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class);
        
        return http.build();
    }
}

// CSRF Cookie Filter (for SPA)
final class CsrfCookieFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        
        CsrfToken csrfToken = (CsrfToken) request.getAttribute("_csrf");
        
        // Render token value to a cookie
        csrfToken.getToken();
        
        filterChain.doFilter(request, response);
    }
}
```

### 4. Sensitive Data Exposure

```java
// ✅ GOOD: Never log sensitive data
@Service
public class PaymentService {
    
    public Result<Payment> processPayment(PaymentRequest request) {
        // ❌ DON'T: log.info("Processing payment: {}", request);
        
        log.info("Processing payment")
            .field("paymentId", request.paymentId())
            .field("amount", request.amount())
            // ❌ DON'T log: creditCard, cvv, ssn
            .log();
        
        // ... process payment
    }
}

// ✅ GOOD: Mask sensitive fields in DTOs
public record PaymentDto(
    String paymentId,
    double amount,
    String maskedCardNumber  // "****-****-****-1234"
) {
    public static PaymentDto from(Payment payment) {
        return new PaymentDto(
            payment.id().value(),
            payment.amount(),
            maskCardNumber(payment.cardNumber())
        );
    }
    
    private static String maskCardNumber(String cardNumber) {
        if (cardNumber.length() < 4) {
            return "****";
        }
        return "****-****-****-" + cardNumber.substring(cardNumber.length() - 4);
    }
}
```

### 5. Broken Access Control

```java
@Service
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final SecurityContext securityContext;
    
    public Result<Order> getOrder(OrderId orderId) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        
        if (orderOpt.isEmpty()) {
            return Result.error(Error.of("NOT_FOUND", "Order not found"));
        }
        
        Order order = orderOpt.get();
        
        // ✅ GOOD: Check ownership
        UserPrincipal principal = securityContext.getCurrentUser();
        
        if (!order.customerId().equals(principal.userId())) {
            log.warn("Unauthorized access attempt")
                .field("userId", principal.userId().value())
                .field("orderId", orderId.value())
                .log();
            
            return Result.error(Error.of("FORBIDDEN", "Access denied"));
        }
        
        return Result.ok(order);
    }
}

// Method-level security
@Service
public class AdminService {
    
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(UserId userId) {
        // Only admins can delete users
    }
    
    @PreAuthorize("@securityService.canAccessTenant(#tenantId)")
    public void updateTenantSettings(TenantId tenantId, Settings settings) {
        // Custom security check
    }
}
```

### 6. Security Misconfiguration

```yaml
# ✅ GOOD: Secure configuration
server:
  port: 8080
  error:
    include-message: never
    include-stacktrace: never
    include-exception: false
  compression:
    enabled: true
  http2:
    enabled: true

spring:
  security:
    headers:
      content-security-policy: "default-src 'self'"
      x-frame-options: DENY
      x-content-type-options: nosniff
      x-xss-protection: "1; mode=block"
      strict-transport-security: max-age=31536000; includeSubDomains
  
management:
  endpoints:
    web:
      exposure:
        include:
          - health
          - metrics
          - prometheus
        # ❌ DON'T expose: env, configprops, beans
  endpoint:
    health:
      show-details: when-authorized
```

### 7. Rate Limiting

```java
@RestController
@RequestMapping("/api")
public class ApiController {
    
    private final RateLimiter rateLimiter;
    
    @GetMapping("/data")
    public ResponseEntity<DataDto> getData(HttpServletRequest request) {
        String apiKey = request.getHeader("X-API-Key");
        
        // Rate limit by API key
        if (!rateLimiter.tryAcquire(apiKey, 100, Duration.ofMinutes(1))) {
            return ResponseEntity.status(429)
                .header("X-RateLimit-Limit", "100")
                .header("X-RateLimit-Remaining", "0")
                .header("Retry-After", "60")
                .build();
        }
        
        // ... process request
        
        return ResponseEntity.ok(data);
    }
}
```

---

## 🧪 Security Testing

### Dependency Scanning

```yaml
# .github/workflows/security.yml
name: Security Scan

on: [push, pull_request]

jobs:
  dependency-check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: OWASP Dependency Check
        uses: dependency-check/Dependency-Check_Action@main
        with:
          project: 'my-app'
          path: '.'
          format: 'HTML'
          
      - name: Snyk Security Scan
        uses: snyk/actions/maven@master
        env:
          SNYK_TOKEN: ${{ secrets.SNYK_TOKEN }}
```

### Penetration Testing

```java
@SpringBootTest
@AutoConfigureMockMvc
class SecurityTests {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void shouldRejectSqlInjection() throws Exception {
        String sqlInjection = "admin' OR '1'='1";
        
        mockMvc.perform(get("/api/users")
                .param("email", sqlInjection))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void shouldSanitizeXss() throws Exception {
        String xssPayload = "<script>alert('XSS')</script>";
        
        mockMvc.perform(post("/api/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"" + xssPayload + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").value(not(containsString("<script>"))));
    }
    
    @Test
    void shouldEnforceCsrf() throws Exception {
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isForbidden());  // Missing CSRF token
    }
}
```

---

## Best Practices

### ✅ DO

```java
// ✅ Use HTTPS everywhere
spring.security.require-ssl: true

// ✅ Rotate secrets regularly
@Scheduled(cron = "0 0 2 * * ?")

// ✅ Hash passwords with bcrypt
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

// ✅ Use short-lived JWT tokens
Duration.ofMinutes(15)  // Access token
Duration.ofDays(7)      // Refresh token

// ✅ Validate all user input
@Valid @RequestBody CreateUserRequest request
```

### ❌ DON'T

```java
// ❌ NÃO armazene secrets em código
String apiKey = "sk_live_123456789";  // ❌ NEVER!

// ❌ NÃO use MD5/SHA1 para passwords
MessageDigest.getInstance("MD5");  // ❌ WEAK!

// ❌ NÃO exponha stacktraces
throw new RuntimeException(e);  // ❌ Exposes internals!

// ❌ NÃO use GET para operações sensíveis
@GetMapping("/delete")  // ❌ Use POST/DELETE!

// ❌ NÃO ignore CORS
@CrossOrigin("*")  // ❌ Too permissive!
```

---

## Ver Também

- [Secrets Port](../api-reference/ports/secrets.md) - Secrets management
- [Multi-tenancy](../api-reference/app-multi-tenancy.md) - Tenant isolation
- [Observability](./observability.md) - Security monitoring
