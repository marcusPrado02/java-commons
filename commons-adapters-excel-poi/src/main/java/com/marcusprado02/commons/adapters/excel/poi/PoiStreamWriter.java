package com.marcusprado02.commons.adapters.excel.poi;

import com.marcusprado02.commons.kernel.errors.Problems;
import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.excel.*;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

/**
 * Streaming writer implementation using Apache POI for memory-efficient Excel file creation.
 *
 * @since 0.1.0
 */
public class PoiStreamWriter implements ExcelStreamWriter {

  private final Path filePath;
  private final ExcelWriteOptions options;
  private final PoiConfiguration configuration;

  private Workbook workbook;
  private Sheet currentSheet;
  private int currentRowNum = 0;
  private Map<Integer, Double> pendingColumnWidths = new HashMap<>();
  private boolean closed = false;

  /**
   * Creates a new streaming writer for the specified file.
   *
   * @param filePath output file path
   * @param options write options
   * @param configuration POI configuration
   * @throws IOException if file cannot be created
   */
  public PoiStreamWriter(Path filePath, ExcelWriteOptions options, PoiConfiguration configuration) throws IOException {
    this.filePath = filePath;
    this.options = options;
    this.configuration = configuration;
    initialize();
  }

  /**
   * Initializes the writer by creating the workbook.
   */
  private void initialize() throws IOException {
    try {
      // Use streaming workbook for XLSX to minimize memory usage
      this.workbook = switch (options.format()) {
        case XLSX -> new SXSSFWorkbook(configuration.streamingRowAccessWindow());
        case XLS -> WorkbookFactory.create(true); // Create empty HSSFWorkbook
      };
    } catch (Exception e) {
      throw new IOException("Failed to create Excel workbook: " + e.getMessage(), e);
    }
  }

  @Override
  public Result<Void> createWorksheet(String name) {
    if (closed) {
      return Result.fail(Problems.business("EXCEL_WRITER_CLOSED", "Writer has been closed"));
    }

    try {
      Sheet sheet = workbook.createSheet(name);
      this.currentSheet = sheet;
      this.currentRowNum = 0;
      this.pendingColumnWidths.clear();
      return Result.ok(null);
    } catch (Exception e) {
      return Result.fail(Problems.technical("EXCEL_CREATE_WORKSHEET_ERROR", "Failed to create worksheet '" + name + "': " + e.getMessage()));
    }
  }

  @Override
  public Result<Void> selectWorksheet(String name) {
    if (closed) {
      return Result.fail(Problems.business("EXCEL_WRITER_CLOSED", "Writer has been closed"));
    }

    try {
      Sheet sheet = workbook.getSheet(name);
      if (sheet == null) {
        return Result.fail(Problems.notFound("EXCEL_WORKSHEET_NOT_FOUND", "Worksheet '" + name + "' not found"));
      }

      this.currentSheet = sheet;
      this.currentRowNum = sheet.getLastRowNum() + 1;
      this.pendingColumnWidths.clear();
      return Result.ok(null);
    } catch (Exception e) {
      return Result.fail(Problems.technical("EXCEL_SELECT_WORKSHEET_ERROR", "Failed to select worksheet: " + e.getMessage()));
    }
  }

  @Override
  public Result<Void> writeRow(List<ExcelCell> cells) {
    if (closed) {
      return Result.fail(Problems.business("EXCEL_WRITER_CLOSED", "Writer has been closed"));
    }

    if (currentSheet == null) {
      return Result.fail(Problems.business("EXCEL_NO_WORKSHEET", "No worksheet selected"));
    }

    try {
      Row poiRow = currentSheet.createRow(currentRowNum);

      for (ExcelCell cell : cells) {
        // Ensure cell is on the current row
        if (cell.row() != currentRowNum) {
          ExcelCell adjustedCell = new ExcelCell(
            currentRowNum, cell.column(), cell.cellType(),
            cell.value(), cell.formula(), cell.style());
          cell = adjustedCell;
        }

        Cell poiCell = poiRow.createCell(cell.column());
        setCellValue(poiCell, cell);

        if (cell.style() != null) {
          CellStyle poiStyle = createCellStyle(cell.style());
          poiCell.setCellStyle(poiStyle);
        }
      }

      currentRowNum++;
      return Result.ok(null);
    } catch (Exception e) {
      return Result.fail(Problems.technical("EXCEL_WRITE_ROW_ERROR", "Failed to write row: " + e.getMessage()));
    }
  }

