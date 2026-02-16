package com.marcusprado02.commons.ports.excel;

/**
 * Options for reading Excel files.
 *
 * @param readAllSheets whether to read all worksheets or just the active one
 * @param readFormulas whether to read formula expressions or just calculated values
 * @param readComments whether to read cell comments
 * @param readHiddenSheets whether to include hidden worksheets
 * @param password password for encrypted files
 * @param maxRows maximum number of rows to read (0 = no limit)
 * @param maxColumns maximum number of columns to read (0 = no limit)
 * @param skipBlankRows whether to skip completely blank rows
 * @param dateFormat format for parsing date cells
 * @since 0.1.0
 */
public record ExcelReadOptions(
    boolean readAllSheets,
    boolean readFormulas,
    boolean readComments,
    boolean readHiddenSheets,
    String password,
    int maxRows,
    int maxColumns,
    boolean skipBlankRows,
    String dateFormat) {

  public ExcelReadOptions {
    if (maxRows < 0) throw new IllegalArgumentException("Max rows must be >= 0");
    if (maxColumns < 0) throw new IllegalArgumentException("Max columns must be >= 0");
  }

  /** Creates default read options. */
  public static ExcelReadOptions defaults() {
    return new Builder().build();
  }

  /** Creates options for reading all worksheets. */
  public static ExcelReadOptions allSheets() {
    return new Builder().readAllSheets(true).build();
  }

  /** Creates options for reading formulas as text. */
  public static ExcelReadOptions withFormulas() {
    return new Builder().readFormulas(true).build();
  }

  /** Creates options for streaming large files. */
  public static ExcelReadOptions streaming() {
    return new Builder().skipBlankRows(true).build();
  }

  /** Creates a new builder. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for ExcelReadOptions. */
  public static class Builder {
    private boolean readAllSheets = false;
    private boolean readFormulas = false;
    private boolean readComments = false;
    private boolean readHiddenSheets = false;
    private String password;
    private int maxRows = 0;
    private int maxColumns = 0;
    private boolean skipBlankRows = false;
    private String dateFormat;

    public Builder readAllSheets(boolean readAllSheets) {
      this.readAllSheets = readAllSheets;
      return this;
    }

    public Builder readFormulas(boolean readFormulas) {
      this.readFormulas = readFormulas;
      return this;
    }

    public Builder readComments(boolean readComments) {
      this.readComments = readComments;
      return this;
    }

    public Builder readHiddenSheets(boolean readHiddenSheets) {
      this.readHiddenSheets = readHiddenSheets;
      return this;
    }

    public Builder password(String password) {
      this.password = password;
      return this;
    }

    public Builder maxRows(int maxRows) {
      this.maxRows = maxRows;
      return this;
    }

    public Builder maxColumns(int maxColumns) {
      this.maxColumns = maxColumns;
      return this;
    }

    public Builder skipBlankRows(boolean skipBlankRows) {
      this.skipBlankRows = skipBlankRows;
      return this;
    }

    public Builder dateFormat(String dateFormat) {
      this.dateFormat = dateFormat;
      return this;
    }

    public ExcelReadOptions build() {
      return new ExcelReadOptions(
          readAllSheets,
          readFormulas,
          readComments,
          readHiddenSheets,
          password,
          maxRows,
          maxColumns,
          skipBlankRows,
          dateFormat);
    }
  }
}
