package com.marcusprado02.commons.platform.context;

/** Aggregates all contextual information of a request. */
public interface RequestContext extends CorrelationContext, TenantContext, SecurityContext {}
