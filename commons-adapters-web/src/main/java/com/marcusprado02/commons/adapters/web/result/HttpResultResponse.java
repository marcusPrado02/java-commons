package com.marcusprado02.commons.adapters.web.result;

import com.marcusprado02.commons.adapters.web.envelope.ApiEnvelope;

/** HttpResultResponse data. */
public record HttpResultResponse(int status, ApiEnvelope body) {}
