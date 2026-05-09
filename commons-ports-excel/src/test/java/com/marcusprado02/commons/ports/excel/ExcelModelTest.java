package com.marcusprado02.commons.ports.excel;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ExcelModelTest {

  // --- CsvOptions ---

  @Test
  void csvOptions_defaults() {
    CsvOptions opts = CsvOptions.defaults();
    assertEquals(',', opts.delimiter());
    assertEquals('"', opts.quote());
    assertTrue(opts.includeHeaders());
    assertEquals("UTF-8", opts.encoding());
    assertTrue(opts.skipEmptyLines());
  }

  @Test
  void csvOptions_semicolon() {
    assertEquals(';', CsvOptions.semicolon().delimiter());
  }

  @Test
  void csvOptions_tab() {
    assertEquals('\t', CsvOptions.tab().delimiter());
  }

  @Test
  void csvOptions_pipe() {
    assertEquals('|', CsvOptions.pipe().delimiter());
  }

  @Test
  void csvOptions_noHeaders() {
    assertFalse(CsvOptions.noHeaders().includeHeaders());
  }

  @Test
  void csvOptions_builder_all_fields() {
    CsvOptions opts =
        CsvOptions.builder()
            .delimiter(';')
            .quote('\'')
            .escape('/')
            .includeHeaders(false)
            .encoding("ISO-8859-1")
            .lineSeparator("\r\n")
            .nullValue("NULL")
            .dateFormat("dd/MM/yyyy")
            .numberFormat("#,##0.00")
            .skipEmptyLines(false)
            .build();
    assertEquals(';', opts.delimiter());
    assertEquals('\'', opts.quote());
    assertFalse(opts.includeHeaders());
    assertEquals("ISO-8859-1", opts.encoding());
  }

  // --- ExcelReadOptions ---

  @Test
  void excelReadOptions_defaults() {
    ExcelReadOptions opts = ExcelReadOptions.defaults();
    assertFalse(opts.readAllSheets());
    assertFalse(opts.readFormulas());
    assertEquals(0, opts.maxRows());
  }

  @Test
  void excelReadOptions_allSheets() {
    assertTrue(ExcelReadOptions.allSheets().readAllSheets());
  }

  @Test
  void excelReadOptions_withFormulas() {
    assertTrue(ExcelReadOptions.withFormulas().readFormulas());
  }

  @Test
  void excelReadOptions_streaming() {
    assertTrue(ExcelReadOptions.streaming().skipBlankRows());
  }

  @Test
  void excelReadOptions_negative_maxRows_throws() {
    assertThrows(
        IllegalArgumentException.class, () -> ExcelReadOptions.builder().maxRows(-1).build());
  }

  @Test
  void excelReadOptions_negative_maxColumns_throws() {
    assertThrows(
        IllegalArgumentException.class, () -> ExcelReadOptions.builder().maxColumns(-1).build());
  }

  @Test
  void excelReadOptions_builder_all_fields() {
    ExcelReadOptions opts =
        ExcelReadOptions.builder()
            .readAllSheets(true)
            .readFormulas(true)
            .readComments(true)
            .readHiddenSheets(true)
            .password("secret")
            .maxRows(100)
            .maxColumns(50)
            .skipBlankRows(true)
            .dateFormat("dd-MM-yyyy")
            .build();
    assertTrue(opts.readComments());
    assertEquals(100, opts.maxRows());
    assertEquals("dd-MM-yyyy", opts.dateFormat());
  }

  // --- ExcelWriteOptions ---

  @Test
  void excelWriteOptions_defaults() {
    ExcelWriteOptions opts = ExcelWriteOptions.defaults();
    assertEquals(ExcelWriteOptions.ExcelFormat.XLSX, opts.format());
    assertTrue(opts.compress());
    assertNull(opts.password());
  }

  @Test
  void excelWriteOptions_xls() {
    assertEquals(ExcelWriteOptions.ExcelFormat.XLS, ExcelWriteOptions.xls().format());
  }

  @Test
  void excelWriteOptions_encrypted() {
    assertEquals("secret", ExcelWriteOptions.encrypted("secret").password());
  }

  @Test
  void excelWriteOptions_performance() {
    ExcelWriteOptions opts = ExcelWriteOptions.performance();
    assertFalse(opts.compress());
    assertFalse(opts.writeFormulas());
    assertFalse(opts.autoSizeColumns());
  }

  @Test
  void excelWriteOptions_builder_all_fields() {
    ExcelWriteOptions opts =
        ExcelWriteOptions.builder()
            .format(ExcelWriteOptions.ExcelFormat.XLS)
            .compress(false)
            .password("pw")
            .writeFormulas(false)
            .writeComments(false)
            .autoSizeColumns(true)
            .lockStructure(true)
            .removePersonalInfo(true)
            .creator("author")
            .build();
    assertFalse(opts.compress());
    assertTrue(opts.autoSizeColumns());
    assertTrue(opts.lockStructure());
    assertEquals("author", opts.creator());
  }

  // --- ExcelValidationResult ---

  @Test
  void excelValidationResult_valid_factory() {
    ExcelValidationResult r = ExcelValidationResult.valid("XLSX", 3);
    assertTrue(r.isValid());
    assertEquals("XLSX", r.format());
    assertEquals(3, r.worksheetCount());
    assertFalse(r.hasWarnings());
    assertFalse(r.hasErrors());
  }

  @Test
  void excelValidationResult_invalid_with_list() {
    ExcelValidationResult r = ExcelValidationResult.invalid(List.of("err1", "err2"));
    assertFalse(r.isValid());
    assertTrue(r.hasErrors());
    assertEquals(2, r.errors().size());
  }

  @Test
  void excelValidationResult_invalid_with_single_error() {
    ExcelValidationResult r = ExcelValidationResult.invalid("single error");
    assertFalse(r.isValid());
    assertEquals(1, r.errors().size());
  }

  @Test
  void excelValidationResult_null_warnings_defaults_to_empty() {
    ExcelValidationResult r =
        new ExcelValidationResult(true, "XLSX", "2016", 1, false, false, false, null, null);
    assertNotNull(r.warnings());
    assertTrue(r.warnings().isEmpty());
  }

  // --- ExcelWorkbook ---

  @Test
  void excelWorkbook_builder_empty() {
    ExcelWorkbook wb = ExcelWorkbook.builder().build();
    assertEquals(0, wb.getWorksheetCount());
    assertTrue(wb.isEmpty());
    assertEquals(0, wb.getTotalCellCount());
    assertTrue(wb.getWorksheetNames().isEmpty());
  }

  @Test
  void excelWorkbook_builder_with_worksheets() {
    ExcelWorksheet ws1 = ExcelWorksheet.builder("Sheet1").build();
    ExcelWorksheet ws2 = ExcelWorksheet.builder("Sheet2").build();
    ExcelWorkbook wb = ExcelWorkbook.builder().worksheet(ws1).worksheet(ws2).build();
    assertEquals(2, wb.getWorksheetCount());
    assertEquals(ws1, wb.getWorksheet(0));
    assertEquals(ws1, wb.getWorksheet("Sheet1"));
    assertNull(wb.getWorksheet("Missing"));
    assertNull(wb.getWorksheet(99));
    assertNotNull(wb.getActiveWorksheet());
    assertEquals(List.of("Sheet1", "Sheet2"), wb.getWorksheetNames());
  }

  @Test
  void excelWorkbook_invalid_active_index_throws() {
    ExcelWorksheet ws = ExcelWorksheet.builder("S").build();
    assertThrows(
        IllegalArgumentException.class,
        () -> ExcelWorkbook.builder().worksheet(ws).activeSheetIndex(5).build());
  }

  @Test
  void excelWorkbook_null_properties_defaults_to_empty() {
    ExcelWorkbook wb = new ExcelWorkbook(null, 0, null, "author", "title", "subject", "comments");
    assertTrue(wb.properties().isEmpty());
    assertTrue(wb.worksheets().isEmpty());
  }

  @Test
  void excelWorkbook_builder_property_and_metadata() {
    ExcelWorkbook wb =
        ExcelWorkbook.builder()
            .property("key", "val")
            .properties(Map.of("k2", "v2"))
            .author("me")
            .title("My Doc")
            .subject("Testing")
            .comments("A test")
            .build();
    assertEquals("val", wb.properties().get("key"));
    assertEquals("me", wb.author());
    assertEquals("My Doc", wb.title());
  }

  // --- ExcelWorksheet ---

  @Test
  void excelWorksheet_builder_basic() {
    ExcelWorksheet ws = ExcelWorksheet.builder("MySheet").build();
    assertEquals("MySheet", ws.name());
    assertTrue(ws.isEmpty());
    assertEquals(-1, ws.getLastRowNum());
    assertEquals(-1, ws.getLastColumnNum());
    assertNull(ws.getUsedRange());
    assertNull(ws.getCell(0, 0));
    assertNull(ws.getCell("A1"));
    assertTrue(ws.getAllCells().isEmpty());
    assertEquals(0, ws.getCellCount());
  }

  @Test
  void excelWorksheet_null_or_empty_name_throws() {
    assertThrows(IllegalArgumentException.class, () -> ExcelWorksheet.builder(null).build());
    assertThrows(IllegalArgumentException.class, () -> ExcelWorksheet.builder("  ").build());
  }

  @Test
  void excelWorksheet_with_cells() {
    ExcelCell cellA1 = ExcelCell.text(0, 0, "Hello");
    ExcelCell cellB2 = ExcelCell.number(1, 1, 42);
    ExcelWorksheet ws = ExcelWorksheet.builder("Data").cell(cellA1).cell(cellB2).build();
    assertFalse(ws.isEmpty());
    assertEquals(2, ws.getCellCount());
    assertEquals(cellA1, ws.getCell(0, 0));
    assertEquals(cellA1, ws.getCell("A1"));
    assertEquals(1, ws.getRow(0).size());
    assertEquals(1, ws.getColumn(0).size());
    assertEquals(1, ws.getLastRowNum());
    assertEquals(1, ws.getLastColumnNum());
    assertNotNull(ws.getUsedRange());
  }

  @Test
  void excelWorksheet_builder_cell_by_value_switch() {
    ExcelWorksheet ws =
        ExcelWorksheet.builder("S")
            .cell(0, 0, null)
            .cell(0, 1, true)
            .cell(0, 2, 42)
            .cell(0, 3, "text")
            .cell(0, 4, List.of("a"))
            .build();
    assertEquals(5, ws.cells().size());
  }

  @Test
  void excelWorksheet_builder_column_row_config() {
    ExcelWorksheet ws =
        ExcelWorksheet.builder("S")
            .columnWidth(0, 20.0)
            .rowHeight(0, 15.0)
            .freezePanes(1, 1)
            .autoFilter(true)
            .printArea("A1:D10")
            .build();
    assertEquals(1, ws.frozenRows());
    assertEquals(1, ws.frozenColumns());
    assertTrue(ws.autoFilter());
    assertEquals("A1:D10", ws.printArea());
  }

  @Test
  void excelWorksheet_builder_cells_collection() {
    ExcelCell c1 = ExcelCell.text(0, 0, "a");
    ExcelCell c2 = ExcelCell.text(0, 1, "b");
    ExcelWorksheet ws = ExcelWorksheet.builder("S").cells(List.of(c1, c2)).build();
    assertEquals(2, ws.cells().size());
  }

  @Test
  void excelWorkbook_with_non_empty_worksheets_not_isEmpty() {
    ExcelCell cell = ExcelCell.text(0, 0, "data");
    ExcelWorksheet ws = ExcelWorksheet.builder("S").cell(cell).build();
    ExcelWorkbook wb = ExcelWorkbook.builder().worksheet(ws).build();
    assertFalse(wb.isEmpty());
    assertEquals(1, wb.getTotalCellCount());
  }

  @Test
  void excelWorkbook_worksheets_list_in_builder() {
    ExcelWorksheet ws1 = ExcelWorksheet.builder("A").build();
    ExcelWorksheet ws2 = ExcelWorksheet.builder("B").build();
    ExcelWorkbook wb = ExcelWorkbook.builder().worksheets(List.of(ws1, ws2)).build();
    assertEquals(2, wb.getWorksheetCount());
  }
}
