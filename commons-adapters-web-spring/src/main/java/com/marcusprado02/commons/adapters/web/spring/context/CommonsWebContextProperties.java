package com.marcusprado02.commons.adapters.web.spring.context;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "commons.web.context")
public class CommonsWebContextProperties {

  /** Header used to receive correlation id from gateways/clients. */
  private String correlationHeader = "X-Correlation-Id";

  /** Header used to receive tenant id. */
  private String tenantHeader = "X-Tenant-Id";

  /** If true, tenant header must be present (multi-tenant hard mode). */
  private boolean tenantRequired = true;

  /** If true, generate correlation id when missing. */
  private boolean generateCorrelationWhenMissing = true;

  public String getCorrelationHeader() {
    return correlationHeader;
  }

  public void setCorrelationHeader(String correlationHeader) {
    this.correlationHeader = correlationHeader;
  }

  public String getTenantHeader() {
    return tenantHeader;
  }

  public void setTenantHeader(String tenantHeader) {
    this.tenantHeader = tenantHeader;
  }

  public boolean isTenantRequired() {
    return tenantRequired;
  }

  public void setTenantRequired(boolean tenantRequired) {
    this.tenantRequired = tenantRequired;
  }

  public boolean isGenerateCorrelationWhenMissing() {
    return generateCorrelationWhenMissing;
  }

  public void setGenerateCorrelationWhenMissing(boolean generateCorrelationWhenMissing) {
    this.generateCorrelationWhenMissing = generateCorrelationWhenMissing;
  }
}
