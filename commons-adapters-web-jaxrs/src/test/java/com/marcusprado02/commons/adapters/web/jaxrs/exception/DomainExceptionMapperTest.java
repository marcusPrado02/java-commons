package com.marcusprado02.commons.adapters.web.jaxrs.exception;

import static org.junit.jupiter.api.Assertions.*;

import com.marcusprado02.commons.adapters.web.problem.HttpProblemMapper;
import com.marcusprado02.commons.adapters.web.problem.HttpProblemResponse;
import com.marcusprado02.commons.kernel.errors.*;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DomainExceptionMapperTest extends JerseyTest {

  @Override
  protected Application configure() {
    return new ResourceConfig(TestResource.class)
        .register(new DomainExceptionMapper(new TestProblemMapper()));
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
  void shouldMapDomainExceptionToProblemResponse() {
    Response response = target("/test/domain-error").request().get();

    assertEquals(400, response.getStatus());
    HttpProblemResponse problem = response.readEntity(HttpProblemResponse.class);
    assertNotNull(problem);
    assertEquals("TEST_ERROR", problem.code());
    assertEquals("Test error message", problem.message());
  }

  @Path("/test")
  public static class TestResource {

    @GET
    @Path("/domain-error")
    public String throwDomainException() {
      throw new DomainException(
          Problem.of(
              ErrorCode.of("TEST_ERROR"),
              ErrorCategory.VALIDATION,
              Severity.ERROR,
              "Test error message"));
    }
  }

  private static class TestProblemMapper implements HttpProblemMapper {
    @Override
    public HttpProblemResponse map(Problem problem) {
      return HttpProblemResponse.of(400, problem.code().value(), problem.message());
    }
  }
}
