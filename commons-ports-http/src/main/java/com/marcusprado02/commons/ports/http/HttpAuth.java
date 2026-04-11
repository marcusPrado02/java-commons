package com.marcusprado02.commons.ports.http;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

/** HTTP authentication strategy that produces an {@code Authorization} header value. */
public sealed interface HttpAuth permits HttpAuth.Bearer, HttpAuth.Basic {

  /**
   * Returns the value for the {@code Authorization} HTTP header.
   *
   * @return the authorization header value
   */
  String asAuthorizationHeaderValue();

  /** Bearer token authentication. */
  record Bearer(String token) implements HttpAuth {
    public Bearer {
      Objects.requireNonNull(token, "token must not be null");
    }

    @Override
    public String asAuthorizationHeaderValue() {
      return "Bearer " + token;
    }
  }

  /** HTTP Basic authentication using a username and password. */
  record Basic(String username, String password) implements HttpAuth {
    public Basic {
      Objects.requireNonNull(username, "username must not be null");
      Objects.requireNonNull(password, "password must not be null");
    }

    @Override
    public String asAuthorizationHeaderValue() {
      String raw = username + ":" + password;
      String encoded = Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
      return "Basic " + encoded;
    }
  }
}
