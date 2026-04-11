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

class GenericExceptionMapperTest extends JerseyTest {

  @Override
  protected Application configure() {
    return new ResourceConfig(TestResource.class).register(GenericExceptionMapper.class);
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
  void shouldMapExceptionTo500() {
    Response response = target("/test/error").request().get();

    assertEquals(500, response.getStatus());
  }

  @Path("/test")
  public static class TestResource {

    @GET
    @Path("/error")
    public String throwError() {
      throw new RuntimeException("unexpected error");
    }
  }
}
