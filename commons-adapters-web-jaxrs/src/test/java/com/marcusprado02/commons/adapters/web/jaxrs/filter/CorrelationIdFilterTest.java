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

class CorrelationIdFilterTest extends JerseyTest {

  @Override
  protected Application configure() {
    return new ResourceConfig(TestResource.class).register(CorrelationIdFilter.class);
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
  void shouldGenerateCorrelationIdIfNotPresent() {
    Response response = target("/test/hello").request().get();

    assertEquals(200, response.getStatus());
    String correlationId = response.getHeaderString("X-Correlation-Id");
    assertNotNull(correlationId);
    assertFalse(correlationId.isBlank());
  }

  @Test
  void shouldPropagateIncomingCorrelationId() {
    String incomingId = "test-correlation-id-123";

    Response response = target("/test/hello").request().header("X-Correlation-Id", incomingId).get();

    assertEquals(200, response.getStatus());
    String correlationId = response.getHeaderString("X-Correlation-Id");
    assertEquals(incomingId, correlationId);
  }

  @Path("/test")
  public static class TestResource {

    @GET
    @Path("/hello")
    public String hello() {
      return "Hello, World!";
    }
  }
}
