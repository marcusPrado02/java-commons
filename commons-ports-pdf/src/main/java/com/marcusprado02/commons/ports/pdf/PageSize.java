package com.marcusprado02.commons.ports.pdf;

/**
 * Standard page sizes for PDF documents.
 *
 * <p>Provides common page dimensions following ISO 216, ANSI, and other standards. Each size
 * includes width and height in points (1 point = 1/72 inch).
 *
 * @since 0.1.0
 */
public final class PageSize {
  /** A4 size: 210mm × 297mm (595 × 842 points). Most common size internationally. */
  public static final PageSize A4 = new PageSize(595, 842);

  /** A3 size: 297mm × 420mm (842 × 1191 points). Double the size of A4. */
  public static final PageSize A3 = new PageSize(842, 1191);

  /** A5 size: 148mm × 210mm (420 × 595 points). Half the size of A4. */
  public static final PageSize A5 = new PageSize(420, 595);

  /** Letter size: 8.5in × 11in (612 × 792 points). Common in North America. */
  public static final PageSize LETTER = new PageSize(612, 792);

  /** Legal size: 8.5in × 14in (612 × 1008 points). Common for legal documents in North America. */
  public static final PageSize LEGAL = new PageSize(612, 1008);

  /** Tabloid size: 11in × 17in (792 × 1224 points). Also known as Ledger. */
  public static final PageSize TABLOID = new PageSize(792, 1224);

  /** Executive size: 7.25in × 10.5in (522 × 756 points). */
  public static final PageSize EXECUTIVE = new PageSize(522, 756);

  private final float width;
  private final float height;

  private PageSize(float width, float height) {
    this.width = width;
    this.height = height;
  }

  /**
   * Gets the page width in points.
   *
   * @return width in points
   */
  public float getWidth() {
    return width;
  }

  /**
   * Gets the page height in points.
   *
   * @return height in points
   */
  public float getHeight() {
    return height;
  }

  /**
   * Creates a rotated version of this page size (landscape orientation).
   *
   * @return new PageSize with swapped dimensions
   */
  public PageSize rotate() {
    return new PageSize(height, width);
  }

  /**
   * Creates a custom page size with specified dimensions.
   *
   * @param width page width in points
   * @param height page height in points
   * @return custom PageSize
   */
  public static PageSize custom(float width, float height) {
    return new PageSize(width, height);
  }

  @Override
  public String toString() {
    return String.format("PageSize[%.0fx%.0f]", width, height);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof PageSize other)) return false;
    return Float.compare(width, other.width) == 0 && Float.compare(height, other.height) == 0;
  }

  @Override
  public int hashCode() {
    return Float.hashCode(width) * 31 + Float.hashCode(height);
  }
}
