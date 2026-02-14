package com.marcusprado02.commons.adapters.web.jaxrs.exception;

import com.marcusprado02.commons.adapters.web.problem.HttpProblemMapper;
import com.marcusprado02.commons.adapters.web.problem.HttpProblemResponse;
import com.marcusprado02.commons.kernel.errors.DomainException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * JAX-RS exception mapper for {@link DomainException}.
 *
 * <p>Maps domain exceptions to RFC 7807 Problem Detail responses.
 *
 * <p><strong>Usage:</strong>
 *
 * <pre>{@code
 * @ApplicationPath("/api")
 * public class RestApplication extends Application {
 *
 *   @Override
 *   public Set<Class<?>> getClasses() {
 *     return Set.of(
 *       DomainExceptionMapper.class,
 *       // other providers
 *     );
 *   }
 *
 *   @Override
 *   public Set<Object> getSingletons() {
 *     HttpProblemMapper mapper = new DefaultHttpProblemMapper();
 *     return Set.of(new DomainExceptionMapper(mapper));
 *   }
 * }
 * }</pre>
 */
@Provider
public class DomainExceptionMapper implements ExceptionMapper<DomainException> {

  private final HttpProblemMapper mapper;

  public DomainExceptionMapper(HttpProblemMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public Response toResponse(DomainException exception) {
    HttpProblemResponse problem = mapper.map(exception.problem());

    return Response.status(problem.status())
        .type(MediaType.APPLICATION_JSON)
        .entity(problem)
        .build();
  }
}
