package com.marcusprado02.commons.adapters.web.spring.context;

import com.marcusprado02.commons.kernel.ddd.audit.ActorId;
import com.marcusprado02.commons.kernel.ddd.tenant.TenantId;
import com.marcusprado02.commons.platform.context.RequestContextSnapshot;
import com.marcusprado02.commons.platform.id.CorrelationIdGenerator;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;

public final class SpringRequestContextResolver {

  private final CommonsWebContextProperties props;
  private final SpringActorResolver actorResolver;

  public SpringRequestContextResolver(
      CommonsWebContextProperties props, SpringActorResolver actorResolver) {
    this.props = Objects.requireNonNull(props, "props");
    this.actorResolver = Objects.requireNonNull(actorResolver, "actorResolver");
  }

  public RequestContextSnapshot resolve(HttpServletRequest request) {
    Objects.requireNonNull(request, "request");

    String correlationId = headerOrNull(request, props.getCorrelationHeader());
    if ((correlationId == null || correlationId.isBlank())
        && props.isGenerateCorrelationWhenMissing()) {
      correlationId = CorrelationIdGenerator.newCorrelationId();
    }

    String tenantRaw = headerOrNull(request, props.getTenantHeader());
    if (props.isTenantRequired() && (tenantRaw == null || tenantRaw.isBlank())) {
      throw new IllegalArgumentException(
          "Missing required tenant header: " + props.getTenantHeader());
    }

    TenantId tenantId = (tenantRaw == null || tenantRaw.isBlank()) ? null : TenantId.of(tenantRaw);

    ActorId actorId = actorResolver.resolve(request);

    return new RequestContextSnapshot(correlationId, null, tenantId, actorId);
  }

  private static String headerOrNull(HttpServletRequest req, String name) {
    if (name == null || name.isBlank()) return null;
    String v = req.getHeader(name);
    return (v == null || v.isBlank()) ? null : v;
  }
}
