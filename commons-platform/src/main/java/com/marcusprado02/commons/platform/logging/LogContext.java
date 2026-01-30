package com.marcusprado02.commons.platform.logging;

import java.util.Map;

/** Key-value context for structured logging. Adapters map this to MDC, OpenTelemetry, etc. */
public interface LogContext {

  Map<String, String> entries();
}
