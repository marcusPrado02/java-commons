package com.marcusprado02.commons.adapters.web.jaxrs.exception;

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

class IllegalArgumentExceptionMapperTest extends JerseyTest {

  @Override
  protected Application configure() {
    return new ResourceConfig(TestResource.class).register(IllegalArgumentExceptionMapper.class);
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
  void shouldMapIllegalArgumentExceptionTo400() {
    Response response = target("/test/bad-input").request().get();

    assertEquals(400, response.getStatus());
  }

  @Path("/test")
  public static class TestResource {

    @GET
    @Path("/bad-input")
    public String throwError() {
      throw new IllegalArgumentException("invalid input");
    }
  }
}
