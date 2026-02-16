package com.marcusprado02.commons.ports.pdf;

import java.awt.Color;
import java.io.InputStream;
import java.util.List;

/**
 * Represents elements that can be added to a PDF document.
 *
 * <p>PDF elements include text, paragraphs, images, tables, and other content. This sealed
 * interface ensures type safety and exhaustive pattern matching.
 *
 * @since 0.1.0
 */
public sealed interface PdfElement
    permits PdfElement.Text,
        PdfElement.Paragraph,
        PdfElement.Image,
        PdfElement.Table,
        PdfElement.Space,
        PdfElement.PageBreak {

  /**
   * Text element with font styling.
   *
   * @param content text content
   * @param fontSize font size in points
   * @param bold whether text is bold
   * @param italic whether text is italic
   * @param color text color (null for default black)
   */
  record Text(String content, float fontSize, boolean bold, boolean italic, Color color)
      implements PdfElement {

    public Text(String content) {
      this(content, 12f, false, false, null);
    }

    public Text(String content, float fontSize) {
      this(content, fontSize, false, false, null);
    }
  }

  /**
   * Paragraph element with alignment and spacing.
   *
   * @param content paragraph text
   * @param fontSize font size in points
   * @param alignment text alignment
   * @param leading line spacing multiplier
   */
  record Paragraph(String content, float fontSize, Alignment alignment, float leading)
      implements PdfElement {

    public Paragraph(String content) {
      this(content, 12f, Alignment.LEFT, 1.5f);
    }

    public Paragraph(String content, Alignment alignment) {
      this(content, 12f, alignment, 1.5f);
    }
  }

  /**
   * Image element from input stream or file.
   *
   * @param imageData image data as InputStream
   * @param width image width in points (null for original)
   * @param height image height in points (null for original)
   * @param alignment image alignment on page
   */
  record Image(InputStream imageData, Float width, Float height, Alignment alignment)
      implements PdfElement {

    public Image(InputStream imageData) {
      this(imageData, null, null, Alignment.LEFT);
    }

    public Image(InputStream imageData, float width, float height) {
      this(imageData, width, height, Alignment.CENTER);
    }
  }

  /**
   * Table element with rows and columns.
   *
   * @param headers column headers (null for no headers)
   * @param rows table data rows
   * @param columnWidths relative column widths (null for equal)
   * @param headerColor header background color
   */
  record Table(
      List<String> headers, List<List<String>> rows, float[] columnWidths, Color headerColor)
      implements PdfElement {

    public Table(List<List<String>> rows) {
      this(null, rows, null, new Color(220, 220, 220));
    }

    public Table(List<String> headers, List<List<String>> rows) {
      this(headers, rows, null, new Color(220, 220, 220));
    }
  }

  /**
   * Vertical space element.
   *
   * @param height space height in points
   */
  record Space(float height) implements PdfElement {
    public Space() {
      this(12f);
    }
  }

  /** Forces a page break. */
  record PageBreak() implements PdfElement {}

  /** Text alignment options. */
  enum Alignment {
    LEFT,
    CENTER,
    RIGHT,
    JUSTIFIED
  }
}
