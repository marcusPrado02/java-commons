package com.marcusprado02.commons.adapters.web.jaxrs.exception;

import com.marcusprado02.commons.adapters.web.problem.HttpProblemResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * JAX-RS exception mapper for {@link IllegalArgumentException}.
 *
 * <p>Maps illegal argument exceptions to 400 Bad Request responses.
 */
@Provider
public class IllegalArgumentExceptionMapper implements ExceptionMapper<IllegalArgumentException> {

  @Override
  public Response toResponse(IllegalArgumentException exception) {
    HttpProblemResponse problem =
        HttpProblemResponse.of(
            Response.Status.BAD_REQUEST.getStatusCode(),
            "INVALID_ARGUMENT",
            exception.getMessage());

    return Response.status(Response.Status.BAD_REQUEST)
        .type(MediaType.APPLICATION_JSON)
        .entity(problem)
        .build();
  }
}
