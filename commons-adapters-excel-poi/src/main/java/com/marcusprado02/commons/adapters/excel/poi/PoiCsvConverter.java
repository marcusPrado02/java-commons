package com.marcusprado02.commons.adapters.excel.poi;

import com.marcusprado02.commons.ports.excel.*;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.*;

/**
 * Utility class for converting between Excel and CSV formats using Apache Commons CSV.
 *
 * @since 0.1.0
 */
public final class PoiCsvConverter {

  private PoiCsvConverter() {
    // Utility class
  }

  /**
   * Converts an Excel workbook (or specific worksheet) to CSV format.
   *
   * @param workbook Excel workbook
   * @param worksheetName specific worksheet name (null for active sheet)
   * @param options CSV conversion options
   * @return CSV content as string
   * @throws Exception if conversion fails
   */
  public static String toCsv(ExcelWorkbook workbook, String worksheetName, CsvOptions options) throws Exception {
    // Select worksheet
    ExcelWorksheet worksheet;
    if (worksheetName != null) {
      worksheet = workbook.getWorksheet(worksheetName);
      if (worksheet == null) {
        throw new IllegalArgumentException("Worksheet '" + worksheetName + "' not found");
      }
    } else {
      worksheet = workbook.getActiveWorksheet();
      if (worksheet == null) {
        throw new IllegalArgumentException("No active worksheet found");
      }
    }

    if (worksheet.isEmpty()) {
      return "";
    }

    // Create CSV format
    CSVFormat csvFormat = createCsvFormat(options);

    try (StringWriter writer = new StringWriter();
         CSVPrinter printer = new CSVPrinter(writer, csvFormat)) {

      // Get worksheet range
      int maxRow = worksheet.getLastRowNum();
      int maxCol = worksheet.getLastColumnNum();

      // Write header row if needed
      if (options.includeHeaders() && maxRow >= 0) {
        List<String> headers = new ArrayList<>();
        List<ExcelCell> headerRow = worksheet.getRow(0);

        for (int col = 0; col <= maxCol; col++) {
          ExcelCell cell = findCellInRow(headerRow, col);
          String header = cell != null ? formatCellForCsv(cell, options) : "";
          headers.add(header);
        }

        printer.printRecord(headers);
      }

      // Write data rows
      int startRow = options.includeHeaders() ? 1 : 0;
      for (int row = startRow; row <= maxRow; row++) {
        List<ExcelCell> rowCells = worksheet.getRow(row);

        // Skip empty lines if requested
        if (options.skipEmptyLines() && isRowEmpty(rowCells)) {
          continue;
        }

        List<String> values = new ArrayList<>();
        for (int col = 0; col <= maxCol; col++) {
          ExcelCell cell = findCellInRow(rowCells, col);
          String value = cell != null ? formatCellForCsv(cell, options) : options.nullValue();
          values.add(value);
        }

        printer.printRecord(values);
      }

      return writer.toString();
    }
  }

  /**
   * Converts CSV content to an Excel workbook.
   *
   * @param csvContent CSV data as string
   * @param worksheetName name for the created worksheet
   * @param options CSV parsing options
   * @return Excel workbook
   * @throws Exception if conversion fails
   */
  public static ExcelWorkbook fromCsv(String csvContent, String worksheetName, CsvOptions options) throws Exception {
    CSVFormat csvFormat = createCsvFormat(options);

    var worksheetBuilder = ExcelWorksheet.builder(worksheetName != null ? worksheetName : "Sheet1");

    try (StringReader reader = new StringReader(csvContent);
         CSVParser parser = new CSVParser(reader, csvFormat)) {

      List<CSVRecord> records = parser.getRecords();
      if (records.isEmpty()) {
        return ExcelWorkbook.builder().worksheet(worksheetBuilder.build()).build();
      }

      DateTimeFormatter dateFormatter = options.dateFormat() != null
          ? DateTimeFormatter.ofPattern(options.dateFormat())
          : DateTimeFormatter.ofPattern("yyyy-MM-dd");

      int rowIndex = 0;

      // Handle headers
      if (options.includeHeaders()) {
        CSVRecord headerRecord = records.get(0);
        for (int col = 0; col < headerRecord.size(); col++) {
          String header = headerRecord.get(col);
          ExcelCell cell = ExcelCell.text(rowIndex, col, header, ExcelCellStyle.headerStyle());
          worksheetBuilder.cell(cell);
        }
        rowIndex++;
      }

      // Process data rows
      int startIndex = options.includeHeaders() ? 1 : 0;
      for (int i = startIndex; i < records.size(); i++) {
        CSVRecord record = records.get(i);

        // Skip empty lines if requested
        if (options.skipEmptyLines() && isRecordEmpty(record)) {
          continue;
        }

        for (int col = 0; col < record.size(); col++) {
          String value = record.get(col);
          ExcelCell cell = parseCsvValue(value, rowIndex, col, options, dateFormatter);
          worksheetBuilder.cell(cell);
        }
        rowIndex++;
      }

      ExcelWorksheet worksheet = worksheetBuilder.build();
      return ExcelWorkbook.builder()
          .worksheet(worksheet)
          .title("Imported from CSV")
          .build();
    }
  }

