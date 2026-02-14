package com.marcusprado02.commons.ports.sms;

import java.util.List;
import java.util.Objects;

/**
 * MMS (Multimedia Messaging Service) message envelope.
 *
 * <p>Extends SMS functionality to support multimedia content like images, videos, and audio files.
 */
public record MMS(
    PhoneNumber from,
    PhoneNumber to,
    String message,
    List<MediaContent> mediaContents,
    SMSOptions options) {

  public MMS {
    Objects.requireNonNull(from, "from phone number must not be null");
    Objects.requireNonNull(to, "to phone number must not be null");
    Objects.requireNonNull(mediaContents, "media contents must not be null");
    Objects.requireNonNull(options, "options must not be null");

    if (mediaContents.isEmpty() && (message == null || message.isBlank())) {
      throw new IllegalArgumentException("MMS must have either text message or media content");
    }

    // Make defensive copy
    mediaContents = List.copyOf(mediaContents);

    // Validate total size (typical MMS limit is 300KB to 1MB)
    long totalSize = mediaContents.stream()
        .mapToLong(media -> media.content().length)
        .sum();

    if (totalSize > 1048576) { // 1MB limit
      throw new IllegalArgumentException("total media size exceeds 1MB limit: " + totalSize + " bytes");
    }
  }

  /**
   * Creates a new builder for constructing MMS instances.
   *
   * @return a new Builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Media content for MMS. */
  public record MediaContent(byte[] content, String contentType, String filename) {
    public MediaContent {
      Objects.requireNonNull(content, "content must not be null");
      Objects.requireNonNull(contentType, "content type must not be null");

      if (content.length == 0) {
        throw new IllegalArgumentException("content cannot be empty");
      }

      if (contentType.isBlank()) {
        throw new IllegalArgumentException("content type cannot be blank");
      }

      // Validate common MMS content types
      if (!isValidMmsContentType(contentType)) {
        throw new IllegalArgumentException("unsupported content type for MMS: " + contentType);
      }
    }

    private static boolean isValidMmsContentType(String contentType) {
      return contentType.startsWith("image/") ||
             contentType.startsWith("video/") ||
             contentType.startsWith("audio/") ||
             "text/plain".equals(contentType) ||
             "application/pdf".equals(contentType);
    }

    public static MediaContent image(byte[] imageData, String imageType) {
      return new MediaContent(imageData, "image/" + imageType, null);
    }

    public static MediaContent video(byte[] videoData, String videoType) {
      return new MediaContent(videoData, "video/" + videoType, null);
    }

    public static MediaContent audio(byte[] audioData, String audioType) {
      return new MediaContent(audioData, "audio/" + audioType, null);
    }
  }

  /** Builder for MMS instances. */
  public static final class Builder {
    private PhoneNumber from;
    private PhoneNumber to;
    private String message;
    private java.util.List<MediaContent> mediaContents = new java.util.ArrayList<>();
    private SMSOptions options = SMSOptions.defaults();

    private Builder() {}

    public Builder from(String from) {
      this.from = PhoneNumber.of(from);
      return this;
    }

    public Builder from(PhoneNumber from) {
      this.from = from;
      return this;
    }

    public Builder to(String to) {
      this.to = PhoneNumber.of(to);
      return this;
    }

    public Builder to(PhoneNumber to) {
      this.to = to;
      return this;
    }

    public Builder message(String message) {
      this.message = message;
      return this;
    }

    public Builder addMedia(MediaContent mediaContent) {
      this.mediaContents.add(mediaContent);
      return this;
    }

    public Builder addImage(byte[] imageData, String imageType) {
      return addMedia(MediaContent.image(imageData, imageType));
    }

    public Builder addVideo(byte[] videoData, String videoType) {
      return addMedia(MediaContent.video(videoData, videoType));
    }

    public Builder addAudio(byte[] audioData, String audioType) {
      return addMedia(MediaContent.audio(audioData, audioType));
    }

    public Builder options(SMSOptions options) {
      this.options = options;
      return this;
    }

    public MMS build() {
      return new MMS(from, to, message, mediaContents, options);
    }
  }
}
