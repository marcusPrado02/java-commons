package com.marcusprado02.commons.adapters.web.jaxrs.exception;

import com.marcusprado02.commons.adapters.web.problem.HttpProblemResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS exception mapper for unexpected exceptions.
 *
 * <p>Catches all unhandled exceptions and maps them to 500 Internal Server Error responses.
 *
 * <p><strong>Note:</strong> Register this mapper last (highest priority) to ensure it only catches
 * exceptions not handled by more specific mappers.
 */
@Provider
public class GenericExceptionMapper implements ExceptionMapper<Exception> {

  private static final Logger log = LoggerFactory.getLogger(GenericExceptionMapper.class);

  @Override
  public Response toResponse(Exception exception) {
    log.error("Unhandled exception", exception);

    HttpProblemResponse problem =
        new HttpProblemResponse(
            Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
            "INTERNAL_ERROR",
            "An unexpected error occurred",
            Map.of(),
            null);

    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
        .type(MediaType.APPLICATION_JSON)
        .entity(problem)
        .build();
  }
}
