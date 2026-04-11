package com.marcusprado02.commons.app.outbox.model;

/** OutboxPayload data. */
public record OutboxPayload(String contentType, byte[] body) {}