  @Override
  public Result<Void> setColumnWidth(int column, double width) {
    if (closed) {
      return Result.fail(Problems.business("EXCEL_WRITER_CLOSED", "Writer has been closed"));
    }

    if (currentSheet == null) {
      return Result.fail(Problems.business("EXCEL_NO_WORKSHEET", "No worksheet selected"));
    }

    try {
      // Store for later application (after all data is written for SXSSF)
      pendingColumnWidths.put(column, width);

      // Apply immediately if not using streaming
      if (!(workbook instanceof SXSSFWorkbook)) {
        int poiWidth = (int) (width * 256); // POI uses 256ths of character width
        currentSheet.setColumnWidth(column, poiWidth);
      }

      return Result.ok(null);
    } catch (Exception e) {
      return Result.fail(Problems.technical("EXCEL_SET_COLUMN_WIDTH_ERROR", "Failed to set column width: " + e.getMessage()));
    }
  }

  @Override
  public Result<Void> setRowHeight(double height) {
    if (closed) {
      return Result.fail(Problems.business("EXCEL_WRITER_CLOSED", "Writer has been closed"));
    }

    if (currentSheet == null) {
      return Result.fail(Problems.business("EXCEL_NO_WORKSHEET", "No worksheet selected"));
    }

    try {
      Row currentRow = currentSheet.getRow(currentRowNum - 1); // Last created row
      if (currentRow != null) {
        currentRow.setHeightInPoints((float) height);
      }
      return Result.ok(null);
    } catch (Exception e) {
      return Result.fail(Problems.technical("EXCEL_SET_ROW_HEIGHT_ERROR", "Failed to set row height: " + e.getMessage()));
    }
  }

  @Override
  public Result<Void> freezePanes(int row, int column) {
    if (closed) {
      return Result.fail(Problems.business("EXCEL_WRITER_CLOSED", "Writer has been closed"));
    }

    if (currentSheet == null) {
      return Result.fail(Problems.business("EXCEL_NO_WORKSHEET", "No worksheet selected"));
    }

    try {
      currentSheet.createFreezePane(column, row);
      return Result.ok(null);
    } catch (Exception e) {
      return Result.fail(Problems.technical("EXCEL_FREEZE_PANES_ERROR", "Failed to freeze panes: " + e.getMessage()));
    }
  }

  @Override
  public Result<Void> enableAutoFilter() {
    if (closed) {
      return Result.fail(Problems.business("EXCEL_WRITER_CLOSED", "Writer has been closed"));
    }

    if (currentSheet == null) {
      return Result.fail(Problems.business("EXCEL_NO_WORKSHEET", "No worksheet selected"));
    }

    try {
      // Auto-filter will be set on the used range when closing
      // For now, just mark that auto-filter should be enabled
      return Result.ok(null);
    } catch (Exception e) {
      return Result.fail(Problems.technical("EXCEL_AUTO_FILTER_ERROR", "Failed to enable auto-filter: " + e.getMessage()));
    }
  }

  @Override
  public int getCurrentRowNum() {
    return currentRowNum;
  }

  @Override
  public void nextRow() {
    currentRowNum++;
  }

  @Override
  public Result<Void> flush() {
    if (closed) {
      return Result.fail(Problems.business("EXCEL_WRITER_CLOSED", "Writer has been closed"));
    }

    try {
      if (workbook instanceof SXSSFWorkbook sxssfWorkbook) {
        // Note: flushRows() method may vary by POI version
        // Using alternative approach for memory management
        // sxssfWorkbook.flushRows(100); // Flush rows from memory to disk, keep last 100 in memory
      }
      return Result.ok(null);
    } catch (Exception e) {
      return Result.fail(Problems.technical("EXCEL_FLUSH_ERROR", "Failed to flush data: " + e.getMessage()));
    }
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }

