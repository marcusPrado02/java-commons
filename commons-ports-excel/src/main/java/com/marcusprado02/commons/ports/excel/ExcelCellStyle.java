package com.marcusprado02.commons.ports.excel;

import java.awt.Color;

/**
 * Formatting style for Excel cells.
 *
 * <p>Defines font properties, colors, alignment, borders, and number formatting for cells.
 *
 * @param fontName font family name
 * @param fontSize font size in points
 * @param bold whether text is bold
 * @param italic whether text is italic
 * @param underline whether text is underlined
 * @param fontColor text color
 * @param backgroundColor cell background color
 * @param horizontalAlignment horizontal text alignment
 * @param verticalAlignment vertical text alignment
 * @param wrapText whether to wrap text in the cell
 * @param numberFormat number/date format pattern
 * @param borderStyle border style for all sides
 * @param borderColor border color
 * @since 0.1.0
 */
public record ExcelCellStyle(
    String fontName,
    Integer fontSize,
    Boolean bold,
    Boolean italic,
    Boolean underline,
    Color fontColor,
    Color backgroundColor,
    HorizontalAlignment horizontalAlignment,
    VerticalAlignment verticalAlignment,
    Boolean wrapText,
    String numberFormat,
    BorderStyle borderStyle,
    Color borderColor) {

  /** Horizontal alignment options. */
  public enum HorizontalAlignment {
    LEFT,
    CENTER,
    RIGHT,
    JUSTIFY,
    FILL
  }

  /** Vertical alignment options. */
  public enum VerticalAlignment {
    TOP,
    CENTER,
    BOTTOM,
    JUSTIFY
  }

  /** Border style options. */
  public enum BorderStyle {
    NONE,
    THIN,
    MEDIUM,
    THICK,
    DOTTED,
    DASHED,
    DOUBLE
  }

  /** Creates a default style. */
  public static ExcelCellStyle defaultStyle() {
    return new Builder().build();
  }

  /** Creates a style for headers (bold, centered). */
  public static ExcelCellStyle headerStyle() {
    return new Builder()
        .bold(true)
        .horizontalAlignment(HorizontalAlignment.CENTER)
        .backgroundColor(new Color(217, 217, 217))
        .borderStyle(BorderStyle.THIN)
        .build();
  }

  /** Creates a style for currency formatting. */
  public static ExcelCellStyle currencyStyle() {
    return new Builder()
        .numberFormat("$#,##0.00")
        .horizontalAlignment(HorizontalAlignment.RIGHT)
        .build();
  }

  /** Creates a style for percentage formatting. */
  public static ExcelCellStyle percentageStyle() {
    return new Builder()
        .numberFormat("0.00%")
        .horizontalAlignment(HorizontalAlignment.RIGHT)
        .build();
  }

  /** Creates a style for date formatting. */
  public static ExcelCellStyle dateStyle() {
    return new Builder().numberFormat("yyyy-mm-dd").build();
  }

  /** Creates a style for datetime formatting. */
  public static ExcelCellStyle dateTimeStyle() {
    return new Builder().numberFormat("yyyy-mm-dd hh:mm:ss").build();
  }

  /** Creates a new builder for ExcelCellStyle. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for ExcelCellStyle. */
  public static class Builder {
    private String fontName = "Calibri";
    private Integer fontSize = 11;
    private Boolean bold = false;
    private Boolean italic = false;
    private Boolean underline = false;
    private Color fontColor = Color.BLACK;
    private Color backgroundColor;
    private HorizontalAlignment horizontalAlignment = HorizontalAlignment.LEFT;
    private VerticalAlignment verticalAlignment = VerticalAlignment.CENTER;
    private Boolean wrapText = false;
    private String numberFormat;
    private BorderStyle borderStyle = BorderStyle.NONE;
    private Color borderColor = Color.BLACK;

    public Builder fontName(String fontName) {
      this.fontName = fontName;
      return this;
    }

    public Builder fontSize(Integer fontSize) {
      this.fontSize = fontSize;
      return this;
    }

    public Builder bold(Boolean bold) {
      this.bold = bold;
      return this;
    }

    public Builder italic(Boolean italic) {
      this.italic = italic;
      return this;
    }

    public Builder underline(Boolean underline) {
      this.underline = underline;
      return this;
    }

    public Builder fontColor(Color fontColor) {
      this.fontColor = fontColor;
      return this;
    }

    public Builder backgroundColor(Color backgroundColor) {
      this.backgroundColor = backgroundColor;
      return this;
    }

    public Builder horizontalAlignment(HorizontalAlignment horizontalAlignment) {
      this.horizontalAlignment = horizontalAlignment;
      return this;
    }

    public Builder verticalAlignment(VerticalAlignment verticalAlignment) {
      this.verticalAlignment = verticalAlignment;
      return this;
    }

    public Builder wrapText(Boolean wrapText) {
      this.wrapText = wrapText;
      return this;
    }

    public Builder numberFormat(String numberFormat) {
      this.numberFormat = numberFormat;
      return this;
    }

    public Builder borderStyle(BorderStyle borderStyle) {
      this.borderStyle = borderStyle;
      return this;
    }

    public Builder borderColor(Color borderColor) {
      this.borderColor = borderColor;
      return this;
    }

    public ExcelCellStyle build() {
      return new ExcelCellStyle(
          fontName,
          fontSize,
          bold,
          italic,
          underline,
          fontColor,
          backgroundColor,
          horizontalAlignment,
          verticalAlignment,
          wrapText,
          numberFormat,
          borderStyle,
          borderColor);
    }
  }
}
