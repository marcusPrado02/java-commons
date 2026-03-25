package com.marcusprado02.commons.adapters.http.okhttp;

import com.marcusprado02.commons.ports.http.HttpClientPort;
import com.marcusprado02.commons.ports.http.HttpClientPortContractTest;

/**
 * Verifies that {@link OkHttpClientAdapter} honours the full {@link HttpClientPort} contract
 * defined by {@link HttpClientPortContractTest}.
 *
 * <p>Every scenario in the abstract base class is automatically inherited and executed here, giving
 * confidence that the OkHttp adapter is a correct implementation of the port without duplicating
 * test logic.
 */
class OkHttpClientAdapterContractTest extends HttpClientPortContractTest {

  @Override
  protected HttpClientPort createAdapter() {
    return OkHttpClientAdapter.builder().build();
  }
}