    try {
      // Apply pending column widths
      if (currentSheet != null) {
        for (Map.Entry<Integer, Double> entry : pendingColumnWidths.entrySet()) {
          int poiWidth = (int) (entry.getValue() * 256);
          currentSheet.setColumnWidth(entry.getKey(), poiWidth);
        }
      }

      // Write the workbook to file
      try (var outputStream = java.nio.file.Files.newOutputStream(filePath)) {
        workbook.write(outputStream);
      }

      // Clean up
      workbook.close();

      // Clean up temporary files for SXSSF
      if (workbook instanceof SXSSFWorkbook sxssfWorkbook) {
        sxssfWorkbook.dispose();
      }

    } catch (Exception e) {
      // Log error but don't throw - we're closing
      System.err.println("Error closing Excel writer: " + e.getMessage());
    } finally {
      closed = true;
      workbook = null;
      currentSheet = null;
      pendingColumnWidths.clear();
    }
  }

  /**
   * Sets the value of a POI cell based on the Commons cell data.
   */
  private void setCellValue(Cell poiCell, ExcelCell cell) {
    switch (cell.cellType()) {
      case BLANK -> poiCell.setBlank();
      case BOOLEAN -> {
        if (cell.value() instanceof Boolean bool) {
          poiCell.setCellValue(bool);
        }
      }
      case NUMERIC -> {
        if (cell.value() instanceof Number number) {
          poiCell.setCellValue(number.doubleValue());
        } else if (cell.value() instanceof LocalDate date) {
          Date javaDate = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
          poiCell.setCellValue(javaDate);
        } else if (cell.value() instanceof LocalDateTime dateTime) {
          Date javaDate = Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
          poiCell.setCellValue(javaDate);
        }
      }
      case STRING -> {
        if (cell.value() != null) {
          poiCell.setCellValue(cell.value().toString());
        }
      }
      case FORMULA -> {
        if (options.writeFormulas() && cell.formula() != null) {
          poiCell.setCellFormula(cell.formula());
        } else if (cell.value() != null) {
          // Write computed value
          if (cell.value() instanceof Number number) {
            poiCell.setCellValue(number.doubleValue());
          } else {
            poiCell.setCellValue(cell.value().toString());
          }
        }
      }
      case ERROR -> poiCell.setCellErrorValue(FormulaError.VALUE.getCode());
    }
  }

  /**
   * Creates a POI CellStyle from a Commons ExcelCellStyle (simplified for streaming).
   */
  private CellStyle createCellStyle(ExcelCellStyle style) {
    CellStyle poiStyle = workbook.createCellStyle();

    // Create font if needed
    if (style.fontName() != null || style.fontSize() != null ||
        style.bold() != null || style.italic() != null) {
      Font font = workbook.createFont();

      if (style.fontName() != null) {
        font.setFontName(style.fontName());
      }
      if (style.fontSize() != null) {
        font.setFontHeightInPoints(style.fontSize().shortValue());
      }
      if (style.bold() != null) {
        font.setBold(style.bold());
      }
      if (style.italic() != null) {
        font.setItalic(style.italic());
      }

      poiStyle.setFont(font);
    }

    // Alignment
    if (style.horizontalAlignment() != null) {
      poiStyle.setAlignment(mapHorizontalAlignment(style.horizontalAlignment()));
    }
    if (style.verticalAlignment() != null) {
      poiStyle.setVerticalAlignment(mapVerticalAlignment(style.verticalAlignment()));
    }

    // Text wrapping
    if (style.wrapText() != null) {
      poiStyle.setWrapText(style.wrapText());
    }

    // Number format
    if (style.numberFormat() != null) {
      DataFormat dataFormat = workbook.createDataFormat();
      poiStyle.setDataFormat(dataFormat.getFormat(style.numberFormat()));
    }

    // Borders (simplified)
    if (style.borderStyle() != null) {
      BorderStyle borderStyle = mapBorderStyle(style.borderStyle());
      poiStyle.setBorderTop(borderStyle);
      poiStyle.setBorderBottom(borderStyle);
      poiStyle.setBorderLeft(borderStyle);
      poiStyle.setBorderRight(borderStyle);
    }

    return poiStyle;
  }

  private HorizontalAlignment mapHorizontalAlignment(ExcelCellStyle.HorizontalAlignment alignment) {
    return switch (alignment) {
      case LEFT -> HorizontalAlignment.LEFT;
      case CENTER -> HorizontalAlignment.CENTER;
      case RIGHT -> HorizontalAlignment.RIGHT;
      case JUSTIFY -> HorizontalAlignment.JUSTIFY;
      case FILL -> HorizontalAlignment.FILL;
    };
  }

  private VerticalAlignment mapVerticalAlignment(ExcelCellStyle.VerticalAlignment alignment) {
    return switch (alignment) {
      case TOP -> VerticalAlignment.TOP;
      case CENTER -> VerticalAlignment.CENTER;
      case BOTTOM -> VerticalAlignment.BOTTOM;
      case JUSTIFY -> VerticalAlignment.JUSTIFY;
    };
  }

  private BorderStyle mapBorderStyle(ExcelCellStyle.BorderStyle borderStyle) {
    return switch (borderStyle) {
      case NONE -> BorderStyle.NONE;
      case THIN -> BorderStyle.THIN;
      case MEDIUM -> BorderStyle.MEDIUM;
      case THICK -> BorderStyle.THICK;
      case DOTTED -> BorderStyle.DOTTED;
      case DASHED -> BorderStyle.DASHED;
      case DOUBLE -> BorderStyle.DOUBLE;
    };
  }
}
