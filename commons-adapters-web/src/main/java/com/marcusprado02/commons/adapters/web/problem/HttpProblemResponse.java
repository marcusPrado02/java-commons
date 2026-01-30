package com.marcusprado02.commons.adapters.web.problem;

import java.util.Map;

public record HttpProblemResponse(
    int status,
    String code,
    String message,
    Map<String, Object> details
) {}