  /**
   * Creates a CSV format configuration from options.
   */
  private static CSVFormat createCsvFormat(CsvOptions options) {
    return CSVFormat.DEFAULT
        .withDelimiter(options.delimiter())
        .withQuote(options.quote())
        .withEscape(options.escape())
        .withRecordSeparator(options.lineSeparator())
        .withNullString(options.nullValue())
        .withIgnoreEmptyLines(options.skipEmptyLines());
  }

  /**
   * Formats an Excel cell value for CSV output.
   */
  private static String formatCellForCsv(ExcelCell cell, CsvOptions options) {
    if (cell.isEmpty()) {
      return options.nullValue();
    }

    return switch (cell.cellType()) {
      case BLANK -> options.nullValue();
      case STRING -> cell.getStringValue();
      case BOOLEAN -> String.valueOf(cell.getBooleanValue());
      case NUMERIC -> {
        if (cell.value() instanceof LocalDate date) {
          DateTimeFormatter formatter = options.dateFormat() != null
              ? DateTimeFormatter.ofPattern(options.dateFormat())
              : DateTimeFormatter.ofPattern("yyyy-MM-dd");
          yield date.format(formatter);
        } else if (cell.value() instanceof LocalDateTime dateTime) {
          DateTimeFormatter formatter = options.dateFormat() != null
              ? DateTimeFormatter.ofPattern(options.dateFormat())
              : DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
          yield dateTime.format(formatter);
        } else {
          // Format numbers
          if (options.numberFormat() != null) {
            yield String.format(options.numberFormat(), cell.getNumericValue());
          } else {
            Double numValue = cell.getNumericValue();
            // Remove .0 for whole numbers
            yield numValue % 1 == 0 ? String.valueOf(numValue.intValue()) : numValue.toString();
          }
        }
      }
      case FORMULA -> {
        // Use calculated value if available, otherwise formula text
        Object value = cell.value();
        if (value != null) {
          if (value instanceof Number number) {
            yield number.toString();
          } else {
            yield value.toString();
          }
        } else {
          yield "=" + (cell.formula() != null ? cell.formula() : "");
        }
      }
      case ERROR -> "#ERROR";
    };
  }

  /**
   * Parses a CSV value into an Excel cell with appropriate type detection.
   */
  private static ExcelCell parseCsvValue(String value, int row, int col, CsvOptions options, DateTimeFormatter dateFormatter) {
    // Handle null/empty values
    if (value == null || value.equals(options.nullValue()) || value.trim().isEmpty()) {
      return ExcelCell.blank(row, col);
    }

    value = value.trim();

    // Try to parse as boolean
    if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
      return ExcelCell.bool(row, col, Boolean.parseBoolean(value));
    }

    // Try to parse as number
    try {
      if (value.contains(".") || value.contains(",")) {
        double doubleValue = Double.parseDouble(value);
        return ExcelCell.number(row, col, doubleValue);
      } else {
        long longValue = Long.parseLong(value);
        return ExcelCell.number(row, col, longValue);
      }
    } catch (NumberFormatException e) {
      // Not a number, continue
    }

    // Try to parse as date
    try {
      LocalDate date = LocalDate.parse(value, dateFormatter);
      return ExcelCell.date(row, col, date);
    } catch (DateTimeParseException e) {
      // Not a date, continue
    }

    // Try to parse as datetime
    try {
      DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
      LocalDateTime dateTime = LocalDateTime.parse(value, dateTimeFormatter);
      return ExcelCell.dateTime(row, col, dateTime);
    } catch (DateTimeParseException e) {
      // Not a datetime, continue
    }

    // Treat as string
    return ExcelCell.text(row, col, value);
  }

  /**
   * Finds a cell with the specified column in a row.
   */
  private static ExcelCell findCellInRow(List<ExcelCell> rowCells, int column) {
    if (rowCells == null) {
      return null;
    }
    return rowCells.stream()
        .filter(cell -> cell.column() == column)
        .findFirst()
        .orElse(null);
  }

  /**
   * Checks if a row is empty (all cells are blank).
   */
  private static boolean isRowEmpty(List<ExcelCell> rowCells) {
    if (rowCells == null || rowCells.isEmpty()) {
      return true;
    }
    return rowCells.stream().allMatch(ExcelCell::isEmpty);
  }

  /**
   * Checks if a CSV record is empty.
   */
  private static boolean isRecordEmpty(CSVRecord record) {
    for (String value : record) {
      if (value != null && !value.trim().isEmpty()) {
        return false;
      }
    }
    return true;
  }
}
