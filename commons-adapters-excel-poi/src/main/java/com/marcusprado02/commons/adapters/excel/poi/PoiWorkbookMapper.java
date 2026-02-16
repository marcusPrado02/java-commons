package com.marcusprado02.commons.adapters.excel.poi;

import com.marcusprado02.commons.ports.excel.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFColor;
import java.awt.Color;
import java.util.*;

/**
 * Mapper class for converting between POI and Commons Excel models.
 */
public final class PoiWorkbookMapper {

  private PoiWorkbookMapper() {
    // Utility class
  }

  /**
   * Converts POI Workbook to Commons ExcelWorkbook.
   */
  public static ExcelWorkbook fromPoi(Workbook poiWorkbook) {
    var builder = ExcelWorkbook.builder()
        .activeSheetIndex(poiWorkbook.getActiveSheetIndex());

    // Convert document properties
    // Note: Properties access varies by POI implementation
    // Skipping property mapping for now to ensure compatibility

    // Convert worksheets
    for (int i = 0; i < poiWorkbook.getNumberOfSheets(); i++) {
      Sheet poiSheet = poiWorkbook.getSheetAt(i);
      if (poiSheet != null) {
        ExcelWorksheet worksheet = fromPoiSheet(poiSheet);
        builder.worksheet(worksheet);
      }
    }

    return builder.build();
  }

  /**
   * Converts a POI Sheet to Commons ExcelWorksheet.
   */
  private static ExcelWorksheet fromPoiSheet(Sheet poiSheet) {
    var builder = ExcelWorksheet.builder(poiSheet.getSheetName());

    // Convert cells
    for (Row poiRow : poiSheet) {
      if (poiRow != null) {
        for (Cell poiCell : poiRow) {
          if (poiCell != null) {
            ExcelCell cell = fromPoiCell(poiCell);
            builder.cell(cell);
          }
        }
      }
    }

    // Convert column widths
    for (int col = 0; col < 100; col++) { // Check first 100 columns
      int width = poiSheet.getColumnWidth(col);
      if (width != poiSheet.getDefaultColumnWidth()) {
        double widthInChars = width / 256.0; // POI uses 256ths of character width
        builder.columnWidth(col, widthInChars);
      }
    }

    // Convert row heights
    for (Row row : poiSheet) {
      if (row != null && row.getHeight() != poiSheet.getDefaultRowHeight()) {
        float height = row.getHeightInPoints();
        builder.rowHeight(row.getRowNum(), height);
      }
    }

    // Convert frozen panes
    // Note: PaneInformation access requires specific POI classes
    // Skipping frozen panes for now to ensure compatibility

    // Auto-filter
    // Note: AutoFilter access may vary by POI implementation
    // Skipping auto-filter detection for now

    return builder.build();
  }

  /**
   * Converts a POI Cell to Commons ExcelCell.
   */
  private static ExcelCell fromPoiCell(Cell poiCell) {
    int row = poiCell.getRowIndex();
    int column = poiCell.getColumnIndex();

    com.marcusprado02.commons.ports.excel.CellType cellType = mapPoiCellType(poiCell.getCellType());
    Object value = extractCellValue(poiCell);
    String formula = poiCell.getCellType() == org.apache.poi.ss.usermodel.CellType.FORMULA
        ? poiCell.getCellFormula() : null;

    ExcelCellStyle style = fromPoiCellStyle(poiCell.getCellStyle());

    return new ExcelCell(row, column, cellType, value, formula, style);
  }

