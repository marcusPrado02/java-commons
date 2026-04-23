package com.marcusprado02.commons.ports.excel;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ExcelCellTest {

  @Test
  void blank_cell_is_empty() {
    ExcelCell c = ExcelCell.blank(0, 0);
    assertEquals(CellType.BLANK, c.cellType());
    assertTrue(c.isEmpty());
    assertEquals("", c.getStringValue());
    assertEquals(0.0, c.getNumericValue());
    assertFalse(c.getBooleanValue());
  }

  @Test
  void text_cell_stores_value() {
    ExcelCell c = ExcelCell.text(1, 2, "hello");
    assertEquals("hello", c.getStringValue());
    assertEquals(CellType.STRING, c.cellType());
    assertFalse(c.isEmpty());
    assertEquals("C2", c.getAddress());
  }

  @Test
  void text_cell_with_style() {
    ExcelCellStyle style = ExcelCellStyle.defaultStyle();
    ExcelCell c = ExcelCell.text(0, 0, "val", style);
    assertEquals(style, c.style());
  }

  @Test
  void number_cell_getNumericValue() {
    ExcelCell c = ExcelCell.number(0, 0, 42.5);
    assertEquals(42.5, c.getNumericValue(), 0.001);
    assertEquals("42.5", c.getStringValue());
    assertEquals(CellType.NUMERIC, c.cellType());
  }

  @Test
  void boolean_cell_true() {
    ExcelCell c = ExcelCell.bool(0, 0, Boolean.TRUE);
    assertTrue(c.getBooleanValue());
    assertEquals("true", c.getStringValue());
    assertEquals(1.0, c.getNumericValue());
  }

  @Test
  void boolean_cell_false() {
    ExcelCell c = ExcelCell.bool(0, 0, Boolean.FALSE);
    assertFalse(c.getBooleanValue());
    assertEquals(0.0, c.getNumericValue());
  }

  @Test
  void formula_cell_getStringValue_includes_equals_sign() {
    ExcelCell c = ExcelCell.formula(0, 0, "SUM(A1:A10)");
    assertEquals("=SUM(A1:A10)", c.getStringValue());
    assertEquals(CellType.FORMULA, c.cellType());
    assertTrue(c.isEmpty());
  }

  @Test
  void formula_cell_with_style() {
    ExcelCell c = ExcelCell.formula(0, 1, "A1+B1", null);
    assertNull(c.style());
  }

  @Test
  void date_cell_stores_date() {
    LocalDate date = LocalDate.of(2026, 1, 1);
    ExcelCell c = ExcelCell.date(0, 0, date);
    assertEquals(CellType.NUMERIC, c.cellType());
    assertEquals(date, c.value());
  }

  @Test
  void date_cell_with_style() {
    ExcelCell c = ExcelCell.date(0, 0, LocalDate.now(), null);
    assertNull(c.style());
  }

  @Test
  void datetime_cell_stores_datetime() {
    LocalDateTime dt = LocalDateTime.of(2026, 1, 1, 12, 0);
    ExcelCell c = ExcelCell.dateTime(0, 0, dt);
    assertEquals(dt, c.value());
  }

  @Test
  void datetime_cell_with_style() {
    ExcelCell c = ExcelCell.dateTime(0, 0, LocalDateTime.now(), null);
    assertNull(c.style());
  }

  @Test
  void number_cell_with_style() {
    ExcelCell c = ExcelCell.number(0, 0, 1, null);
    assertNull(c.style());
  }

  @Test
  void column_to_letter_and_back() {
    assertEquals("A", ExcelCell.columnToLetter(0));
    assertEquals("B", ExcelCell.columnToLetter(1));
    assertEquals("Z", ExcelCell.columnToLetter(25));
    assertEquals("AA", ExcelCell.columnToLetter(26));
    assertEquals("AB", ExcelCell.columnToLetter(27));

    assertEquals(0, ExcelCell.letterToColumn("A"));
    assertEquals(25, ExcelCell.letterToColumn("Z"));
    assertEquals(26, ExcelCell.letterToColumn("AA"));
  }

  @Test
  void getAddress_uses_one_based_row() {
    ExcelCell c = ExcelCell.text(0, 0, "x");
    assertEquals("A1", c.getAddress());
    ExcelCell c2 = ExcelCell.text(4, 2, "y");
    assertEquals("C5", c2.getAddress());
  }

  @Test
  void rejects_negative_row() {
    assertThrows(IllegalArgumentException.class, () -> ExcelCell.blank(-1, 0));
  }

  @Test
  void rejects_negative_column() {
    assertThrows(IllegalArgumentException.class, () -> ExcelCell.blank(0, -1));
  }

  @Test
  void rejects_null_cell_type() {
    assertThrows(IllegalArgumentException.class,
        () -> new ExcelCell(0, 0, null, null, null, null));
  }

  @Test
  void string_cell_getBooleanValue_true_string() {
    ExcelCell c = ExcelCell.text(0, 0, "true");
    assertTrue(c.getBooleanValue());
  }

  @Test
  void string_cell_getBooleanValue_non_true_string() {
    ExcelCell c = ExcelCell.text(0, 0, "false");
    assertFalse(c.getBooleanValue());
  }

  @Test
  void getStringValue_formula_null_formula_returns_empty() {
    ExcelCell c = new ExcelCell(0, 0, CellType.FORMULA, null, null, null);
    assertEquals("", c.getStringValue());
  }

  @Test
  void getStringValue_null_value_returns_empty() {
    ExcelCell c = new ExcelCell(0, 0, CellType.STRING, null, null, null);
    assertEquals("", c.getStringValue());
  }

  @Test
  void getNumericValue_non_numeric_non_boolean_returns_zero() {
    ExcelCell c = ExcelCell.blank(0, 0);
    assertEquals(0.0, c.getNumericValue());
  }

  @Test
  void getNumericValue_boolean_null_value_returns_zero() {
    ExcelCell c = new ExcelCell(0, 0, CellType.BOOLEAN, null, null, null);
    assertEquals(0.0, c.getNumericValue());
  }

  @Test
  void getBooleanValue_numeric_zero_is_false() {
    ExcelCell c = ExcelCell.number(0, 0, 0.0);
    assertFalse(c.getBooleanValue());
  }

  @Test
  void getBooleanValue_numeric_nonzero_is_true() {
    ExcelCell c = ExcelCell.number(0, 0, 1.0);
    assertTrue(c.getBooleanValue());
  }

  @Test
  void getStringValue_error_type_returns_empty() {
    ExcelCell c = new ExcelCell(0, 0, CellType.ERROR, null, null, null);
    assertEquals("", c.getStringValue());
  }

  @Test
  void getBooleanValue_default_case_returns_false() {
    ExcelCell c = new ExcelCell(0, 0, CellType.ERROR, null, null, null);
    assertFalse(c.getBooleanValue());
  }
}
