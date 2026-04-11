package com.marcusprado02.commons.ports.excel;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Represents an Excel worksheet with cells, formatting, and metadata.
 *
 * <p>Immutable collection of cells with support for reading, writing, and formatting Excel data.
 *
 * @param name worksheet name
 * @param cells map of cell addresses to cells
 * @param columnWidths map of column indices to widths
 * @param rowHeights map of row indices to heights
 * @param frozenRows number of frozen rows from top
 * @param frozenColumns number of frozen columns from left
 * @param autoFilter whether auto-filter is enabled
 * @param printArea print area range (e.g., "A1:G20")
 * @since 0.1.0
 */
public record ExcelWorksheet(
    String name,
    Map<String, ExcelCell> cells,
    Map<Integer, Double> columnWidths,
    Map<Integer, Double> rowHeights,
    int frozenRows,
    int frozenColumns,
    boolean autoFilter,
    String printArea) {

  /** Validates worksheet fields and creates defensive copies. */
  public ExcelWorksheet {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Worksheet name cannot be null or empty");
    }
    cells = cells == null ? Map.of() : Map.copyOf(cells);
    columnWidths = columnWidths == null ? Map.of() : Map.copyOf(columnWidths);
    rowHeights = rowHeights == null ? Map.of() : Map.copyOf(rowHeights);
  }

  /**
   * Gets a cell by row and column indices.
   *
   * @param row 0-based row index
   * @param column 0-based column index
   * @return cell or null if not found
   */
  public ExcelCell getCell(int row, int column) {
    String address = ExcelCell.columnToLetter(column) + (row + 1);
    return cells.get(address);
  }

  /**
   * Gets a cell by address (e.g., "A1", "B5").
   *
   * @param address cell address in A1 notation
   * @return cell or null if not found
   */
  public ExcelCell getCell(String address) {
    return cells.get(address.toUpperCase(Locale.ROOT));
  }

  /**
   * Gets all cells in the worksheet.
   *
   * @return collection of all cells
   */
  public Collection<ExcelCell> getAllCells() {
    return cells.values();
  }

  /**
   * Gets cells in a specific row.
   *
   * @param row 0-based row index
   * @return list of cells in the row, sorted by column
   */
  public List<ExcelCell> getRow(int row) {
    return cells.values().stream()
        .filter(cell -> cell.row() == row)
        .sorted(Comparator.comparing(ExcelCell::column))
        .toList();
  }

  /**
   * Gets cells in a specific column.
   *
   * @param column 0-based column index
   * @return list of cells in the column, sorted by row
   */
  public List<ExcelCell> getColumn(int column) {
    return cells.values().stream()
        .filter(cell -> cell.column() == column)
        .sorted(Comparator.comparing(ExcelCell::row))
        .toList();
  }

  /**
   * Gets the maximum row index with data.
   *
   * @return last row index or -1 if no data
   */
  public int getLastRowNum() {
    return cells.values().stream().mapToInt(ExcelCell::row).max().orElse(-1);
  }

  /**
   * Gets the maximum column index with data.
   *
   * @return last column index or -1 if no data
   */
  public int getLastColumnNum() {
    return cells.values().stream().mapToInt(ExcelCell::column).max().orElse(-1);
  }

  /**
   * Gets the used range of the worksheet (e.g., "A1:G20").
   *
   * @return used range or null if empty
   */
  public String getUsedRange() {
    if (cells.isEmpty()) {
      return null;
    }

    int minRow = cells.values().stream().mapToInt(ExcelCell::row).min().orElse(0);
    int maxRow = getLastRowNum();
    int minColumn = cells.values().stream().mapToInt(ExcelCell::column).min().orElse(0);
    int maxColumn = getLastColumnNum();

    String startCell = ExcelCell.columnToLetter(minColumn) + (minRow + 1);
    String endCell = ExcelCell.columnToLetter(maxColumn) + (maxRow + 1);

    return startCell + ":" + endCell;
  }

  /**
   * Checks if the worksheet is empty.
   *
   * @return true if no cells with data
   */
  public boolean isEmpty() {
    return cells.isEmpty() || cells.values().stream().allMatch(ExcelCell::isEmpty);
  }

  /**
   * Gets the number of cells with data.
   *
   * @return count of non-empty cells
   */
  public int getCellCount() {
    return (int) cells.values().stream().filter(cell -> !cell.isEmpty()).count();
  }

  /** Creates a new builder for ExcelWorksheet. */
  public static Builder builder(String name) {
    return new Builder(name);
  }

  /** Builder for ExcelWorksheet. */
  public static class Builder {
    private final String name;
    private Map<String, ExcelCell> cells = new HashMap<>();
    private Map<Integer, Double> columnWidths = new HashMap<>();
    private Map<Integer, Double> rowHeights = new HashMap<>();
    private int frozenRows = 0;
    private int frozenColumns = 0;
    private boolean autoFilter = false;
    private String printArea;

    public Builder(String name) {
      this.name = name;
    }

    /**
     * Adds a cell to the worksheet.
     *
     * @param cell the cell to add
     * @return this builder
     */
    public Builder cell(ExcelCell cell) {
      this.cells.put(cell.getAddress(), cell);
      return this;
    }

    /**
     * Adds a cell with a value, inferring the cell type from the value.
     *
     * @param row 0-based row index
     * @param column 0-based column index
     * @param value the cell value
     * @return this builder
     */
    @SuppressWarnings("checkstyle:indentation")
    public Builder cell(int row, int column, Object value) {
      var cell =
          switch (value) {
            case null -> ExcelCell.blank(row, column);
            case Boolean b -> ExcelCell.bool(row, column, b);
            case Number n -> ExcelCell.number(row, column, n);
            case String s -> ExcelCell.text(row, column, s);
            default -> ExcelCell.text(row, column, value.toString());
          };
      return cell(cell);
    }

    /**
     * Adds multiple cells to the worksheet.
     *
     * @param cells collection of cells to add
     * @return this builder
     */
    public Builder cells(Collection<ExcelCell> cells) {
      for (ExcelCell cell : cells) {
        this.cells.put(cell.getAddress(), cell);
      }
      return this;
    }

    /**
     * Sets the width of a column.
     *
     * @param column 0-based column index
     * @param width column width
     * @return this builder
     */
    public Builder columnWidth(int column, double width) {
      this.columnWidths.put(column, width);
      return this;
    }

    /**
     * Sets the height of a row.
     *
     * @param row 0-based row index
     * @param height row height
     * @return this builder
     */
    public Builder rowHeight(int row, double height) {
      this.rowHeights.put(row, height);
      return this;
    }

    /** Executes the freezePanes operation. */
    public Builder freezePanes(int rows, int columns) {
      this.frozenRows = rows;
      this.frozenColumns = columns;
      return this;
    }

    public Builder autoFilter(boolean autoFilter) {
      this.autoFilter = autoFilter;
      return this;
    }

    public Builder printArea(String printArea) {
      this.printArea = printArea;
      return this;
    }

    public ExcelWorksheet build() {
      return new ExcelWorksheet(
          name, cells, columnWidths, rowHeights, frozenRows, frozenColumns, autoFilter, printArea);
    }
  }
}
