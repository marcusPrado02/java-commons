package com.marcusprado02.commons.adapters.web.result;

import com.marcusprado02.commons.adapters.web.envelope.ApiEnvelopeWithContext;

public record HttpResultResponseWithContext(int status, ApiEnvelopeWithContext body) {}
