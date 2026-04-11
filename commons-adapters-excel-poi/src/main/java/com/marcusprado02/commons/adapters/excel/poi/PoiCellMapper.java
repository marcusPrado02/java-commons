package com.marcusprado02.commons.adapters.excel.poi;

import com.marcusprado02.commons.ports.excel.ExcelCell;
import com.marcusprado02.commons.ports.excel.ExcelCellStyle;
import java.util.Date;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;

/** Helper class for mapping POI cells to Commons cells. */
final class PoiCellMapper {

  private PoiCellMapper() {
    // Utility class
  }

  /** Converts a POI Cell to Commons ExcelCell with simplified style mapping for streaming. */
  public static ExcelCell fromPoi(Cell poiCell) {
    int row = poiCell.getRowIndex();
    int column = poiCell.getColumnIndex();

    com.marcusprado02.commons.ports.excel.CellType cellType = mapCellType(poiCell.getCellType());
    Object value = extractCellValue(poiCell);
    String formula =
        poiCell.getCellType() == org.apache.poi.ss.usermodel.CellType.FORMULA
            ? poiCell.getCellFormula()
            : null;

    // For streaming, we skip detailed style mapping to improve performance
    ExcelCellStyle style = null;

    return new ExcelCell(row, column, cellType, value, formula, style);
  }

  @SuppressWarnings("checkstyle:indentation")
  private static com.marcusprado02.commons.ports.excel.CellType mapCellType(
      org.apache.poi.ss.usermodel.CellType poiCellType) {
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

  @SuppressWarnings("checkstyle:indentation")
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
