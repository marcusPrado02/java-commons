package com.marcusprado02.commons.adapters.web.spring.ratelimit;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple in-memory rate limiting filter using token bucket algorithm.
 *
 * <p>Limits requests per client based on a configurable key (IP address, user ID, API key, etc.).
 *
 * <p><strong>Usage:</strong>
 *
 * <pre>{@code
 * @Bean
 * public FilterRegistrationBean<RateLimitFilter> rateLimitFilter() {
 *   RateLimitFilter filter = RateLimitFilter.builder()
 *       .limit(100) // 100 requests
 *       .window(Duration.ofMinutes(1)) // per minute
 *       .keyExtractor(request -> request.getRemoteAddr()) // by IP
 *       .build();
 *
 *   FilterRegistrationBean<RateLimitFilter> bean = new FilterRegistrationBean<>();
 *   bean.setFilter(filter);
 *   bean.setUrlPatterns(List.of("/api/*"));
 *   bean.setOrder(5);
 *   return bean;
 * }
 * }</pre>
 *
 * <p><strong>Note:</strong> This is a simple in-memory implementation. For distributed systems,
 * use Redis-based rate limiting (e.g., Bucket4j with Redis).
 */
public final class RateLimitFilter implements Filter {

  private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

  private final int limit;
  private final Duration window;
  private final Function<HttpServletRequest, String> keyExtractor;
  private final Map<String, TokenBucket> buckets;

  private RateLimitFilter(
      int limit, Duration window, Function<HttpServletRequest, String> keyExtractor) {
    this.limit = limit;
    this.window = window;
    this.keyExtractor = keyExtractor;
    this.buckets = new ConcurrentHashMap<>();
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    String key = keyExtractor.apply(httpRequest);
    TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(limit, window));

    if (bucket.tryConsume()) {
      setRateLimitHeaders(httpResponse, bucket);
      chain.doFilter(request, response);
    } else {
      log.warn("Rate limit exceeded for key: {}", key);
      httpResponse.setStatus(429); // Too Many Requests
      httpResponse.setHeader("Retry-After", String.valueOf(bucket.getSecondsUntilRefill()));
      httpResponse.setHeader("X-RateLimit-Limit", String.valueOf(limit));
      httpResponse.setHeader("X-RateLimit-Remaining", "0");
      httpResponse.setContentType("application/json");
      httpResponse
          .getWriter()
          .write(
              """
          {
            "status": 429,
            "code": "RATE_LIMIT_EXCEEDED",
            "message": "Rate limit exceeded. Please try again later."
          }
          """);
    }
  }

  private void setRateLimitHeaders(HttpServletResponse response, TokenBucket bucket) {
    response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
    response.setHeader("X-RateLimit-Remaining", String.valueOf(bucket.getAvailableTokens()));
    response.setHeader("X-RateLimit-Reset", String.valueOf(bucket.getResetTime()));
  }

  public static final class Builder {
    private int limit = 60;
    private Duration window = Duration.ofMinutes(1);
    private Function<HttpServletRequest, String> keyExtractor = HttpServletRequest::getRemoteAddr;

    private Builder() {}

    public Builder limit(int limit) {
      if (limit <= 0) {
        throw new IllegalArgumentException("Limit must be positive");
      }
      this.limit = limit;
      return this;
    }

    public Builder window(Duration window) {
      if (window == null || window.isNegative() || window.isZero()) {
        throw new IllegalArgumentException("Window must be a positive duration");
      }
      this.window = window;
      return this;
    }

    public Builder keyExtractor(Function<HttpServletRequest, String> keyExtractor) {
      if (keyExtractor == null) {
        throw new IllegalArgumentException("Key extractor must not be null");
      }
      this.keyExtractor = keyExtractor;
      return this;
    }

    public RateLimitFilter build() {
      return new RateLimitFilter(limit, window, keyExtractor);
    }
  }

  private static final class TokenBucket {
    private final int capacity;
    private final long refillIntervalMillis;
    private int availableTokens;
    private long lastRefillTime;

    TokenBucket(int capacity, Duration refillInterval) {
      this.capacity = capacity;
      this.refillIntervalMillis = refillInterval.toMillis();
      this.availableTokens = capacity;
      this.lastRefillTime = System.currentTimeMillis();
    }

    synchronized boolean tryConsume() {
      refill();
      if (availableTokens > 0) {
        availableTokens--;
        return true;
      }
      return false;
    }

    synchronized int getAvailableTokens() {
      refill();
      return availableTokens;
    }

    synchronized long getResetTime() {
      return (lastRefillTime + refillIntervalMillis) / 1000;
    }

    synchronized long getSecondsUntilRefill() {
      long now = System.currentTimeMillis();
      long nextRefill = lastRefillTime + refillIntervalMillis;
      return Math.max(0, (nextRefill - now) / 1000);
    }

    private void refill() {
      long now = System.currentTimeMillis();
      long timeSinceLastRefill = now - lastRefillTime;

      if (timeSinceLastRefill >= refillIntervalMillis) {
        availableTokens = capacity;
        lastRefillTime = now;
      }
    }
  }
}