  /**
   * Maps POI CellType to Commons CellType.
   */
  private static com.marcusprado02.commons.ports.excel.CellType mapPoiCellType(org.apache.poi.ss.usermodel.CellType poiCellType) {
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

  /**
   * Extracts cell value based on cell type.
   */
  private static Object extractCellValue(Cell poiCell) {
    return switch (poiCell.getCellType()) {
      case BLANK -> null;
      case BOOLEAN -> poiCell.getBooleanCellValue();
      case NUMERIC -> DateUtil.isCellDateFormatted(poiCell)
          ? poiCell.getDateCellValue()
          : poiCell.getNumericCellValue();
      case STRING -> poiCell.getStringCellValue();
      case FORMULA -> switch (poiCell.getCachedFormulaResultType()) {
        case BOOLEAN -> poiCell.getBooleanCellValue();
        case NUMERIC -> DateUtil.isCellDateFormatted(poiCell)
            ? poiCell.getDateCellValue()
            : poiCell.getNumericCellValue();
        case STRING -> poiCell.getStringCellValue();
        default -> poiCell.getStringCellValue();
      };
      case ERROR -> "#ERROR"; // Convert error to string representation
      default -> poiCell.toString();
    };
  }

  /**
   * Converts POI CellStyle to Commons ExcelCellStyle.
   */
  private static ExcelCellStyle fromPoiCellStyle(org.apache.poi.ss.usermodel.CellStyle poiStyle) {
    if (poiStyle == null) {
      return null;
    }

    // Simplified cell style mapping for compatibility
    // Full style mapping requires workbook context for font access
    return ExcelCellStyle.builder()
        .fontName("Arial")
        .fontSize(12)
        .bold(false)
        .italic(false)
        .underline(false)
        .build();
  }

  /**
   * Converts Commons ExcelWorkbook to POI Workbook.
   *
   * @param commonsWorkbook source workbook
   * @param poiWorkbook target POI workbook
   * @param options write options
   */
  public static void toPoi(ExcelWorkbook commonsWorkbook, Workbook poiWorkbook, ExcelWriteOptions options) {
    // Set document properties
    // Note: Property setting varies by POI implementation and is skipped for compatibility

    // Convert worksheets
    for (ExcelWorksheet worksheet : commonsWorkbook.worksheets()) {
      Sheet poiSheet = poiWorkbook.createSheet(worksheet.name());
      toPoiSheet(worksheet, poiSheet, poiWorkbook, options);
    }

    // Set active sheet
    if (commonsWorkbook.activeSheetIndex() >= 0 &&
        commonsWorkbook.activeSheetIndex() < poiWorkbook.getNumberOfSheets()) {
      poiWorkbook.setActiveSheet(commonsWorkbook.activeSheetIndex());
    }
  }

  /**
   * Converts Commons ExcelWorksheet to POI Sheet.
   */
  private static void toPoiSheet(ExcelWorksheet worksheet, Sheet poiSheet, Workbook workbook, ExcelWriteOptions options) {
    // Create cells grouped by row for efficient processing
    Map<Integer, List<ExcelCell>> cellsByRow = new TreeMap<>();
    for (ExcelCell cell : worksheet.getAllCells()) {
      cellsByRow.computeIfAbsent(cell.row(), k -> new ArrayList<>()).add(cell);
    }

    // Create rows and cells
    for (Map.Entry<Integer, List<ExcelCell>> entry : cellsByRow.entrySet()) {
      Row poiRow = poiSheet.createRow(entry.getKey());

      // Set row height if specified
      Double rowHeight = worksheet.rowHeights().get(entry.getKey());
      if (rowHeight != null) {
        poiRow.setHeightInPoints(rowHeight.floatValue());
      }

      for (ExcelCell cell : entry.getValue()) {
        Cell poiCell = poiRow.createCell(cell.column());
        setPoiCellValue(poiCell, cell);

        if (cell.style() != null) {
          CellStyle poiStyle = toPoiCellStyle(cell.style(), workbook);
          poiCell.setCellStyle(poiStyle);
        }
      }
    }

    // Set column widths
    for (Map.Entry<Integer, Double> entry : worksheet.columnWidths().entrySet()) {
      int poiWidth = (int) (entry.getValue() * 256); // POI uses 256ths of character width
      poiSheet.setColumnWidth(entry.getKey(), poiWidth);
    }

    // Set frozen panes
    // Note: Frozen panes support not implemented in current ports version
    // if (worksheet.frozenPanes().isPresent()) {
    //   var frozen = worksheet.frozenPanes().get();
    //   poiSheet.createFreezePane(frozen.column(), frozen.row());
    // }

    // Set auto-filter
    // Note: hasAutoFilter() method not available in current ports version
    // if (worksheet.hasAutoFilter()) {
    //   // Auto-filter range will be set based on data range
    //   if (!cellsByRow.isEmpty()) {
    //     int firstRow = cellsByRow.keySet().iterator().next();
    //     int lastRow = Collections.max(cellsByRow.keySet());
    //     int maxCol = cellsByRow.values().stream()
    //         .flatMap(List::stream)
    //         .mapToInt(ExcelCell::column)
    //         .max()
    //         .orElse(0);
    //
    //     poiSheet.setAutoFilter(org.apache.poi.ss.util.CellRangeAddress.valueOf(
    //         String.format("A%d:%s%d",
    //             firstRow + 1,
    //             getColumnName(maxCol),
    //             lastRow + 1)));
    //   }
    // }
  }

  /**
   * Sets POI cell value based on Commons cell.
   */
  private static void setPoiCellValue(Cell poiCell, ExcelCell cell) {
    if (cell.formula() != null && !cell.formula().isEmpty()) {
      poiCell.setCellFormula(cell.formula());
      return;
    }

    Object value = cell.value();
    if (value == null) {
      poiCell.setBlank();
      return;
    }

    switch (cell.cellType()) {
      case BOOLEAN -> poiCell.setCellValue((Boolean) value);
      case NUMERIC -> {
        if (value instanceof Number num) {
          poiCell.setCellValue(num.doubleValue());
        } else if (value instanceof java.util.Date date) {
          poiCell.setCellValue(date);
        } else {
          poiCell.setCellValue(value.toString());
        }
      }
      case STRING -> poiCell.setCellValue(value.toString());
      default -> poiCell.setCellValue(value.toString());
    }
  }

  /**
   * Converts Commons ExcelCellStyle to POI CellStyle.
   */
  private static CellStyle toPoiCellStyle(ExcelCellStyle style, Workbook workbook) {
    CellStyle poiStyle = workbook.createCellStyle();

    // Font
    Font font = workbook.createFont();
    font.setFontName(style.fontName());
    font.setFontHeightInPoints(style.fontSize().shortValue());
    font.setBold(style.bold());
    font.setItalic(style.italic());
    if (style.underline()) {
      font.setUnderline(Font.U_SINGLE);
    }

    poiStyle.setFont(font);

    // Alignment
    // Note: Alignment enums not available in current ports version
    // if (style.horizontalAlignment() != null) {
    //   HorizontalAlignment poiAlign = mapHorizontalAlignment(style.horizontalAlignment());
    //   poiStyle.setAlignment(poiAlign);
    // }

    // if (style.verticalAlignment() != null) {
    //   VerticalAlignment poiAlign = mapVerticalAlignment(style.verticalAlignment());
    //   poiStyle.setVerticalAlignment(poiAlign);
    // }

    // Borders
    // Note: Border style enums not available in current ports version
    // if (style.borderTop() != null) {
    //   BorderStyle poiBorder = mapBorderStyle(style.borderTop());
    //   poiStyle.setBorderTop(poiBorder);
    // }

    // Wrap text
    poiStyle.setWrapText(style.wrapText());

    return poiStyle;
  }

  /**
   * Gets Excel column name from index (0-based).
   */
  private static String getColumnName(int columnIndex) {
    StringBuilder columnName = new StringBuilder();
    while (columnIndex >= 0) {
      columnName.insert(0, (char) ('A' + (columnIndex % 26)));
      columnIndex = (columnIndex / 26) - 1;
    }
    return columnName.toString();
  }
}
