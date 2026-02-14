package com.marcusprado02.commons.ports.email;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Email message envelope.
 *
 * <p>Represents a complete email message with all its components.
 */
public record Email(
    EmailAddress from,
    List<EmailAddress> to,
    List<EmailAddress> cc,
    List<EmailAddress> bcc,
    EmailSubject subject,
    EmailContent content,
    List<EmailAttachment> attachments,
    EmailAddress replyTo) {

  public Email {
    Objects.requireNonNull(from, "from address must not be null");
    Objects.requireNonNull(to, "to addresses must not be null");
    Objects.requireNonNull(cc, "cc addresses must not be null");
    Objects.requireNonNull(bcc, "bcc addresses must not be null");
    Objects.requireNonNull(subject, "subject must not be null");
    Objects.requireNonNull(content, "content must not be null");
    Objects.requireNonNull(attachments, "attachments must not be null");

    if (to.isEmpty()) {
      throw new IllegalArgumentException("at least one recipient (to) is required");
    }

    // Make defensive copies to ensure immutability
    to = List.copyOf(to);
    cc = List.copyOf(cc);
    bcc = List.copyOf(bcc);
    attachments = List.copyOf(attachments);
  }

  /**
   * Creates a new builder for constructing Email instances.
   *
   * @return a new Builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Returns true if the email has attachments.
   *
   * @return true if attachments exist
   */
  public boolean hasAttachments() {
    return !attachments.isEmpty();
  }

  /**
   * Returns the total size of all attachments in bytes.
   *
   * @return total attachment size
   */
  public long totalAttachmentSize() {
    return attachments.stream().mapToLong(EmailAttachment::size).sum();
  }

  /** Builder for Email. */
  public static final class Builder {
    private EmailAddress from;
    private final List<EmailAddress> to = new ArrayList<>();
    private final List<EmailAddress> cc = new ArrayList<>();
    private final List<EmailAddress> bcc = new ArrayList<>();
    private EmailSubject subject;
    private EmailContent content;
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

    public Builder cc(List<EmailAddress> addresses) {
      this.cc.addAll(addresses);
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

    public Builder bcc(List<EmailAddress> addresses) {
      this.bcc.addAll(addresses);
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

    public Builder content(EmailContent content) {
      this.content = content;
      return this;
    }

    public Builder htmlContent(String html) {
      this.content = EmailContent.html(html);
      return this;
    }

    public Builder textContent(String text) {
      this.content = EmailContent.text(text);
      return this;
    }

    public Builder bothContent(String html, String text) {
      this.content = EmailContent.both(html, text);
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

    public Email build() {
      return new Email(from, to, cc, bcc, subject, content, attachments, replyTo);
    }
  }
}
