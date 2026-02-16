package com.marcusprado02.commons.ports.excel;

/**
 * Options for writing Excel files.
 *
 * @param format output format (XLSX or XLS)
 * @param compress whether to enable compression
 * @param password password for encryption (null for no encryption)
 * @param writeFormulas whether to write formulas or calculated values
 * @param writeComments whether to write cell comments
 * @param autoSizeColumns whether to auto-size columns to fit content
 * @param lockStructure whether to lock workbook structure
 * @param removePersonalInfo whether to remove personal information
 * @param creator creator/author name
 * @since 0.1.0
 */
public record ExcelWriteOptions(
    ExcelFormat format,
    boolean compress,
    String password,
    boolean writeFormulas,
    boolean writeComments,
    boolean autoSizeColumns,
    boolean lockStructure,
    boolean removePersonalInfo,
    String creator) {

  /** Excel file formats. */
  public enum ExcelFormat {
    /** Excel 2007+ format (.xlsx) - default */
    XLSX,
    /** Excel 97-2003 format (.xls) - legacy */
    XLS
  }

  /** Creates default write options (XLSX format, compressed). */
  public static ExcelWriteOptions defaults() {
    return new Builder().build();
  }

  /** Creates options for legacy Excel format. */
  public static ExcelWriteOptions xls() {
    return new Builder().format(ExcelFormat.XLS).build();
  }

  /** Creates options with password protection. */
  public static ExcelWriteOptions encrypted(String password) {
    return new Builder().password(password).build();
  }

  /** Creates options optimized for performance (minimal features). */
  public static ExcelWriteOptions performance() {
    return new Builder()
        .compress(false)
        .writeFormulas(false)
        .writeComments(false)
        .autoSizeColumns(false)
        .build();
  }

  /** Creates a new builder. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for ExcelWriteOptions. */
  public static class Builder {
    private ExcelFormat format = ExcelFormat.XLSX;
    private boolean compress = true;
    private String password;
    private boolean writeFormulas = true;
    private boolean writeComments = true;
    private boolean autoSizeColumns = false;
    private boolean lockStructure = false;
    private boolean removePersonalInfo = false;
    private String creator;

    public Builder format(ExcelFormat format) {
      this.format = format;
      return this;
    }

    public Builder compress(boolean compress) {
      this.compress = compress;
      return this;
    }

    public Builder password(String password) {
      this.password = password;
      return this;
    }

    public Builder writeFormulas(boolean writeFormulas) {
      this.writeFormulas = writeFormulas;
      return this;
    }

    public Builder writeComments(boolean writeComments) {
      this.writeComments = writeComments;
      return this;
    }

    public Builder autoSizeColumns(boolean autoSizeColumns) {
      this.autoSizeColumns = autoSizeColumns;
      return this;
    }

    public Builder lockStructure(boolean lockStructure) {
      this.lockStructure = lockStructure;
      return this;
    }

    public Builder removePersonalInfo(boolean removePersonalInfo) {
      this.removePersonalInfo = removePersonalInfo;
      return this;
    }

    public Builder creator(String creator) {
      this.creator = creator;
      return this;
    }

    public ExcelWriteOptions build() {
      return new ExcelWriteOptions(
          format,
          compress,
          password,
          writeFormulas,
          writeComments,
          autoSizeColumns,
          lockStructure,
          removePersonalInfo,
          creator);
    }
  }
}
