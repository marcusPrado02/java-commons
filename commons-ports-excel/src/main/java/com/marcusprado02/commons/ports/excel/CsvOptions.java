package com.marcusprado02.commons.ports.excel;

/**
 * Options for CSV conversion operations.
 *
 * @param delimiter field delimiter character
 * @param quote quote character for text fields
 * @param escape escape character for special characters
 * @param includeHeaders whether to include column headers
 * @param encoding text encoding (UTF-8, ISO-8859-1, etc.)
 * @param lineSeparator line separator (\n, \r\n, \r)
 * @param nullValue string representation of null values
 * @param dateFormat date format pattern
 * @param numberFormat number format pattern
 * @param skipEmptyLines whether to skip empty lines when parsing
 * @since 0.1.0
 */
public record CsvOptions(
    char delimiter,
    char quote,
    char escape,
    boolean includeHeaders,
    String encoding,
    String lineSeparator,
    String nullValue,
    String dateFormat,
    String numberFormat,
    boolean skipEmptyLines) {

  /** Creates default CSV options (comma-separated, UTF-8, headers included). */
  public static CsvOptions defaults() {
    return new Builder().build();
  }

  /** Creates options for semicolon-separated files. */
  public static CsvOptions semicolon() {
    return new Builder().delimiter(';').build();
  }

  /** Creates options for tab-separated files. */
  public static CsvOptions tab() {
    return new Builder().delimiter('\t').build();
  }

  /** Creates options for pipe-separated files. */
  public static CsvOptions pipe() {
    return new Builder().delimiter('|').build();
  }

  /** Creates options without headers. */
  public static CsvOptions noHeaders() {
    return new Builder().includeHeaders(false).build();
  }

  /** Creates a new builder. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for CsvOptions. */
  public static class Builder {
    private char delimiter = ',';
    private char quote = '"';
    private char escape = '\\';
    private boolean includeHeaders = true;
    private String encoding = "UTF-8";
    private String lineSeparator = System.lineSeparator();
    private String nullValue = "";
    private String dateFormat = "yyyy-MM-dd";
    private String numberFormat;
    private boolean skipEmptyLines = true;

    public Builder delimiter(char delimiter) {
      this.delimiter = delimiter;
      return this;
    }

    public Builder quote(char quote) {
      this.quote = quote;
      return this;
    }

    public Builder escape(char escape) {
      this.escape = escape;
      return this;
    }

    public Builder includeHeaders(boolean includeHeaders) {
      this.includeHeaders = includeHeaders;
      return this;
    }

    public Builder encoding(String encoding) {
      this.encoding = encoding;
      return this;
    }

    public Builder lineSeparator(String lineSeparator) {
      this.lineSeparator = lineSeparator;
      return this;
    }

    public Builder nullValue(String nullValue) {
      this.nullValue = nullValue;
      return this;
    }

    public Builder dateFormat(String dateFormat) {
      this.dateFormat = dateFormat;
      return this;
    }

    public Builder numberFormat(String numberFormat) {
      this.numberFormat = numberFormat;
      return this;
    }

    public Builder skipEmptyLines(boolean skipEmptyLines) {
      this.skipEmptyLines = skipEmptyLines;
      return this;
    }

    public CsvOptions build() {
      return new CsvOptions(
          delimiter,
          quote,
          escape,
          includeHeaders,
          encoding,
          lineSeparator,
          nullValue,
          dateFormat,
          numberFormat,
          skipEmptyLines);
    }
  }
}
