package com.marcusprado02.commons.ports.excel;

import com.marcusprado02.commons.kernel.result.Result;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

/**
 * Port interface for Excel spreadsheet operations.
 *
 * <p>Provides functionality to read from and write to Excel files (.xls, .xlsx) with support for
 * multiple formats, streaming large files, and CSV conversion.
 *
 * @since 0.1.0
 */
public interface ExcelPort {

  /**
   * Reads an Excel workbook from a file path.
   *
   * @param filePath path to Excel file (.xls or .xlsx)
   * @return result containing workbook or error
   */
  Result<ExcelWorkbook> readWorkbook(Path filePath);

  /**
   * Reads an Excel workbook from an input stream.
   *
   * @param inputStream Excel file stream
   * @param fileName file name for format detection (.xls or .xlsx)
   * @return result containing workbook or error
   */
  Result<ExcelWorkbook> readWorkbook(InputStream inputStream, String fileName);

  /**
   * Writes an Excel workbook to a file path.
   *
   * @param workbook workbook to write
   * @param filePath output file path
   * @param options write options
   * @return result indicating success or error
   */
  Result<Void> writeWorkbook(ExcelWorkbook workbook, Path filePath, ExcelWriteOptions options);

  /**
   * Writes an Excel workbook to an output stream.
   *
   * @param workbook workbook to write
   * @param outputStream output stream
   * @param options write options
   * @return result indicating success or error
   */
  Result<Void> writeWorkbook(ExcelWorkbook workbook, OutputStream outputStream, ExcelWriteOptions options);

  /**
   * Creates a streaming reader for large Excel files.
   *
   * @param filePath path to Excel file
   * @param options read options
   * @return result containing streaming reader or error
   */
  Result<ExcelStreamReader> createStreamReader(Path filePath, ExcelReadOptions options);

  /**
   * Creates a streaming writer for large Excel files.
   *
   * @param filePath output file path
   * @param options write options
   * @return result containing streaming writer or error
   */
  Result<ExcelStreamWriter> createStreamWriter(Path filePath, ExcelWriteOptions options);

  /**
   * Converts Excel workbook to CSV format.
   *
   * @param workbook workbook to convert
   * @param worksheetName specific worksheet to convert (null for active sheet)
   * @param options CSV conversion options
   * @return result containing CSV content or error
   */
  Result<String> toCsv(ExcelWorkbook workbook, String worksheetName, CsvOptions options);

  /**
   * Converts CSV content to Excel workbook.
   *
   * @param csvContent CSV data
   * @param worksheetName name for the created worksheet
   * @param options CSV parsing options
   * @return result containing workbook or error
   */
  Result<ExcelWorkbook> fromCsv(String csvContent, String worksheetName, CsvOptions options);

  /**
   * Validates Excel file format and structure.
   *
   * @param filePath path to Excel file
   * @return result containing validation info or error
   */
  Result<ExcelValidationResult> validateFile(Path filePath);

  /**
   * Gets supported Excel formats.
   *
   * @return list of supported file extensions
   */
  default java.util.List<String> getSupportedFormats() {
    return java.util.List.of(".xlsx", ".xls");
  }

  /**
   * Gets maximum supported rows per worksheet.
   *
   * @return maximum row count (implementation dependent)
   */
  default int getMaxRows() {
    return 1048576; // Excel 2007+ limit
  }

  /**
   * Gets maximum supported columns per worksheet.
   *
   * @return maximum column count (implementation dependent)
   */
  default int getMaxColumns() {
    return 16384; // Excel 2007+ limit (XFD)
  }
}
