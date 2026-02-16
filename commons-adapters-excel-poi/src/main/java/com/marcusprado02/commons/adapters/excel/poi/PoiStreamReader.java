package com.marcusprado02.commons.adapters.excel.poi;

import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Problems;
import com.marcusprado02.commons.kernel.errors.Severity;
import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.excel.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.StreamSupport;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.SharedStringsTable;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.openxml4j.opc.OPCPackage;

/**
 * Streaming reader implementation using Apache POI for memory-efficient Excel file processing.
 *
 * @since 0.1.0
 */
public class PoiStreamReader implements ExcelStreamReader {

  private final Path filePath;
  private final ExcelReadOptions options;
  private final PoiConfiguration configuration;

  private Workbook workbook;
  private Sheet currentSheet;
  private Iterator<Row> rowIterator;
  private int currentRowNum = -1;
  private List<String> worksheetNames;

  /**
   * Creates a new streaming reader for the specified file.
   *
   * @param filePath path to Excel file
   * @param options read options
   * @param configuration POI configuration
   * @throws IOException if file cannot be opened
   */
  public PoiStreamReader(Path filePath, ExcelReadOptions options, PoiConfiguration configuration) throws IOException {
    this.filePath = filePath;
    this.options = options;
    this.configuration = configuration;
    initialize();
  }

