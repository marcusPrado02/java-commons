package com.marcusprado02.commons.adapters.excel.poi;

import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.excel.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Apache POI implementation of the Excel processing port.
 *
 * <p>Provides comprehensive Excel file support using Apache POI library, including reading/writing
 * .xlsx and .xls files, streaming operations for large files, and CSV conversion capabilities.
 *
 * @since 0.1.0
 */
public class PoiExcelAdapter implements ExcelPort {

  private final PoiConfiguration configuration;

  /**
   * Creates a new POI Excel adapter with default configuration.
   */
  public PoiExcelAdapter() {
    this.configuration = PoiConfiguration.defaults();
  }

  /**
   * Creates a new POI Excel adapter with custom configuration.
   *
   * @param configuration POI-specific configuration
   */
  public PoiExcelAdapter(PoiConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  public Result<ExcelWorkbook> readWorkbook(Path filePath) {
    try (InputStream inputStream = Files.newInputStream(filePath)) {
      return readWorkbook(inputStream, filePath.getFileName().toString());
    } catch (IOException e) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("EXCEL_READ_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to read Excel file: " + e.getMessage()));
    }
  }

  @Override
  public Result<ExcelWorkbook> readWorkbook(InputStream inputStream, String fileName) {
    try (Workbook poiWorkbook = WorkbookFactory.create(inputStream)) {
      ExcelWorkbook workbook = PoiWorkbookMapper.fromPoi(poiWorkbook);
      return Result.ok(workbook);
    } catch (IOException e) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("EXCEL_PARSE_ERROR"),
              ErrorCategory.VALIDATION,
              Severity.ERROR,
              "Failed to parse Excel file '" + fileName + "': " + e.getMessage()));
    } catch (Exception e) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("EXCEL_READ_UNEXPECTED_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Unexpected error reading Excel file: " + e.getMessage()));
    }
  }

  @Override
  public Result<Void> writeWorkbook(ExcelWorkbook workbook, Path filePath, ExcelWriteOptions options) {
    try (OutputStream outputStream = Files.newOutputStream(filePath)) {
      return writeWorkbook(workbook, outputStream, options);
    } catch (IOException e) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("EXCEL_WRITE_FILE_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to write Excel file: " + e.getMessage()));
    }
  }

  @Override
  public Result<Void> writeWorkbook(ExcelWorkbook workbook, OutputStream outputStream, ExcelWriteOptions options) {
    try {
      Workbook poiWorkbook = createPoiWorkbook(options);
      PoiWorkbookMapper.toPoi(workbook, poiWorkbook, options);

      poiWorkbook.write(outputStream);
      poiWorkbook.close();

      return Result.ok(null);
    } catch (IOException e) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("EXCEL_WRITE_WORKBOOK_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to write Excel workbook: " + e.getMessage()));
    } catch (Exception e) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("EXCEL_WRITE_UNEXPECTED_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Unexpected error writing Excel workbook: " + e.getMessage()));
    }
  }

  @Override
  public Result<ExcelStreamReader> createStreamReader(Path filePath, ExcelReadOptions options) {
    try {
      PoiStreamReader reader = new PoiStreamReader(filePath, options, configuration);
      return Result.ok(reader);
    } catch (Exception e) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("EXCEL_STREAM_READER_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to create stream reader: " + e.getMessage()));
    }
  }

  @Override
  public Result<ExcelStreamWriter> createStreamWriter(Path filePath, ExcelWriteOptions options) {
    try {
      PoiStreamWriter writer = new PoiStreamWriter(filePath, options, configuration);
      return Result.ok(writer);
    } catch (Exception e) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("EXCEL_STREAM_WRITER_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to create stream writer: " + e.getMessage()));
    }
  }

  @Override
  public Result<String> toCsv(ExcelWorkbook workbook, String worksheetName, CsvOptions options) {
    try {
      String csvContent = PoiCsvConverter.toCsv(workbook, worksheetName, options);
      return Result.ok(csvContent);
    } catch (Exception e) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("EXCEL_TO_CSV_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to convert Excel to CSV: " + e.getMessage()));
    }
  }

  @Override
  public Result<ExcelWorkbook> fromCsv(String csvContent, String worksheetName, CsvOptions options) {
    try {
      ExcelWorkbook workbook = PoiCsvConverter.fromCsv(csvContent, worksheetName, options);
      return Result.ok(workbook);
    } catch (Exception e) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("CSV_TO_EXCEL_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to convert CSV to Excel: " + e.getMessage()));
    }
  }

  @Override
  public Result<ExcelValidationResult> validateFile(Path filePath) {
    try {
      ExcelValidationResult result = PoiFileValidator.validate(filePath);
      return Result.ok(result);
    } catch (Exception e) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("EXCEL_VALIDATION_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to validate Excel file: " + e.getMessage()));
    }
  }

  @Override
  public int getMaxRows() {
    return configuration.maxRows();
  }

  @Override
  public int getMaxColumns() {
    return configuration.maxColumns();
  }

  /**
   * Creates the appropriate POI workbook based on write options.
   */
  private Workbook createPoiWorkbook(ExcelWriteOptions options) {
    return switch (options.format()) {
      case XLSX -> new XSSFWorkbook();
      case XLS -> new HSSFWorkbook();
    };
  }

  /**
   * Creates a streaming POI workbook for large file operations.
   */
  Workbook createStreamingWorkbook(ExcelWriteOptions options) {
    return switch (options.format()) {
      case XLSX -> new SXSSFWorkbook(configuration.streamingRowAccessWindow());
      case XLS -> new HSSFWorkbook(); // HSSFWorkbook doesn't support streaming
    };
  }
}
