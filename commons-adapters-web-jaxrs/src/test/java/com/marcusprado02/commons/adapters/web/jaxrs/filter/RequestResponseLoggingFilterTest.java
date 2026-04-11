package com.marcusprado02.commons.adapters.web.jaxrs.filter;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RequestResponseLoggingFilterTest extends JerseyTest {

  @Override
  protected Application configure() {
    return new ResourceConfig(TestResource.class).register(RequestResponseLoggingFilter.class);
  }

  @BeforeEach
  @Override
  public void setUp() throws Exception {
    super.setUp();
  }

  @AfterEach
  @Override
  public void tearDown() throws Exception {
    super.tearDown();
  }

  @Test
  void shouldPassRequestThroughWhenDebugDisabled() {
    Response response = target("/test/hello").request().get();

    assertEquals(200, response.getStatus());
  }

  @Test
  void shouldPassRequestWithHeadersThrough() {
    Response response =
        target("/test/hello")
            .request()
            .header("X-Correlation-Id", "test-id")
            .header("X-Tenant-Id", "tenant-1")
            .get();

    assertEquals(200, response.getStatus());
  }

  @Test
  void shouldPassRequestWithQueryParamsThrough() {
    Response response = target("/test/hello").queryParam("foo", "bar").request().get();

    assertEquals(200, response.getStatus());
  }

  @Path("/test")
  public static class TestResource {

    @GET
    @Path("/hello")
    public String hello() {
      return "Hello";
    }
  }
}
