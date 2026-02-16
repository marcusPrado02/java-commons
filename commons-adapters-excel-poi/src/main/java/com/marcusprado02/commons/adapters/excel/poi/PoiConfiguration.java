package com.marcusprado02.commons.adapters.excel.poi;

/**
 * Configuration options for Apache POI Excel adapter.
 *
 * @param enableFormulasEvaluation whether to evaluate formulas when reading
 * @param streamingRowAccessWindow number of rows to keep in memory for SXSSF streaming
 * @param maxRows maximum rows supported (Excel 2007+ = 1048576, Excel 97-2003 = 65536)
 * @param maxColumns maximum columns supported (Excel 2007+ = 16384, Excel 97-2003 = 256)
 * @param compressTemporaryFiles whether to compress temporary files during streaming
 * @param useSharedStrings whether to use shared strings table (reduces memory for repeated text)
 * @param readOnlyMode whether to open files in read-only mode for better performance
 * @param strictParsing whether to use strict OOXML parsing (may fail on non-compliant files)
 * @since 0.1.0
 */
public record PoiConfiguration(
    boolean enableFormulasEvaluation,
    int streamingRowAccessWindow,
    int maxRows,
    int maxColumns,
    boolean compressTemporaryFiles,
    boolean useSharedStrings,
    boolean readOnlyMode,
    boolean strictParsing) {

  public PoiConfiguration {
    if (streamingRowAccessWindow <= 0) {
      throw new IllegalArgumentException("Streaming row access window must be > 0");
    }
    if (maxRows <= 0) {
      throw new IllegalArgumentException("Max rows must be > 0");
    }
    if (maxColumns <= 0) {
      throw new IllegalArgumentException("Max columns must be > 0");
    }
  }

  /** Creates default configuration optimized for general use. */
  public static PoiConfiguration defaults() {
    return new Builder().build();
  }

  /** Creates configuration optimized for performance (minimal features). */
  public static PoiConfiguration performance() {
    return new Builder()
        .enableFormulasEvaluation(false)
        .streamingRowAccessWindow(100)
        .compressTemporaryFiles(false)
        .useSharedStrings(false)
        .readOnlyMode(true)
        .strictParsing(false)
        .build();
  }

  /** Creates configuration optimized for memory usage. */
  public static PoiConfiguration memoryOptimized() {
    return new Builder()
        .streamingRowAccessWindow(50)
        .compressTemporaryFiles(true)
        .useSharedStrings(true)
        .readOnlyMode(true)
        .build();
  }

  /** Creates configuration with full feature support. */
  public static PoiConfiguration fullFeatures() {
    return new Builder()
        .enableFormulasEvaluation(true)
        .streamingRowAccessWindow(1000)
        .strictParsing(true)
        .build();
  }

  /** Creates a new builder for PoiConfiguration. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for PoiConfiguration. */
  public static class Builder {
    private boolean enableFormulasEvaluation = true;
    private int streamingRowAccessWindow = 500;
    private int maxRows = 1048576; // Excel 2007+ limit
    private int maxColumns = 16384; // Excel 2007+ limit (XFD)
    private boolean compressTemporaryFiles = true;
    private boolean useSharedStrings = true;
    private boolean readOnlyMode = false;
    private boolean strictParsing = false;

    public Builder enableFormulasEvaluation(boolean enableFormulasEvaluation) {
      this.enableFormulasEvaluation = enableFormulasEvaluation;
      return this;
    }

    public Builder streamingRowAccessWindow(int streamingRowAccessWindow) {
      this.streamingRowAccessWindow = streamingRowAccessWindow;
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

    public Builder compressTemporaryFiles(boolean compressTemporaryFiles) {
      this.compressTemporaryFiles = compressTemporaryFiles;
      return this;
    }

    public Builder useSharedStrings(boolean useSharedStrings) {
      this.useSharedStrings = useSharedStrings;
      return this;
    }

    public Builder readOnlyMode(boolean readOnlyMode) {
      this.readOnlyMode = readOnlyMode;
      return this;
    }

    public Builder strictParsing(boolean strictParsing) {
      this.strictParsing = strictParsing;
      return this;
    }

    public PoiConfiguration build() {
      return new PoiConfiguration(
          enableFormulasEvaluation,
          streamingRowAccessWindow,
          maxRows,
          maxColumns,
          compressTemporaryFiles,
          useSharedStrings,
          readOnlyMode,
          strictParsing);
    }
  }
}
