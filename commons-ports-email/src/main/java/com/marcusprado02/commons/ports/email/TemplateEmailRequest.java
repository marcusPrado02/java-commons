package com.marcusprado02.commons.ports.email;

import java.util.*;

/**
 * Request for sending an email using a template.
 *
 * <p>Represents an email that will be rendered from a template with provided variables.
 */
public record TemplateEmailRequest(
    EmailAddress from,
    List<EmailAddress> to,
    List<EmailAddress> cc,
    List<EmailAddress> bcc,
    EmailSubject subject,
    String templateName,
    Map<String, Object> variables,
    List<EmailAttachment> attachments,
    EmailAddress replyTo) {

  public TemplateEmailRequest {
    Objects.requireNonNull(from, "from address must not be null");
    Objects.requireNonNull(to, "to addresses must not be null");
    Objects.requireNonNull(cc, "cc addresses must not be null");
    Objects.requireNonNull(bcc, "bcc addresses must not be null");
    Objects.requireNonNull(subject, "subject must not be null");
    Objects.requireNonNull(templateName, "templateName must not be null");
    Objects.requireNonNull(variables, "variables must not be null");
    Objects.requireNonNull(attachments, "attachments must not be null");

    if (to.isEmpty()) {
      throw new IllegalArgumentException("at least one recipient (to) is required");
    }
    if (templateName.trim().isEmpty()) {
      throw new IllegalArgumentException("templateName must not be blank");
    }

    // Make defensive copies
    to = List.copyOf(to);
    cc = List.copyOf(cc);
    bcc = List.copyOf(bcc);
    variables = Map.copyOf(variables);
    attachments = List.copyOf(attachments);
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Builder for TemplateEmailRequest. */
  public static final class Builder {
    private EmailAddress from;
    private final List<EmailAddress> to = new ArrayList<>();
    private final List<EmailAddress> cc = new ArrayList<>();
    private final List<EmailAddress> bcc = new ArrayList<>();
    private EmailSubject subject;
    private String templateName;
    private final Map<String, Object> variables = new HashMap<>();
    private final List<EmailAttachment> attachments = new ArrayList<>();
    private EmailAddress replyTo;

    private Builder() {}

    public Builder from(EmailAddress from) {
      this.from = from;
      return this;
    }

    public Builder from(String email) {
      this.from = EmailAddress.of(email);
      return this;
    }

    public Builder to(EmailAddress address) {
      this.to.add(address);
      return this;
    }

    public Builder to(String email) {
      this.to.add(EmailAddress.of(email));
      return this;
    }

    public Builder to(List<EmailAddress> addresses) {
      this.to.addAll(addresses);
      return this;
    }

    public Builder cc(EmailAddress address) {
      this.cc.add(address);
      return this;
    }

    public Builder cc(String email) {
      this.cc.add(EmailAddress.of(email));
      return this;
    }

    public Builder bcc(EmailAddress address) {
      this.bcc.add(address);
      return this;
    }

    public Builder bcc(String email) {
      this.bcc.add(EmailAddress.of(email));
      return this;
    }

    public Builder subject(EmailSubject subject) {
      this.subject = subject;
      return this;
    }

    public Builder subject(String subject) {
      this.subject = EmailSubject.of(subject);
      return this;
    }

    public Builder templateName(String templateName) {
      this.templateName = templateName;
      return this;
    }

    public Builder variable(String key, Object value) {
      this.variables.put(key, value);
      return this;
    }

    public Builder variables(Map<String, Object> variables) {
      this.variables.putAll(variables);
      return this;
    }

    public Builder attachment(EmailAttachment attachment) {
      this.attachments.add(attachment);
      return this;
    }

    public Builder attachments(List<EmailAttachment> attachments) {
      this.attachments.addAll(attachments);
      return this;
    }

    public Builder replyTo(EmailAddress replyTo) {
      this.replyTo = replyTo;
      return this;
    }

    public Builder replyTo(String email) {
      this.replyTo = EmailAddress.of(email);
      return this;
    }

    public TemplateEmailRequest build() {
      return new TemplateEmailRequest(
          from, to, cc, bcc, subject, templateName, variables, attachments, replyTo);
    }
  }
}
