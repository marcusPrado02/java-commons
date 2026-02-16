package com.marcusprado02.commons.ports.excel;

import com.marcusprado02.commons.kernel.result.Result;

/**
 * Streaming writer for creating large Excel files row by row.
 *
 * <p>Allows memory-efficient writing of large Excel files without keeping the entire workbook in
 * memory.
 *
 * @since 0.1.0
 */
public interface ExcelStreamWriter extends AutoCloseable {

  /**
   * Creates a new worksheet.
   *
   * @param name worksheet name
   * @return result indicating success or error
   */
  Result<Void> createWorksheet(String name);

  /**
   * Switches to an existing worksheet for writing.
   *
   * @param name worksheet name
   * @return result indicating success or error
   */
  Result<Void> selectWorksheet(String name);

  /**
   * Writes a row of cells.
   *
   * @param cells cells to write
   * @return result indicating success or error
   */
  Result<Void> writeRow(java.util.List<ExcelCell> cells);

  /**
   * Writes a row of values (auto-converted to cells).
   *
   * @param values values to write
   * @return result indicating success or error
   */
  default Result<Void> writeRow(Object... values) {
    var cells = new java.util.ArrayList<ExcelCell>();
    int column = 0;
    for (Object value : values) {
      cells.add(createCell(getCurrentRowNum(), column++, value));
    }
    return writeRow(cells);
  }

  /**
   * Sets column width for the active worksheet.
   *
   * @param column column index (0-based)
   * @param width column width
   * @return result indicating success or error
   */
  Result<Void> setColumnWidth(int column, double width);

  /**
   * Sets row height for the current row.
   *
   * @param height row height
   * @return result indicating success or error
   */
  Result<Void> setRowHeight(double height);

  /**
   * Freezes panes at the specified position.
   *
   * @param row row to freeze (0-based)
   * @param column column to freeze (0-based)
   * @return result indicating success or error
   */
  Result<Void> freezePanes(int row, int column);

  /**
   * Enables auto-filter for the current worksheet.
   *
   * @return result indicating success or error
   */
  Result<Void> enableAutoFilter();

  /**
   * Gets the current row number for writing.
   *
   * @return current row index (0-based)
   */
  int getCurrentRowNum();

  /**
   * Moves to the next row for writing.
   */
  void nextRow();

  /**
   * Flushes any buffered data to the output.
   *
   * @return result indicating success or error
   */
  Result<Void> flush();

  /**
   * Closes the writer and finalizes the Excel file.
   */
  @Override
  void close();

  /**
   * Helper method to create a cell from a value.
   */
  private ExcelCell createCell(int row, int column, Object value) {
    return switch (value) {
      case null -> ExcelCell.blank(row, column);
      case Boolean b -> ExcelCell.bool(row, column, b);
      case Number n -> ExcelCell.number(row, column, n);
      case String s -> ExcelCell.text(row, column, s);
      default -> ExcelCell.text(row, column, value.toString());
    };
  }
}