  /**
   * Initializes the reader by opening the workbook and reading sheet names.
   */
  private void initialize() throws IOException {
    try {
      this.workbook = WorkbookFactory.create(filePath.toFile(), options.password(), configuration.readOnlyMode());
      this.worksheetNames = new ArrayList<>();

      for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
        Sheet sheet = workbook.getSheetAt(i);
        if (sheet != null && (options.readHiddenSheets() || !workbook.isSheetHidden(i))) {
          worksheetNames.add(sheet.getSheetName());
        }
      }
    } catch (Exception e) {
      throw new IOException("Failed to open Excel file: " + e.getMessage(), e);
    }
  }

  @Override
  public Result<List<String>> getWorksheetNames() {
    return Result.ok(new ArrayList<>(worksheetNames));
  }

  @Override
  public Result<Void> selectWorksheet(String worksheetName) {
    if (workbook == null) {
      return Result.fail(Problems.business("EXCEL_READER_CLOSED", "Reader has been closed"));
    }

    try {
      Sheet sheet = workbook.getSheet(worksheetName);
      if (sheet == null) {
        return Result.fail(Problems.notFound("EXCEL_WORKSHEET_NOT_FOUND", "Worksheet '" + worksheetName + "' not found"));
      }

      this.currentSheet = sheet;
      this.rowIterator = sheet.iterator();
      this.currentRowNum = -1;

      return Result.ok(null);
    } catch (Exception e) {
      return Result.fail(Problems.technical("EXCEL_WORKSHEET_SELECT_ERROR", "Failed to select worksheet: " + e.getMessage()));
    }
  }

  @Override
  public boolean hasNext() {
    if (rowIterator == null) {
      return false;
    }

    // Skip empty rows if requested
    if (options.skipBlankRows()) {
      while (rowIterator.hasNext()) {
        Row nextRow = rowIterator.next();
        if (isRowEmpty(nextRow)) {
          currentRowNum = nextRow.getRowNum();
          continue;
        }
        // Put the row back (we'll read it in readNext)
        // Note: POI doesn't support putting back, so we need a different approach
        return true;
      }
      return false;
    }

    return rowIterator.hasNext();
  }

  @Override
  public Result<List<ExcelCell>> readNext() {
    if (rowIterator == null) {
      return Result.fail(Problems.business("EXCEL_NO_WORKSHEET", "No worksheet selected"));
    }

    if (!rowIterator.hasNext()) {
      return Result.fail(Problems.business("EXCEL_NO_MORE_ROWS", "No more rows to read"));
    }

    try {
      Row poiRow = rowIterator.next();
      currentRowNum = poiRow.getRowNum();

      // Skip empty rows if requested
      if (options.skipBlankRows() && isRowEmpty(poiRow)) {
        return readNext(); // Recurse to get next non-empty row
      }

      // Check row limit
      if (options.maxRows() > 0 && currentRowNum >= options.maxRows()) {
        return Result.fail(Problems.validation("EXCEL_MAX_ROWS_EXCEEDED", "Maximum row limit reached: " + options.maxRows()));
      }

      List<ExcelCell> cells = new ArrayList<>();
      int maxColumn = options.maxColumns() > 0
          ? Math.min(poiRow.getLastCellNum(), options.maxColumns())
          : poiRow.getLastCellNum();

      for (int col = 0; col < maxColumn; col++) {
        Cell poiCell = poiRow.getCell(col);
        if (poiCell != null) {
          ExcelCell cell = PoiCellMapper.fromPoi(poiCell);
          cells.add(cell);
        } else {
          // Create blank cell for missing cells to maintain column alignment
          cells.add(ExcelCell.blank(currentRowNum, col));
        }
      }

      return Result.ok(cells);
    } catch (Exception e) {
      return Result.fail(Problems.technical("EXCEL_ROW_READ_ERROR", "Failed to read row: " + e.getMessage()));
    }
  }

  @Override
  public int getCurrentRowNum() {
    return currentRowNum;
  }

  @Override
  public Result<Void> skip(int count) {
    if (count <= 0) {
      return Result.ok(null);
    }

    if (rowIterator == null) {
      return Result.fail(Problems.business("EXCEL_NO_WORKSHEET", "No worksheet selected"));
    }

    try {
      for (int i = 0; i < count && rowIterator.hasNext(); i++) {
        Row row = rowIterator.next();
        currentRowNum = row.getRowNum();
      }
      return Result.ok(null);
    } catch (Exception e) {
      return Result.fail(Problems.technical("EXCEL_SKIP_ROWS_ERROR", "Failed to skip rows: " + e.getMessage()));
    }
  }

  /**
   * Checks if a row is empty (all cells are blank).
   */
  private boolean isRowEmpty(Row row) {
    if (row == null) {
      return true;
    }

    for (Cell cell : row) {
      if (cell != null && cell.getCellType() != org.apache.poi.ss.usermodel.CellType.BLANK) {
        String stringValue = cell.toString().trim();
        if (!stringValue.isEmpty()) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public void close() {
    if (workbook != null) {
      try {
        workbook.close();
      } catch (IOException e) {
        // Log but don't throw - we're closing
      }
      workbook = null;
    }
    currentSheet = null;
    rowIterator = null;
    worksheetNames = null;
  }
}

/**
 * Helper class for mapping POI cells to Commons cells.
 */
final class PoiCellMapper {

  private PoiCellMapper() {
    // Utility class
  }

  /**
   * Converts a POI Cell to Commons ExcelCell with simplified style mapping for streaming.
   */
  public static ExcelCell fromPoi(Cell poiCell) {
    int row = poiCell.getRowIndex();
    int column = poiCell.getColumnIndex();

    com.marcusprado02.commons.ports.excel.CellType cellType = mapCellType(poiCell.getCellType());
    Object value = extractCellValue(poiCell);
    String formula = poiCell.getCellType() == org.apache.poi.ss.usermodel.CellType.FORMULA
        ? poiCell.getCellFormula() : null;

    // For streaming, we skip detailed style mapping to improve performance
    ExcelCellStyle style = null;

    return new ExcelCell(row, column, cellType, value, formula, style);
  }

  private static com.marcusprado02.commons.ports.excel.CellType mapCellType(org.apache.poi.ss.usermodel.CellType poiCellType) {
    return switch (poiCellType) {
      case BLANK -> com.marcusprado02.commons.ports.excel.CellType.BLANK;
      case BOOLEAN -> com.marcusprado02.commons.ports.excel.CellType.BOOLEAN;
      case NUMERIC -> com.marcusprado02.commons.ports.excel.CellType.NUMERIC;
      case STRING -> com.marcusprado02.commons.ports.excel.CellType.STRING;
      case FORMULA -> com.marcusprado02.commons.ports.excel.CellType.FORMULA;
      case ERROR -> com.marcusprado02.commons.ports.excel.CellType.ERROR;
      default -> com.marcusprado02.commons.ports.excel.CellType.STRING;
    };
  }

  private static Object extractCellValue(Cell poiCell) {
    return switch (poiCell.getCellType()) {
      case BLANK -> null;
      case BOOLEAN -> poiCell.getBooleanCellValue();
      case NUMERIC -> {
        if (DateUtil.isCellDateFormatted(poiCell)) {
          Date date = poiCell.getDateCellValue();
          yield date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        } else {
          yield poiCell.getNumericCellValue();
        }
      }
      case STRING -> poiCell.getStringCellValue();
      case FORMULA -> {
        try {
          yield switch (poiCell.getCachedFormulaResultType()) {
            case BOOLEAN -> poiCell.getBooleanCellValue();
            case NUMERIC -> {
              if (DateUtil.isCellDateFormatted(poiCell)) {
                Date date = poiCell.getDateCellValue();
                yield date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
              } else {
                yield poiCell.getNumericCellValue();
              }
            }
            case STRING -> poiCell.getStringCellValue();
            default -> null;
          };
        } catch (Exception e) {
          yield null;
        }
      }
      case ERROR -> "#ERROR";
      default -> poiCell.toString();
    };
  }
}
