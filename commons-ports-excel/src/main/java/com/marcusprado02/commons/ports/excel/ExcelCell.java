package com.marcusprado02.commons.ports.excel;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a cell in an Excel worksheet.
 *
 * <p>Immutable value object that can hold different types of data including text, numbers, dates,
 * formulas, and formatting information.
 *
 * @param row row index (0-based)
 * @param column column index (0-based)
 * @param cellType type of data in the cell
 * @param value cell value (type depends on cellType)
 * @param formula formula text (null if not a formula cell)
 * @param style cell formatting style
 * @since 0.1.0
 */
public record ExcelCell(
    int row, int column, CellType cellType, Object value, String formula, ExcelCellStyle style) {

  public ExcelCell {
    if (row < 0) throw new IllegalArgumentException("Row must be >= 0");
    if (column < 0) throw new IllegalArgumentException("Column must be >= 0");
    if (cellType == null) throw new IllegalArgumentException("CellType cannot be null");
  }

  /**
   * Creates a blank cell.
   *
   * @param row row index
   * @param column column index
   */
  public static ExcelCell blank(int row, int column) {
    return new ExcelCell(row, column, CellType.BLANK, null, null, null);
  }

  /**
   * Creates a string cell.
   *
   * @param row row index
   * @param column column index
   * @param text cell text
   */
  public static ExcelCell text(int row, int column, String text) {
    return new ExcelCell(row, column, CellType.STRING, text, null, null);
  }

  /**
   * Creates a string cell with style.
   *
   * @param row row index
   * @param column column index
   * @param text cell text
   * @param style cell style
   */
  public static ExcelCell text(int row, int column, String text, ExcelCellStyle style) {
    return new ExcelCell(row, column, CellType.STRING, text, null, style);
  }

  /**
   * Creates a numeric cell.
   *
   * @param row row index
   * @param column column index
   * @param number cell number
   */
  public static ExcelCell number(int row, int column, Number number) {
    return new ExcelCell(row, column, CellType.NUMERIC, number, null, null);
  }

  /**
   * Creates a numeric cell with style.
   *
   * @param row row index
   * @param column column index
   * @param number cell number
   * @param style cell style
   */
  public static ExcelCell number(int row, int column, Number number, ExcelCellStyle style) {
    return new ExcelCell(row, column, CellType.NUMERIC, number, null, style);
  }

  /**
   * Creates a boolean cell.
   *
   * @param row row index
   * @param column column index
   * @param bool cell boolean value
   */
  public static ExcelCell bool(int row, int column, Boolean bool) {
    return new ExcelCell(row, column, CellType.BOOLEAN, bool, null, null);
  }

  /**
   * Creates a date cell.
   *
   * @param row row index
   * @param column column index
   * @param date cell date
   */
  public static ExcelCell date(int row, int column, LocalDate date) {
    return new ExcelCell(row, column, CellType.NUMERIC, date, null, null);
  }

  /**
   * Creates a date cell with style.
   *
   * @param row row index
   * @param column column index
   * @param date cell date
   * @param style cell style
   */
  public static ExcelCell date(int row, int column, LocalDate date, ExcelCellStyle style) {
    return new ExcelCell(row, column, CellType.NUMERIC, date, null, style);
  }

  /**
   * Creates a datetime cell.
   *
   * @param row row index
   * @param column column index
   * @param dateTime cell datetime
   */
  public static ExcelCell dateTime(int row, int column, LocalDateTime dateTime) {
    return new ExcelCell(row, column, CellType.NUMERIC, dateTime, null, null);
  }

  /**
   * Creates a datetime cell with style.
   *
   * @param row row index
   * @param column column index
   * @param dateTime cell datetime
   * @param style cell style
   */
  public static ExcelCell dateTime(int row, int column, LocalDateTime dateTime, ExcelCellStyle style) {
    return new ExcelCell(row, column, CellType.NUMERIC, dateTime, null, style);
  }

  /**
   * Creates a formula cell.
   *
   * @param row row index
   * @param column column index
   * @param formula formula text (without =)
   */
  public static ExcelCell formula(int row, int column, String formula) {
    return new ExcelCell(row, column, CellType.FORMULA, null, formula, null);
  }

  /**
   * Creates a formula cell with style.
   *
   * @param row row index
   * @param column column index
   * @param formula formula text (without =)
   * @param style cell style
   */
  public static ExcelCell formula(int row, int column, String formula, ExcelCellStyle style) {
    return new ExcelCell(row, column, CellType.FORMULA, null, formula, style);
  }

  /**
   * Gets the string representation of the cell value.
   *
   * @return string value or empty string for blank cells
   */
  public String getStringValue() {
    return switch (cellType) {
      case BLANK, ERROR -> "";
      case STRING -> value != null ? value.toString() : "";
      case NUMERIC, BOOLEAN -> value != null ? value.toString() : "";
      case FORMULA -> formula != null ? "=" + formula : "";
    };
  }

  /**
   * Gets the numeric value of the cell.
   *
   * @return numeric value or 0 for non-numeric cells
   */
  public double getNumericValue() {
    return switch (cellType) {
      case NUMERIC -> value instanceof Number n ? n.doubleValue() : 0.0;
      case BOOLEAN -> value instanceof Boolean b ? (b ? 1.0 : 0.0) : 0.0;
      default -> 0.0;
    };
  }

  /**
   * Gets the boolean value of the cell.
   *
   * @return boolean value or false for non-boolean cells
   */
  public boolean getBooleanValue() {
    return switch (cellType) {
      case BOOLEAN -> value instanceof Boolean b && b;
      case NUMERIC -> value instanceof Number n && n.doubleValue() != 0.0;
      case STRING -> "true".equalsIgnoreCase(getStringValue());
      default -> false;
    };
  }

  /**
   * Checks if the cell is empty (blank or null value).
   *
   * @return true if cell is empty
   */
  public boolean isEmpty() {
    return cellType == CellType.BLANK || value == null;
  }

  /**
   * Gets the column letter (A, B, C, ..., Z, AA, AB, etc.).
   *
   * @return column letter representation
   */
  public String getColumnLetter() {
    return columnToLetter(column);
  }

  /**
   * Gets the cell address in A1 notation (e.g., A1, B5, AA10).
   *
   * @return cell address
   */
  public String getAddress() {
    return getColumnLetter() + (row + 1);
  }

  /**
   * Converts column index to letter.
   *
   * @param column 0-based column index
   * @return column letter (A, B, ..., Z, AA, AB, ...)
   */
  public static String columnToLetter(int column) {
    StringBuilder result = new StringBuilder();
    while (column >= 0) {
      result.insert(0, (char) ('A' + (column % 26)));
      column = (column / 26) - 1;
    }
    return result.toString();
  }

  /**
   * Converts column letter to index.
   *
   * @param letter column letter (A, B, ..., Z, AA, AB, ...)
   * @return 0-based column index
   */
  public static int letterToColumn(String letter) {
    int result = 0;
    for (int i = 0; i < letter.length(); i++) {
      result = result * 26 + (letter.charAt(i) - 'A' + 1);
    }
    return result - 1;
  }
}
