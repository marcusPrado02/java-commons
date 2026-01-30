package com.marcusprado02.commons.adapters.web.spring.context;

import com.marcusprado02.commons.kernel.ddd.audit.ActorId;
import jakarta.servlet.http.HttpServletRequest;

/**
 * SPI for resolving the current actor in Spring Web.
 *
 * <p>Default implementation returns system actor. Spring Security adapters may override this.
 */
public interface SpringActorResolver {

  ActorId resolve(HttpServletRequest request);
}
