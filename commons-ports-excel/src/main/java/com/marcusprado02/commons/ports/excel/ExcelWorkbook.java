package com.marcusprado02.commons.ports.excel;

import java.util.*;

/**
 * Represents an Excel workbook containing multiple worksheets.
 *
 * <p>Immutable collection of worksheets with metadata and properties.
 *
 * @param worksheets list of worksheets in the workbook
 * @param activeSheetIndex index of currently active worksheet
 * @param properties custom document properties
 * @param author document author
 * @param title document title
 * @param subject document subject
 * @param comments document comments
 * @since 0.1.0
 */
public record ExcelWorkbook(
    List<ExcelWorksheet> worksheets,
    int activeSheetIndex,
    Map<String, String> properties,
    String author,
    String title,
    String subject,
    String comments) {

  public ExcelWorkbook {
    worksheets = worksheets == null ? List.of() : List.copyOf(worksheets);
    properties = properties == null ? Map.of() : Map.copyOf(properties);
    if (activeSheetIndex < 0 || (!worksheets.isEmpty() && activeSheetIndex >= worksheets.size())) {
      throw new IllegalArgumentException("Active sheet index out of bounds");
    }
  }

  /**
   * Gets a worksheet by index.
   *
   * @param index 0-based worksheet index
   * @return worksheet or null if index is out of bounds
   */
  public ExcelWorksheet getWorksheet(int index) {
    return index >= 0 && index < worksheets.size() ? worksheets.get(index) : null;
  }

  /**
   * Gets a worksheet by name.
   *
   * @param name worksheet name
   * @return worksheet or null if not found
   */
  public ExcelWorksheet getWorksheet(String name) {
    return worksheets.stream().filter(sheet -> sheet.name().equals(name)).findFirst().orElse(null);
  }

  /**
   * Gets the active worksheet.
   *
   * @return active worksheet or null if no worksheets
   */
  public ExcelWorksheet getActiveWorksheet() {
    return getWorksheet(activeSheetIndex);
  }

  /**
   * Gets the number of worksheets.
   *
   * @return worksheet count
   */
  public int getWorksheetCount() {
    return worksheets.size();
  }

  /**
   * Gets worksheet names.
   *
   * @return list of worksheet names
   */
  public List<String> getWorksheetNames() {
    return worksheets.stream().map(ExcelWorksheet::name).toList();
  }

  /**
   * Checks if the workbook is empty.
   *
   * @return true if no worksheets or all worksheets are empty
   */
  public boolean isEmpty() {
    return worksheets.isEmpty() || worksheets.stream().allMatch(ExcelWorksheet::isEmpty);
  }

  /**
   * Gets the total number of cells with data across all worksheets.
   *
   * @return total cell count
   */
  public int getTotalCellCount() {
    return worksheets.stream().mapToInt(ExcelWorksheet::getCellCount).sum();
  }

  /** Creates a new builder for ExcelWorkbook. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for ExcelWorkbook. */
  public static class Builder {
    private List<ExcelWorksheet> worksheets = new ArrayList<>();
    private int activeSheetIndex = 0;
    private Map<String, String> properties = new HashMap<>();
    private String author;
    private String title;
    private String subject;
    private String comments;

    public Builder worksheet(ExcelWorksheet worksheet) {
      this.worksheets.add(worksheet);
      return this;
    }

    public Builder worksheets(List<ExcelWorksheet> worksheets) {
      this.worksheets.addAll(worksheets);
      return this;
    }

    public Builder activeSheetIndex(int activeSheetIndex) {
      this.activeSheetIndex = activeSheetIndex;
      return this;
    }

    public Builder property(String key, String value) {
      this.properties.put(key, value);
      return this;
    }

    public Builder properties(Map<String, String> properties) {
      this.properties.putAll(properties);
      return this;
    }

    public Builder author(String author) {
      this.author = author;
      return this;
    }

    public Builder title(String title) {
      this.title = title;
      return this;
    }

    public Builder subject(String subject) {
      this.subject = subject;
      return this;
    }

    public Builder comments(String comments) {
      this.comments = comments;
      return this;
    }

    public ExcelWorkbook build() {
      return new ExcelWorkbook(
          worksheets, activeSheetIndex, properties, author, title, subject, comments);
    }
  }
}
