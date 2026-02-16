package com.marcusprado02.commons.ports.excel;

import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.kernel.errors.Problems;

/**
 * Streaming reader for processing large Excel files row by row.
 *
 * <p>Allows memory-efficient reading of large Excel files without loading the entire workbook into
 * memory.
 *
 * @since 0.1.0
 */
public interface ExcelStreamReader extends AutoCloseable {

  /**
   * Gets available worksheets in the file.
   *
   * @return result containing sheet names or error
   */
  Result<java.util.List<String>> getWorksheetNames();

  /**
   * Starts reading a specific worksheet.
   *
   * @param worksheetName name of worksheet to read
   * @return result indicating success or error
   */
  Result<Void> selectWorksheet(String worksheetName);

  /**
   * Starts reading the first/active worksheet.
   *
   * @return result indicating success or error
   */
  default Result<Void> selectFirstWorksheet() {
    return getWorksheetNames()
        .flatMap(
            names ->
                names.isEmpty()
                    ? Result.fail(Problems.business("EXCEL_NO_WORKSHEETS", "No worksheets found"))
                    : selectWorksheet(names.get(0)));
  }

  /**
   * Checks if there are more rows to read.
   *
   * @return true if more rows are available
   */
  boolean hasNext();

  /**
   * Reads the next row of cells.
   *
   * @return result containing row cells or error
   */
  Result<java.util.List<ExcelCell>> readNext();

  /**
   * Gets the current row number (0-based).
   *
   * @return current row index
   */
  int getCurrentRowNum();

  /**
   * Skips the specified number of rows.
   *
   * @param count number of rows to skip
   * @return result indicating success or error
   */
  Result<Void> skip(int count);

  /**
   * Closes the reader and releases resources.
   */
  @Override
  void close();
}
