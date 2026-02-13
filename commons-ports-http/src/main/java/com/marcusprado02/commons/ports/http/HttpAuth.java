package com.marcusprado02.commons.ports.http;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

public sealed interface HttpAuth permits HttpAuth.Bearer, HttpAuth.Basic {

  String asAuthorizationHeaderValue();

  record Bearer(String token) implements HttpAuth {
    public Bearer {
      Objects.requireNonNull(token, "token must not be null");
    }

    @Override
    public String asAuthorizationHeaderValue() {
      return "Bearer " + token;
    }
  }

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
