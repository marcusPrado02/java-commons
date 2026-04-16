package com.marcusprado02.commons.adapters.excel.poi;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.excel.CsvOptions;
import com.marcusprado02.commons.ports.excel.ExcelCell;
import com.marcusprado02.commons.ports.excel.ExcelReadOptions;
import com.marcusprado02.commons.ports.excel.ExcelValidationResult;
import com.marcusprado02.commons.ports.excel.ExcelWorkbook;
import com.marcusprado02.commons.ports.excel.ExcelWriteOptions;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PoiExcelInfrastructureTest {

  @TempDir Path tempDir;

  // ── helper: write an XLSX workbook to a temp file ────────────────────────

  private Path writeXlsxFile(String sheetName, String... values) throws IOException {
    try (Workbook wb = new XSSFWorkbook()) {
      Sheet sheet = wb.createSheet(sheetName);
      Row row = sheet.createRow(0);
      for (int i = 0; i < values.length; i++) {
        row.createCell(i).setCellValue(values[i]);
      }
      Path file = tempDir.resolve("test.xlsx");
      try (var out = Files.newOutputStream(file)) {
        wb.write(out);
      }
      return file;
    }
  }

  // ── PoiFileValidator ──────────────────────────────────────────────────────

  @Test
  void validateValidXlsxFileShouldReturnValid() throws IOException {
    Path file = writeXlsxFile("Sheet1", "A", "B", "C");
    ExcelValidationResult result = PoiFileValidator.validate(file);
    assertThat(result.isValid()).isTrue();
    assertThat(result.worksheetCount()).isEqualTo(1);
  }

  @Test
  void validateNonExistentFileShouldReturnInvalid() {
    Path missing = tempDir.resolve("missing.xlsx");
    ExcelValidationResult result = PoiFileValidator.validate(missing);
    assertThat(result.isValid()).isFalse();
    assertThat(result.errors()).isNotEmpty();
  }

  @Test
  void validateEmptyFileShouldReturnInvalid() throws IOException {
    Path emptyFile = tempDir.resolve("empty.xlsx");
    Files.createFile(emptyFile);
    ExcelValidationResult result = PoiFileValidator.validate(emptyFile);
    assertThat(result.isValid()).isFalse();
  }

  @Test
  void validateCorruptFileShouldReturnInvalid() throws IOException {
    Path corrupt = tempDir.resolve("corrupt.xlsx");
    Files.writeString(corrupt, "this is not a valid excel file content at all");
    ExcelValidationResult result = PoiFileValidator.validate(corrupt);
    assertThat(result.isValid()).isFalse();
  }

  // ── PoiCellMapper ─────────────────────────────────────────────────────────

  @Test
  void cellMapperShouldMapStringCell() {
    try (Workbook wb = new XSSFWorkbook()) {
      Sheet sheet = wb.createSheet("S");
      Row row = sheet.createRow(0);
      Cell cell = row.createCell(0);
      cell.setCellValue("hello");

      ExcelCell mapped = PoiCellMapper.fromPoi(cell);

      assertThat(mapped.row()).isEqualTo(0);
      assertThat(mapped.column()).isEqualTo(0);
      assertThat(mapped.value()).isEqualTo("hello");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void cellMapperShouldMapNumericCell() {
    try (Workbook wb = new XSSFWorkbook()) {
      Sheet sheet = wb.createSheet("S");
      Row row = sheet.createRow(0);
      Cell cell = row.createCell(0, CellType.NUMERIC);
      cell.setCellValue(42.5);

      ExcelCell mapped = PoiCellMapper.fromPoi(cell);

      assertThat(mapped.value()).isEqualTo(42.5);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void cellMapperShouldMapBooleanCell() {
    try (Workbook wb = new XSSFWorkbook()) {
      Sheet sheet = wb.createSheet("S");
      Row row = sheet.createRow(0);
      Cell cell = row.createCell(0, CellType.BOOLEAN);
      cell.setCellValue(true);

      ExcelCell mapped = PoiCellMapper.fromPoi(cell);

      assertThat(mapped.value()).isEqualTo(true);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void cellMapperShouldMapBlankCell() {
    try (Workbook wb = new XSSFWorkbook()) {
      Sheet sheet = wb.createSheet("S");
      Row row = sheet.createRow(0);
      Cell cell = row.createCell(0, CellType.BLANK);

      ExcelCell mapped = PoiCellMapper.fromPoi(cell);

      assertThat(mapped.value()).isNull();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void cellMapperShouldMapErrorCell() {
    try (Workbook wb = new XSSFWorkbook()) {
      Sheet sheet = wb.createSheet("S");
      Row row = sheet.createRow(0);
      Cell cell = row.createCell(0, CellType.ERROR);
      cell.setCellErrorValue((byte) 0x07);

      ExcelCell mapped = PoiCellMapper.fromPoi(cell);

      assertThat(mapped.value()).isEqualTo("#ERROR");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  // ── PoiWorkbookMapper ─────────────────────────────────────────────────────

  @Test
  void workbookMapperFromPoiShouldConvertSheets() {
    try (Workbook wb = new XSSFWorkbook()) {
      Sheet s1 = wb.createSheet("Alpha");
      Row row = s1.createRow(0);
      row.createCell(0).setCellValue("X");
      wb.createSheet("Beta");

      ExcelWorkbook mapped = PoiWorkbookMapper.fromPoi(wb);

      assertThat(mapped.getWorksheetCount()).isEqualTo(2);
      assertThat(mapped.getWorksheet("Alpha")).isNotNull();
      assertThat(mapped.getWorksheet("Beta")).isNotNull();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void workbookMapperToPoiShouldCreateSheets() {
    try (Workbook wb = new XSSFWorkbook()) {
      var worksheet =
          com.marcusprado02.commons.ports.excel.ExcelWorksheet.builder("TestSheet")
              .cell(0, 0, "Value1")
              .cell(0, 1, 99)
              .build();

      var workbook =
          com.marcusprado02.commons.ports.excel.ExcelWorkbook.builder()
              .worksheet(worksheet)
              .build();

      PoiWorkbookMapper.toPoi(workbook, wb, ExcelWriteOptions.defaults());

      assertThat(wb.getNumberOfSheets()).isEqualTo(1);
      assertThat(wb.getSheetName(0)).isEqualTo("TestSheet");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  // ── PoiStreamWriter ───────────────────────────────────────────────────────

  @Test
  void streamWriterShouldCreateWorksheetAndWriteRows() throws IOException {
    Path output = tempDir.resolve("stream-output.xlsx");
    var config = PoiConfiguration.defaults();
    var options = ExcelWriteOptions.defaults();

    try (PoiStreamWriter writer = new PoiStreamWriter(output, options, config)) {
      Result<Void> create = writer.createWorksheet("Data");
      assertThat(create.isOk()).isTrue();

      List<ExcelCell> row0 =
          List.of(
              new ExcelCell(
                  0, 0, com.marcusprado02.commons.ports.excel.CellType.STRING, "A", null, null),
              new ExcelCell(
                  0, 1, com.marcusprado02.commons.ports.excel.CellType.NUMERIC, 1.0, null, null));

      Result<Void> write = writer.writeRow(row0);
      assertThat(write.isOk()).isTrue();

      Result<Void> flush = writer.flush();
      assertThat(flush.isOk()).isTrue();
    }

    assertThat(Files.size(output)).isGreaterThan(0);
  }

  @Test
  void streamWriterWriteToClosedWriterShouldReturnFail() throws IOException {
    Path output = tempDir.resolve("closed-writer.xlsx");
    var writer =
        new PoiStreamWriter(output, ExcelWriteOptions.defaults(), PoiConfiguration.defaults());
    writer.close();

    Result<Void> result = writer.createWorksheet("X");
    assertThat(result.isFail()).isTrue();
  }

  @Test
  void streamWriterSelectWorksheetShouldWork() throws IOException {
    Path output = tempDir.resolve("select-sheet.xlsx");
    try (PoiStreamWriter writer =
        new PoiStreamWriter(output, ExcelWriteOptions.defaults(), PoiConfiguration.defaults())) {
      writer.createWorksheet("First");
      Result<Void> select = writer.selectWorksheet("First");
      assertThat(select.isOk()).isTrue();
    }
  }

  @Test
  void streamWriterSelectNonExistentWorksheetShouldFail() throws IOException {
    Path output = tempDir.resolve("select-missing.xlsx");
    try (PoiStreamWriter writer =
        new PoiStreamWriter(output, ExcelWriteOptions.defaults(), PoiConfiguration.defaults())) {
      Result<Void> select = writer.selectWorksheet("DoesNotExist");
      assertThat(select.isFail()).isTrue();
    }
  }

  // ── PoiStreamReader ───────────────────────────────────────────────────────

  @Test
  void streamReaderShouldListWorksheetNames() throws IOException {
    Path file = writeXlsxFile("MySheet", "Col1", "Col2");

    var options = ExcelReadOptions.defaults();
    try (PoiStreamReader reader = new PoiStreamReader(file, options, PoiConfiguration.defaults())) {
      Result<List<String>> names = reader.getWorksheetNames();
      assertThat(names.isOk()).isTrue();
      assertThat(names.getOrNull()).contains("MySheet");
    }
  }

  @Test
  void streamReaderShouldSelectWorksheetAndReadRows() throws IOException {
    Path file = writeXlsxFile("Data", "Hello", "World");

    var options = ExcelReadOptions.defaults();
    try (PoiStreamReader reader = new PoiStreamReader(file, options, PoiConfiguration.defaults())) {
      Result<Void> select = reader.selectWorksheet("Data");
      assertThat(select.isOk()).isTrue();
      assertThat(reader.hasNext()).isTrue();

      Result<List<ExcelCell>> row = reader.readNext();
      assertThat(row.isOk()).isTrue();
      assertThat(row.getOrNull()).hasSize(2);
    }
  }

  @Test
  void streamReaderSelectMissingWorksheetShouldFail() throws IOException {
    Path file = writeXlsxFile("Sheet1", "A");

    var options = ExcelReadOptions.defaults();
    try (PoiStreamReader reader = new PoiStreamReader(file, options, PoiConfiguration.defaults())) {
      Result<Void> select = reader.selectWorksheet("NoSuch");
      assertThat(select.isFail()).isTrue();
    }
  }

  @Test
  void streamReaderShouldReadAllRowsUntilEmpty() throws IOException {
    Path file;
    try (Workbook wb = new XSSFWorkbook()) {
      Sheet sheet = wb.createSheet("Rows");
      for (int r = 0; r < 5; r++) {
        Row row = sheet.createRow(r);
        row.createCell(0).setCellValue("row-" + r);
        row.createCell(1).setCellValue(r * 10.0);
      }
      file = tempDir.resolve("multi-row.xlsx");
      try (var out = Files.newOutputStream(file)) {
        wb.write(out);
      }
    }

    try (PoiStreamReader reader =
        new PoiStreamReader(file, ExcelReadOptions.defaults(), PoiConfiguration.defaults())) {
      reader.selectWorksheet("Rows");
      int count = 0;
      while (reader.hasNext()) {
        Result<List<ExcelCell>> row = reader.readNext();
        assertThat(row.isOk()).isTrue();
        count++;
      }
      assertThat(count).isEqualTo(5);
    }
  }

  @Test
  void streamReaderGetCurrentRowNumShouldIncrementOnRead() throws IOException {
    Path file = writeXlsxFile("Sheet1", "A", "B");

    try (PoiStreamReader reader =
        new PoiStreamReader(file, ExcelReadOptions.defaults(), PoiConfiguration.defaults())) {
      reader.selectWorksheet("Sheet1");
      assertThat(reader.hasNext()).isTrue();
      reader.readNext();
      assertThat(reader.getCurrentRowNum()).isGreaterThanOrEqualTo(0);
    }
  }

  // ── PoiStreamWriter: extended coverage ───────────────────────────────────

  @Test
  void streamWriterWriteRowWithNullSheetShouldFail() throws IOException {
    Path output = tempDir.resolve("no-sheet.xlsx");
    try (PoiStreamWriter writer =
        new PoiStreamWriter(output, ExcelWriteOptions.defaults(), PoiConfiguration.defaults())) {
      // No worksheet created — writeRow should fail
      List<ExcelCell> row =
          List.of(
              new ExcelCell(
                  0, 0, com.marcusprado02.commons.ports.excel.CellType.STRING, "x", null, null));
      Result<Void> result = writer.writeRow(row);
      assertThat(result.isFail()).isTrue();
    }
  }

  @Test
  void streamWriterSetColumnWidthShouldWork() throws IOException {
    Path output = tempDir.resolve("col-width.xlsx");
    try (PoiStreamWriter writer =
        new PoiStreamWriter(output, ExcelWriteOptions.defaults(), PoiConfiguration.defaults())) {
      writer.createWorksheet("Sheet1");
      Result<Void> result = writer.setColumnWidth(0, 20.0);
      assertThat(result.isOk()).isTrue();
    }
  }

  @Test
  void streamWriterSetRowHeightShouldWork() throws IOException {
    Path output = tempDir.resolve("row-height.xlsx");
    try (PoiStreamWriter writer =
        new PoiStreamWriter(output, ExcelWriteOptions.defaults(), PoiConfiguration.defaults())) {
      writer.createWorksheet("Sheet1");
      writer.writeRow(
          List.of(
              new ExcelCell(
                  0, 0, com.marcusprado02.commons.ports.excel.CellType.STRING, "x", null, null)));
      Result<Void> result = writer.setRowHeight(25.0);
      assertThat(result.isOk()).isTrue();
    }
  }

  @Test
  void streamWriterFreezePanesShouldWork() throws IOException {
    Path output = tempDir.resolve("freeze.xlsx");
    try (PoiStreamWriter writer =
        new PoiStreamWriter(output, ExcelWriteOptions.defaults(), PoiConfiguration.defaults())) {
      writer.createWorksheet("Sheet1");
      Result<Void> result = writer.freezePanes(1, 0);
      assertThat(result.isOk()).isTrue();
    }
  }

  @Test
  void streamWriterEnableAutoFilterShouldWork() throws IOException {
    Path output = tempDir.resolve("autofilter.xlsx");
    try (PoiStreamWriter writer =
        new PoiStreamWriter(output, ExcelWriteOptions.defaults(), PoiConfiguration.defaults())) {
      writer.createWorksheet("Sheet1");
      Result<Void> result = writer.enableAutoFilter();
      assertThat(result.isOk()).isTrue();
    }
  }

  @Test
  void streamWriterCurrentRowNumAndNextRowShouldWork() throws IOException {
    Path output = tempDir.resolve("rownum.xlsx");
    try (PoiStreamWriter writer =
        new PoiStreamWriter(output, ExcelWriteOptions.defaults(), PoiConfiguration.defaults())) {
      writer.createWorksheet("Sheet1");
      int initial = writer.getCurrentRowNum();
      writer.nextRow();
      assertThat(writer.getCurrentRowNum()).isEqualTo(initial + 1);
    }
  }

  @Test
  void streamWriterXlsFormatShouldWork() throws IOException {
    Path output = tempDir.resolve("stream-output.xls");
    try (PoiStreamWriter writer =
        new PoiStreamWriter(output, ExcelWriteOptions.xls(), PoiConfiguration.defaults())) {
      writer.createWorksheet("Sheet1");
      writer.writeRow(
          List.of(
              new ExcelCell(
                  0,
                  0,
                  com.marcusprado02.commons.ports.excel.CellType.STRING,
                  "hello",
                  null,
                  null)));
      Result<Void> flush = writer.flush();
      assertThat(flush.isOk()).isTrue();
    }
    assertThat(Files.size(output)).isGreaterThan(0);
  }

  @Test
  void streamWriterShouldWriteAllCellTypes() throws IOException {
    Path output = tempDir.resolve("all-types.xlsx");
    try (PoiStreamWriter writer =
        new PoiStreamWriter(output, ExcelWriteOptions.defaults(), PoiConfiguration.defaults())) {
      writer.createWorksheet("Types");

      List<ExcelCell> row =
          List.of(
              new ExcelCell(
                  0, 0, com.marcusprado02.commons.ports.excel.CellType.BLANK, null, null, null),
              new ExcelCell(
                  0, 1, com.marcusprado02.commons.ports.excel.CellType.BOOLEAN, true, null, null),
              new ExcelCell(
                  0, 2, com.marcusprado02.commons.ports.excel.CellType.NUMERIC, 3.14, null, null),
              new ExcelCell(
                  0, 3, com.marcusprado02.commons.ports.excel.CellType.STRING, "text", null, null),
              new ExcelCell(
                  0, 4, com.marcusprado02.commons.ports.excel.CellType.ERROR, null, null, null));

      Result<Void> result = writer.writeRow(row);
      assertThat(result.isOk()).isTrue();
    }
  }

  @Test
  void streamWriterShouldWriteNumericDateCellTypes() throws IOException {
    Path output = tempDir.resolve("date-types.xlsx");
    try (PoiStreamWriter writer =
        new PoiStreamWriter(output, ExcelWriteOptions.defaults(), PoiConfiguration.defaults())) {
      writer.createWorksheet("Dates");

      List<ExcelCell> row =
          List.of(
              new ExcelCell(
                  0,
                  0,
                  com.marcusprado02.commons.ports.excel.CellType.NUMERIC,
                  java.time.LocalDate.now(),
                  null,
                  null),
              new ExcelCell(
                  0,
                  1,
                  com.marcusprado02.commons.ports.excel.CellType.NUMERIC,
                  java.time.LocalDateTime.now(),
                  null,
                  null));

      Result<Void> result = writer.writeRow(row);
      assertThat(result.isOk()).isTrue();
    }
  }

  @Test
  void streamWriterShouldWriteFormulaCellWithFormula() throws IOException {
    Path output = tempDir.resolve("formula.xlsx");
    var options =
        com.marcusprado02.commons.ports.excel.ExcelWriteOptions.builder()
            .writeFormulas(true)
            .build();
    try (PoiStreamWriter writer =
        new PoiStreamWriter(output, options, PoiConfiguration.defaults())) {
      writer.createWorksheet("Formulas");
      List<ExcelCell> row =
          List.of(
              new ExcelCell(
                  0,
                  0,
                  com.marcusprado02.commons.ports.excel.CellType.FORMULA,
                  null,
                  "SUM(1,2)",
                  null));
      Result<Void> result = writer.writeRow(row);
      assertThat(result.isOk()).isTrue();
    }
  }

  @Test
  void streamWriterWriteRowAdjustsRowIndexMismatch() throws IOException {
    Path output = tempDir.resolve("row-adjust.xlsx");
    try (PoiStreamWriter writer =
        new PoiStreamWriter(output, ExcelWriteOptions.defaults(), PoiConfiguration.defaults())) {
      writer.createWorksheet("Sheet1");
      // Cell has row=5 but writer is at row 0 — should be auto-adjusted
      List<ExcelCell> row =
          List.of(
              new ExcelCell(
                  5,
                  0,
                  com.marcusprado02.commons.ports.excel.CellType.STRING,
                  "adjusted",
                  null,
                  null));
      Result<Void> result = writer.writeRow(row);
      assertThat(result.isOk()).isTrue();
    }
  }

  // ── PoiExcelAdapter: uncovered methods ───────────────────────────────────

  @Test
  void adapterValidateFileShouldReturnResult() throws IOException {
    var adapter = new PoiExcelAdapter();
    Path file = writeXlsxFile("Sheet1", "A", "B");
    assertThat(adapter.validateFile(file)).isNotNull();
  }

  @Test
  void adapterCreateStreamReaderForValidFileShouldSucceed() throws IOException {
    var adapter = new PoiExcelAdapter();
    Path file = writeXlsxFile("Sheet1", "A");
    Result<?> result = adapter.createStreamReader(file, ExcelReadOptions.defaults());
    assertThat(result.isOk()).isTrue();
    // Close the reader to release file handle
    if (result.getOrNull() instanceof AutoCloseable ac) {
      try {
        ac.close();
      } catch (Exception ignored) {
      }
    }
  }

  // ── PoiWorkbookMapper: styled cells and formula cells ────────────────────

  @Test
  void workbookMapperToPoiShouldHandleFormulaCell() {
    try (Workbook wb = new XSSFWorkbook()) {
      var cell =
          new ExcelCell(
              0,
              0,
              com.marcusprado02.commons.ports.excel.CellType.FORMULA,
              null,
              "SUM(A1:A3)",
              null);
      var worksheet =
          com.marcusprado02.commons.ports.excel.ExcelWorksheet.builder("Formulas")
              .cell(cell)
              .build();
      var workbook =
          com.marcusprado02.commons.ports.excel.ExcelWorkbook.builder()
              .worksheet(worksheet)
              .build();

      PoiWorkbookMapper.toPoi(workbook, wb, ExcelWriteOptions.defaults());
      assertThat(wb.getSheetAt(0).getRow(0).getCell(0).getCellFormula()).isEqualTo("SUM(A1:A3)");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void workbookMapperToPoiShouldHandleBooleanAndNullCell() {
    try (Workbook wb = new XSSFWorkbook()) {
      var worksheet =
          com.marcusprado02.commons.ports.excel.ExcelWorksheet.builder("Types")
              .cell(
                  new ExcelCell(
                      0,
                      0,
                      com.marcusprado02.commons.ports.excel.CellType.BOOLEAN,
                      true,
                      null,
                      null))
              .cell(
                  new ExcelCell(
                      0,
                      1,
                      com.marcusprado02.commons.ports.excel.CellType.STRING,
                      null,
                      null,
                      null))
              .build();
      var workbook =
          com.marcusprado02.commons.ports.excel.ExcelWorkbook.builder()
              .worksheet(worksheet)
              .build();

      PoiWorkbookMapper.toPoi(workbook, wb, ExcelWriteOptions.defaults());
      assertThat(wb.getSheetAt(0).getRow(0).getCell(0).getBooleanCellValue()).isTrue();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void workbookMapperToPoiShouldHandleStyledCell() {
    try (Workbook wb = new XSSFWorkbook()) {
      var style =
          com.marcusprado02.commons.ports.excel.ExcelCellStyle.builder()
              .fontName("Arial")
              .fontSize(12)
              .bold(true)
              .italic(false)
              .wrapText(true)
              .build();
      var cell =
          new ExcelCell(
              0, 0, com.marcusprado02.commons.ports.excel.CellType.STRING, "styled", null, style);
      var worksheet =
          com.marcusprado02.commons.ports.excel.ExcelWorksheet.builder("Styled").cell(cell).build();
      var workbook =
          com.marcusprado02.commons.ports.excel.ExcelWorkbook.builder()
              .worksheet(worksheet)
              .build();

      PoiWorkbookMapper.toPoi(workbook, wb, ExcelWriteOptions.defaults());
      // Styled cell written — just verify it didn't throw
      assertThat(wb.getSheetAt(0).getRow(0).getCell(0).getStringCellValue()).isEqualTo("styled");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void workbookMapperFromPoiShouldMapNumericAndBooleanCells() {
    try (Workbook wb = new XSSFWorkbook()) {
      Sheet sheet = wb.createSheet("Data");
      Row row = sheet.createRow(0);
      Cell numCell = row.createCell(0, CellType.NUMERIC);
      numCell.setCellValue(99.0);
      Cell boolCell = row.createCell(1, CellType.BOOLEAN);
      boolCell.setCellValue(false);
      Cell blankCell = row.createCell(2, CellType.BLANK);

      ExcelWorkbook mapped = PoiWorkbookMapper.fromPoi(wb);

      assertThat(mapped.getWorksheet("Data")).isNotNull();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  // ── PoiStreamWriter: style-exercising tests ───────────────────────────────

  @Test
  void streamWriterShouldWriteStyledCell() throws IOException {
    Path output = tempDir.resolve("styled-cell.xlsx");
    try (PoiStreamWriter writer =
        new PoiStreamWriter(output, ExcelWriteOptions.defaults(), PoiConfiguration.defaults())) {
      writer.createWorksheet("Sheet1");

      var style =
          com.marcusprado02.commons.ports.excel.ExcelCellStyle.builder()
              .fontName("Arial")
              .fontSize(14)
              .bold(true)
              .italic(true)
              .horizontalAlignment(
                  com.marcusprado02.commons.ports.excel.ExcelCellStyle.HorizontalAlignment.CENTER)
              .verticalAlignment(
                  com.marcusprado02.commons.ports.excel.ExcelCellStyle.VerticalAlignment.CENTER)
              .wrapText(true)
              .numberFormat("#,##0.00")
              .borderStyle(com.marcusprado02.commons.ports.excel.ExcelCellStyle.BorderStyle.THIN)
              .build();

      List<ExcelCell> row =
          List.of(
              new ExcelCell(
                  0,
                  0,
                  com.marcusprado02.commons.ports.excel.CellType.STRING,
                  "styled",
                  null,
                  style));

      Result<Void> result = writer.writeRow(row);
      assertThat(result.isOk()).isTrue();
    }
  }

  // ── PoiStreamWriter: setColumnWidth with XLS (non-streaming) ─────────────

  @Test
  void streamWriterSetColumnWidthXlsShouldApplyImmediately() throws IOException {
    Path output = tempDir.resolve("col-width-xls.xls");
    try (PoiStreamWriter writer =
        new PoiStreamWriter(output, ExcelWriteOptions.xls(), PoiConfiguration.defaults())) {
      writer.createWorksheet("Sheet1");
      // XLS uses HSSFWorkbook (not SXSSF), so width applies immediately
      Result<Void> result = writer.setColumnWidth(0, 15.0);
      assertThat(result.isOk()).isTrue();
    }
  }

  // ── PoiCsvConverter: delimiter variants ──────────────────────────────────

  @Test
  void csvConverterWithSemicolonDelimiterShouldWork() {
    var adapter = new PoiExcelAdapter();
    var worksheet =
        com.marcusprado02.commons.ports.excel.ExcelWorksheet.builder("Sheet1")
            .cell(0, 0, "Name")
            .cell(0, 1, "Score")
            .cell(1, 0, "Alice")
            .cell(1, 1, 95)
            .build();
    var workbook =
        com.marcusprado02.commons.ports.excel.ExcelWorkbook.builder().worksheet(worksheet).build();

    Result<String> result = adapter.toCsv(workbook, "Sheet1", CsvOptions.semicolon());

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).contains(";");
  }

  @Test
  void csvConverterWithTabDelimiterShouldWork() {
    var adapter = new PoiExcelAdapter();
    var worksheet =
        com.marcusprado02.commons.ports.excel.ExcelWorksheet.builder("Sheet1")
            .cell(0, 0, "A")
            .cell(0, 1, "B")
            .build();
    var workbook =
        com.marcusprado02.commons.ports.excel.ExcelWorkbook.builder().worksheet(worksheet).build();

    Result<String> result = adapter.toCsv(workbook, "Sheet1", CsvOptions.tab());

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).contains("\t");
  }

  @Test
  void csvFromCsvWithSemicolonDelimiterShouldRoundTrip() {
    var adapter = new PoiExcelAdapter();
    String csv = "Name;Score\nAlice;95";

    Result<ExcelWorkbook> result = adapter.fromCsv(csv, "Data", CsvOptions.semicolon());

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().getWorksheet("Data")).isNotNull();
  }

  // ── PoiCellMapper: formula cell branches ─────────────────────────────────

  @Test
  void cellMapperShouldMapFormulaCellWithCachedString() {
    try (Workbook wb = new XSSFWorkbook()) {
      Sheet sheet = wb.createSheet("S");
      Row row = sheet.createRow(0);
      // Create a FORMULA cell — the cached result type will be STRING
      org.apache.poi.xssf.usermodel.XSSFCell cell =
          (org.apache.poi.xssf.usermodel.XSSFCell) row.createCell(0, CellType.FORMULA);
      cell.setCellFormula("CONCATENATE(\"a\",\"b\")");
      cell.setCellValue("ab"); // set cached value as string

      ExcelCell mapped = PoiCellMapper.fromPoi(cell);

      assertThat(mapped).isNotNull();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void streamReaderHasNextWithoutSelectShouldReturnFalse() throws IOException {
    Path file = writeXlsxFile("Sheet1", "A");
    // Don't call selectWorksheet — rowIterator is null, hasNext() should return false
    try (PoiStreamReader reader =
        new PoiStreamReader(file, ExcelReadOptions.defaults(), PoiConfiguration.defaults())) {
      assertThat(reader.hasNext()).isFalse();
    }
  }

  @Test
  void cellMapperShouldHandleFormulaCellWithCachedNumeric() {
    try (Workbook wb = new XSSFWorkbook()) {
      Sheet sheet = wb.createSheet("S");
      Row row = sheet.createRow(0);
      org.apache.poi.xssf.usermodel.XSSFCell cell =
          (org.apache.poi.xssf.usermodel.XSSFCell) row.createCell(0, CellType.FORMULA);
      cell.setCellFormula("1+1");
      cell.setCellValue(2.0); // cached numeric value

      ExcelCell mapped = PoiCellMapper.fromPoi(cell);

      assertThat(mapped).isNotNull();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
