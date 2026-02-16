package com.marcusprado02.commons.ports.excel;

/**
 * Types of data that can be stored in Excel cells.
 *
 * @since 0.1.0
 */
public enum CellType {
  /** Blank cell with no data. */
  BLANK,

  /** Boolean value (true/false). */
  BOOLEAN,

  /** Numeric value (integers, decimals, dates). */
  NUMERIC,

  /** String/text value. */
  STRING,

  /** Formula that calculates a value. */
  FORMULA,

  /** Error value (#DIV/0!, #VALUE!, etc.). */
  ERROR
}
