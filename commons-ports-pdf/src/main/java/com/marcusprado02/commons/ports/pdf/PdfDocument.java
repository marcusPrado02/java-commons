package com.marcusprado02.commons.ports.pdf;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents a PDF document with metadata and content elements.
 *
 * <p>Immutable value object built using the Builder pattern. Contains document metadata (title,
 * author, etc.) and content elements.
 *
 * @param title document title
 * @param author document author
 * @param subject document subject
 * @param keywords document keywords
 * @param creator application that created the document
 * @param pageSize page size for all pages
 * @param margins page margins (top, right, bottom, left) in points
 * @param elements content elements to be rendered
 * @param properties custom properties for the document
 * @param createdAt document creation timestamp
 * @since 0.1.0
 */
public record PdfDocument(
    String title,
    String author,
    String subject,
    String keywords,
    String creator,
    PageSize pageSize,
    Margins margins,
    List<PdfElement> elements,
    Map<String, String> properties,
    Instant createdAt) {

  public PdfDocument {
    elements = Collections.unmodifiableList(new ArrayList<>(elements));
    properties = properties == null ? Map.of() : Map.copyOf(properties);
    createdAt = createdAt == null ? Instant.now() : createdAt;
  }

  /**
   * Page margins in points.
   *
   * @param top top margin
   * @param right right margin
   * @param bottom bottom margin
   * @param left left margin
   */
  public record Margins(float top, float right, float bottom, float left) {
    public static Margins uniform(float margin) {
      return new Margins(margin, margin, margin, margin);
    }

    public static Margins standard() {
      return uniform(72f); // 1 inch
    }

    public static Margins narrow() {
      return uniform(36f); // 0.5 inch
    }
  }

  /** Creates a new builder for PdfDocument. */
  public static Builder builder() {
    return new Builder();
  }

  /** Creates a builder initialized with this document's values. */
  public Builder toBuilder() {
    return new Builder()
        .title(title)
        .author(author)
        .subject(subject)
        .keywords(keywords)
        .creator(creator)
        .pageSize(pageSize)
        .margins(margins)
        .elements(new ArrayList<>(elements))
        .properties(properties)
        .createdAt(createdAt);
  }

  /** Builder for PdfDocument. */
  public static class Builder {
    private String title;
    private String author;
    private String subject;
    private String keywords;
    private String creator = "Commons PDF Port";
    private PageSize pageSize = PageSize.A4;
    private Margins margins = Margins.standard();
    private List<PdfElement> elements = new ArrayList<>();
    private Map<String, String> properties = Map.of();
    private Instant createdAt;

    public Builder title(String title) {
      this.title = title;
      return this;
    }

    public Builder author(String author) {
      this.author = author;
      return this;
    }

    public Builder subject(String subject) {
      this.subject = subject;
      return this;
    }

    public Builder keywords(String keywords) {
      this.keywords = keywords;
      return this;
    }

    public Builder creator(String creator) {
      this.creator = creator;
      return this;
    }

    public Builder pageSize(PageSize pageSize) {
      this.pageSize = pageSize;
      return this;
    }

    public Builder margins(Margins margins) {
      this.margins = margins;
      return this;
    }

    public Builder margins(float uniform) {
      this.margins = Margins.uniform(uniform);
      return this;
    }

    public Builder element(PdfElement element) {
      this.elements.add(element);
      return this;
    }

    public Builder elements(List<PdfElement> elements) {
      this.elements = new ArrayList<>(elements);
      return this;
    }

    public Builder addElements(PdfElement... elements) {
      Collections.addAll(this.elements, elements);
      return this;
    }

    public Builder properties(Map<String, String> properties) {
      this.properties = properties;
      return this;
    }

    public Builder property(String key, String value) {
      if (this.properties.isEmpty()) {
        this.properties = new java.util.HashMap<>();
      } else if (!(this.properties instanceof java.util.HashMap)) {
        this.properties = new java.util.HashMap<>(this.properties);
      }
      this.properties.put(key, value);
      return this;
    }

    public Builder createdAt(Instant createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public PdfDocument build() {
      return new PdfDocument(
          title,
          author,
          subject,
          keywords,
          creator,
          pageSize,
          margins,
          elements,
          properties,
          createdAt);
    }
  }
}
