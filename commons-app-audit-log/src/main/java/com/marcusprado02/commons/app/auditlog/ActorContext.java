package com.marcusprado02.commons.app.auditlog;

/**
 * Additional context about the actor.
 *
 * <p>Contains information like IP address and user agent.
 */
public final class ActorContext {

  private final String ipAddress;
  private final String userAgent;

  public ActorContext(String ipAddress, String userAgent) {
    this.ipAddress = ipAddress;
    this.userAgent = userAgent;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public String getUserAgent() {
    return userAgent;
  }
}
