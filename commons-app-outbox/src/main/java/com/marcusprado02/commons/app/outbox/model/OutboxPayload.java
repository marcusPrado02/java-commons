package com.marcusprado02.commons.app.outbox.model;

public record OutboxPayload(String contentType, byte[] body) {}
