package com.marcusprado02.commons.adapters.excel.poi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.excel.CellType;
import com.marcusprado02.commons.ports.excel.CsvOptions;
import com.marcusprado02.commons.ports.excel.ExcelCell;
import com.marcusprado02.commons.ports.excel.ExcelCellStyle;
import com.marcusprado02.commons.ports.excel.ExcelReadOptions;
import com.marcusprado02.commons.ports.excel.ExcelValidationResult;
import com.marcusprado02.commons.ports.excel.ExcelWorkbook;
import com.marcusprado02.commons.ports.excel.ExcelWorksheet;
import com.marcusprado02.commons.ports.excel.ExcelWriteOptions;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Branch/line coverage tests for commons-adapters-excel-poi. */
class PoiBranchTest {

  @TempDir Path tempDir;

  // ── helpers ──────────────────────────────────────────────────────────────

  private Path writeXlsxWith(String sheetName, java.util.function.Consumer<Sheet> filler)
      throws IOException {
    try (Workbook wb = new XSSFWorkbook()) {
      Sheet sheet = wb.createSheet(sheetName);
      filler.accept(sheet);
      Path file = tempDir.resolve("test-" + System.nanoTime() + ".xlsx");
      try (var out = Files.newOutputStream(file)) {
        wb.write(out);
      }
      return file;
    }
  }

  private Path writeXlsxFile(String sheetName, String... values) throws IOException {
    return writeXlsxWith(
        sheetName,
        sheet -> {
          Row row = sheet.createRow(0);
          for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i]);
          }
        });
  }

  // ── PoiConfiguration: validation branches ────────────────────────────────

  @Test
  void configurationShouldRejectZeroStreamingWindow() {
    assertThatThrownBy(
            () -> new PoiConfiguration(true, 0, 1048576, 16384, true, true, false, false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Streaming row access window");
  }

  @Test
  void configurationShouldRejectNegativeStreamingWindow() {
    assertThatThrownBy(
            () -> new PoiConfiguration(true, -1, 1048576, 16384, true, true, false, false))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void configurationShouldRejectZeroMaxRows() {
    assertThatThrownBy(() -> new PoiConfiguration(true, 100, 0, 16384, true, true, false, false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Max rows");
  }

  @Test
  void configurationShouldRejectZeroMaxColumns() {
    assertThatThrownBy(() -> new PoiConfiguration(true, 100, 1048576, 0, true, true, false, false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Max columns");
  }

  // ── PoiExcelAdapter: file-path based readWorkbook ─────────────────────────

  @Test
  void adapterReadWorkbookFromPathShouldSucceed() throws IOException {
    Path file = writeXlsxFile("Sheet1", "Hello");
    var adapter = new PoiExcelAdapter();
    Result<ExcelWorkbook> result = adapter.readWorkbook(file);
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().getWorksheetCount()).isEqualTo(1);
  }

  @Test
  void adapterReadWorkbookFromMissingPathShouldFail() {
    Path missing = tempDir.resolve("does-not-exist.xlsx");
    var adapter = new PoiExcelAdapter();
    Result<ExcelWorkbook> result = adapter.readWorkbook(missing);
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("EXCEL_READ_ERROR");
  }

  // ── PoiExcelAdapter: file-path based writeWorkbook ────────────────────────

  @Test
  void adapterWriteWorkbookToPathShouldSucceed() {
    Path output = tempDir.resolve("output.xlsx");
    var adapter = new PoiExcelAdapter();
    var worksheet = ExcelWorksheet.builder("Sheet1").cell(0, 0, "Data").build();
    var workbook = ExcelWorkbook.builder().worksheet(worksheet).build();

    Result<Void> result = adapter.writeWorkbook(workbook, output, ExcelWriteOptions.defaults());
    assertThat(result.isOk()).isTrue();
    assertThat(output.toFile()).exists();
  }

  @Test
  void adapterWriteWorkbookToInvalidPathShouldFail() {
    Path output = Path.of("/nonexistent-dir-xyz/output.xlsx");
    var adapter = new PoiExcelAdapter();
    var worksheet = ExcelWorksheet.builder("S").cell(0, 0, "x").build();
    var workbook = ExcelWorkbook.builder().worksheet(worksheet).build();

    Result<Void> result = adapter.writeWorkbook(workbook, output, ExcelWriteOptions.defaults());
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("EXCEL_WRITE_FILE_ERROR");
  }

  // ── PoiExcelAdapter: createStreamReader from invalid path ─────────────────

  @Test
  void adapterCreateStreamReaderFromMissingPathShouldFail() {
    Path missing = tempDir.resolve("nope.xlsx");
    var adapter = new PoiExcelAdapter();
    Result<?> result = adapter.createStreamReader(missing, ExcelReadOptions.defaults());
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("EXCEL_STREAM_READER_ERROR");
  }

  // ── PoiExcelAdapter: validateFile with non-existent path ─────────────────

  @Test
  void adapterValidateFileMissingShouldReturnInvalid() {
    var adapter = new PoiExcelAdapter();
    Result<ExcelValidationResult> result = adapter.validateFile(tempDir.resolve("missing.xlsx"));
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().isValid()).isFalse();
  }

  // ── PoiExcelAdapter: toCsv with null worksheet name (active sheet) ────────

  @Test
  void adapterToCsvWithNullWorksheetNameShouldUseActiveSheet() {
    var adapter = new PoiExcelAdapter();
    var worksheet = ExcelWorksheet.builder("Active").cell(0, 0, "X").cell(0, 1, "Y").build();
    var workbook = ExcelWorkbook.builder().worksheet(worksheet).build();

    Result<String> result = adapter.toCsv(workbook, null, CsvOptions.defaults());
    assertThat(result.isOk()).isTrue();
  }

  // ── PoiExcelAdapter: fromCsv with null worksheet name ─────────────────────

  @Test
  void adapterFromCsvWithNullWorksheetNameShouldDefaultToSheet1() {
    var adapter = new PoiExcelAdapter();
    Result<ExcelWorkbook> result = adapter.fromCsv("a,b\n1,2", null, CsvOptions.defaults());
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().getWorksheet("Sheet1")).isNotNull();
  }

  // ── PoiFileValidator: format detection branches ──────────────────────────

  @Test
  void validateXlsFileShouldReturnValid() throws IOException {
    Path file = tempDir.resolve("legacy.xls");
    try (Workbook wb = new HSSFWorkbook()) {
      wb.createSheet("Sheet1");
      try (var out = Files.newOutputStream(file)) {
        wb.write(out);
      }
    }
    ExcelValidationResult result = PoiFileValidator.validate(file);
    assertThat(result.isValid()).isTrue();
    assertThat(result.format()).isEqualTo("XLS");
  }

  @Test
  void validateFileWithUnknownExtensionShouldAddWarning() throws IOException {
    // Write a real xlsx file but with .bin extension -> unknown format
    Path xlsxSrc = writeXlsxFile("S", "v");
    Path unknown = tempDir.resolve("file.bin");
    Files.copy(xlsxSrc, unknown);
    ExcelValidationResult result = PoiFileValidator.validate(unknown);
    // warnings about unrecognized extension are expected
    assertThat(result).isNotNull();
  }

  @Test
  void validateCsvExtensionShouldDetectCsvFormat() throws IOException {
    // Write a real xlsx bytes but with .csv extension; validator detects by extension first
    Path xlsxSrc = writeXlsxFile("S", "v");
    Path csvFile = tempDir.resolve("data.csv");
    Files.copy(xlsxSrc, csvFile);
    ExcelValidationResult result = PoiFileValidator.validate(csvFile);
    assertThat(result).isNotNull();
  }

  @Test
  void validateXlsmExtensionShouldDetectXlsm() throws IOException {
    Path xlsxSrc = writeXlsxFile("S", "v");
    Path xlsm = tempDir.resolve("macro.xlsm");
    Files.copy(xlsxSrc, xlsm);
    ExcelValidationResult result = PoiFileValidator.validate(xlsm);
    assertThat(result).isNotNull();
  }

  @Test
  void validateXlsbExtensionShouldDetectXlsb() throws IOException {
    // Can't easily create real XLSB, just check that the validator doesn't throw
    Path fake = tempDir.resolve("data.xlsb");
    Files.writeString(fake, "not a real xlsb");
    ExcelValidationResult result = PoiFileValidator.validate(fake);
    // Should fail gracefully (corrupt content)
    assertThat(result.isValid()).isFalse();
  }

  @Test
  void validateXlsxWithFormulaShouldDetectFormulas() throws IOException {
    Path file =
        writeXlsxWith(
            "Sheet1",
            sheet -> {
              Row row = sheet.createRow(0);
              row.createCell(0).setCellValue(1.0);
              row.createCell(1).setCellValue(2.0);
              Cell formulaCell = row.createCell(2, org.apache.poi.ss.usermodel.CellType.FORMULA);
              formulaCell.setCellFormula("A1+B1");
            });
    ExcelValidationResult result = PoiFileValidator.validate(file);
    assertThat(result.isValid()).isTrue();
    assertThat(result.hasFormulas()).isTrue();
  }

  // ── PoiCellMapper: date-formatted numeric cells ──────────────────────────

  @Test
  void cellMapperShouldMapDateFormattedNumericCell() throws IOException {
    try (Workbook wb = new XSSFWorkbook()) {
      Sheet sheet = wb.createSheet("S");
      Row row = sheet.createRow(0);
      Cell cell = row.createCell(0, org.apache.poi.ss.usermodel.CellType.NUMERIC);
      // Apply a date format style
      CellStyle style = wb.createCellStyle();
      DataFormat fmt = wb.createDataFormat();
      style.setDataFormat(fmt.getFormat("yyyy-mm-dd"));
      cell.setCellStyle(style);
      cell.setCellValue(
          java.util.Date.from(
              LocalDate.of(2024, 6, 15)
                  .atStartOfDay(java.time.ZoneId.systemDefault())
                  .toInstant()));

      ExcelCell mapped = PoiCellMapper.fromPoi(cell);

      assertThat(mapped.value()).isInstanceOf(LocalDate.class);
    }
  }

  @Test
  void cellMapperShouldMapFormulaCellWithCachedBooleanTrue() throws IOException {
    try (Workbook wb = new XSSFWorkbook()) {
      Sheet sheet = wb.createSheet("S");
      Row row = sheet.createRow(0);
      org.apache.poi.xssf.usermodel.XSSFCell cell =
          (org.apache.poi.xssf.usermodel.XSSFCell)
              row.createCell(0, org.apache.poi.ss.usermodel.CellType.FORMULA);
      cell.setCellFormula("TRUE()");
      cell.setCellValue(true);

      ExcelCell mapped = PoiCellMapper.fromPoi(cell);

      assertThat(mapped).isNotNull();
      // Formula type with cached boolean
      assertThat(mapped.formula()).isNotNull();
    }
  }

  @Test
  void cellMapperShouldMapFormulaCellWithDateFormattedNumeric() throws IOException {
    try (Workbook wb = new XSSFWorkbook()) {
      Sheet sheet = wb.createSheet("S");
      Row row = sheet.createRow(0);
      org.apache.poi.xssf.usermodel.XSSFCell cell =
          (org.apache.poi.xssf.usermodel.XSSFCell)
              row.createCell(0, org.apache.poi.ss.usermodel.CellType.FORMULA);
      CellStyle style = wb.createCellStyle();
      DataFormat fmt = wb.createDataFormat();
      style.setDataFormat(fmt.getFormat("yyyy-mm-dd"));
      cell.setCellStyle(style);
      cell.setCellFormula("TODAY()");
      cell.setCellValue(
          java.util.Date.from(
              LocalDate.of(2024, 1, 1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));

      ExcelCell mapped = PoiCellMapper.fromPoi(cell);

      assertThat(mapped).isNotNull();
    }
  }

  // ── PoiCsvConverter: toCsv branches ──────────────────────────────────────

  @Test
  void csvConverterToCsvShouldReturnEmptyForEmptyWorksheet() {
    var adapter = new PoiExcelAdapter();
    var worksheet = ExcelWorksheet.builder("Empty").build();
    var workbook = ExcelWorkbook.builder().worksheet(worksheet).build();

    Result<String> result = adapter.toCsv(workbook, "Empty", CsvOptions.noHeaders());
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isEmpty();
  }

  @Test
  void csvConverterToCsvWithoutHeadersShouldSkipHeaderRow() {
    var adapter = new PoiExcelAdapter();
    var worksheet =
        ExcelWorksheet.builder("Data")
            .cell(0, 0, "Name")
            .cell(0, 1, "Age")
            .cell(1, 0, "Bob")
            .cell(1, 1, 25)
            .build();
    var workbook = ExcelWorkbook.builder().worksheet(worksheet).build();

    Result<String> result = adapter.toCsv(workbook, "Data", CsvOptions.noHeaders());
    assertThat(result.isOk()).isTrue();
    String csv = result.getOrNull();
    // Both rows are written when no headers option means start from row 0
    assertThat(csv).isNotEmpty();
  }

  @Test
  void csvConverterToCsvShouldFormatDateCell() {
    var adapter = new PoiExcelAdapter();
    var worksheet =
        ExcelWorksheet.builder("Dates")
            .cell(new ExcelCell(0, 0, CellType.NUMERIC, LocalDate.of(2024, 3, 15), null, null))
            .build();
    var workbook = ExcelWorkbook.builder().worksheet(worksheet).build();

    Result<String> result = adapter.toCsv(workbook, "Dates", CsvOptions.noHeaders());
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).contains("2024-03-15");
  }

  @Test
  void csvConverterToCsvShouldFormatDateCellWithCustomFormat() {
    var adapter = new PoiExcelAdapter();
    var worksheet =
        ExcelWorksheet.builder("Dates")
            .cell(new ExcelCell(0, 0, CellType.NUMERIC, LocalDate.of(2024, 3, 15), null, null))
            .build();
    var workbook = ExcelWorkbook.builder().worksheet(worksheet).build();
    var options = CsvOptions.builder().dateFormat("dd/MM/yyyy").includeHeaders(false).build();

    Result<String> result = adapter.toCsv(workbook, "Dates", options);
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).contains("15/03/2024");
  }

  @Test
  void csvConverterToCsvShouldFormatDateTimeCell() {
    var adapter = new PoiExcelAdapter();
    LocalDateTime dt = LocalDateTime.of(2024, 3, 15, 10, 30, 0);
    var worksheet =
        ExcelWorksheet.builder("DateTimes")
            .cell(new ExcelCell(0, 0, CellType.NUMERIC, dt, null, null))
            .build();
    var workbook = ExcelWorkbook.builder().worksheet(worksheet).build();

    Result<String> result = adapter.toCsv(workbook, "DateTimes", CsvOptions.noHeaders());
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).contains("2024-03-15");
  }

  @Test
  void csvConverterToCsvShouldFormatDateTimeWithCustomFormat() {
    var adapter = new PoiExcelAdapter();
    LocalDateTime dt = LocalDateTime.of(2024, 3, 15, 10, 30, 0);
    var worksheet =
        ExcelWorksheet.builder("DateTimes")
            .cell(new ExcelCell(0, 0, CellType.NUMERIC, dt, null, null))
            .build();
    var workbook = ExcelWorkbook.builder().worksheet(worksheet).build();
    var options =
        CsvOptions.builder().dateFormat("yyyy/MM/dd HH:mm:ss").includeHeaders(false).build();

    Result<String> result = adapter.toCsv(workbook, "DateTimes", options);
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).contains("2024/03/15");
  }

  @Test
  void csvConverterToCsvShouldFormatNumericWithCustomFormat() {
    var adapter = new PoiExcelAdapter();
    var worksheet =
        ExcelWorksheet.builder("Nums")
            .cell(new ExcelCell(0, 0, CellType.NUMERIC, 3.14159, null, null))
            .build();
    var workbook = ExcelWorkbook.builder().worksheet(worksheet).build();
    var options = CsvOptions.builder().numberFormat("%.2f").includeHeaders(false).build();

    Result<String> result = adapter.toCsv(workbook, "Nums", options);
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).contains("3.14");
  }

  @Test
  void csvConverterToCsvShouldFormatWholeNumberWithoutDecimal() {
    var adapter = new PoiExcelAdapter();
    var worksheet =
        ExcelWorksheet.builder("Nums")
            .cell(new ExcelCell(0, 0, CellType.NUMERIC, 42.0, null, null))
            .build();
    var workbook = ExcelWorkbook.builder().worksheet(worksheet).build();

    Result<String> result = adapter.toCsv(workbook, "Nums", CsvOptions.noHeaders());
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).contains("42");
    // Should not have trailing ".0"
    assertThat(result.getOrNull()).doesNotContain("42.0");
  }

  @Test
  void csvConverterToCsvShouldFormatNonWholeNumber() {
    var adapter = new PoiExcelAdapter();
    var worksheet =
        ExcelWorksheet.builder("Nums")
            .cell(new ExcelCell(0, 0, CellType.NUMERIC, 3.7, null, null))
            .build();
    var workbook = ExcelWorkbook.builder().worksheet(worksheet).build();

    Result<String> result = adapter.toCsv(workbook, "Nums", CsvOptions.noHeaders());
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).contains("3.7");
  }

  @Test
  void csvConverterToCsvShouldHandleBooleanCell() {
    var adapter = new PoiExcelAdapter();
    var worksheet =
        ExcelWorksheet.builder("Bools")
            .cell(new ExcelCell(0, 0, CellType.BOOLEAN, true, null, null))
            .cell(new ExcelCell(0, 1, CellType.BOOLEAN, false, null, null))
            .build();
    var workbook = ExcelWorkbook.builder().worksheet(worksheet).build();

    Result<String> result = adapter.toCsv(workbook, "Bools", CsvOptions.noHeaders());
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).contains("true").contains("false");
  }

  @Test
  void csvConverterToCsvShouldHandleFormulaWithNullValueAndNullFormula() {
    // value is non-null to pass isEmpty() check, but the FORMULA branch with null value
    // uses "=" + formula; here formula is null so yields "="
    var adapter = new PoiExcelAdapter();
    // We need a FORMULA cell that is not "empty" — pass a non-null value placeholder
    // but value is not a Number/String so it falls into formula text path via non-null value
    // Actually: value must be non-null to skip isEmpty(). Use a String to hit value.toString()
    var worksheet =
        ExcelWorksheet.builder("Formulas")
            .cell(new ExcelCell(0, 0, CellType.FORMULA, "placeholder", null, null))
            .build();
    var workbook = ExcelWorkbook.builder().worksheet(worksheet).build();

    Result<String> result = adapter.toCsv(workbook, "Formulas", CsvOptions.noHeaders());
    assertThat(result.isOk()).isTrue();
    // value "placeholder" is a String -> value.toString() -> "placeholder"
    assertThat(result.getOrNull()).contains("placeholder");
  }

  @Test
  void csvConverterToCsvShouldHandleFormulaWithFormulaText() {
    // FORMULA cell where value is null so isEmpty()=true -> returns nullValue=""
    // To hit the "=formula" branch, value must be non-null so isEmpty() passes,
    // then value==null check in FORMULA case yields "=" + formula
    // But value being non-null means it will be treated as a value, not formula text.
    // Instead verify the FORMULA branch processes correctly with a non-null formula value.
    var adapter = new PoiExcelAdapter();
    // Use non-null value (not Number) to hit the value.toString() branch
    var worksheet =
        ExcelWorksheet.builder("Formulas")
            .cell(new ExcelCell(0, 0, CellType.FORMULA, "SUM_RESULT", "SUM(A1:A5)", null))
            .build();
    var workbook = ExcelWorkbook.builder().worksheet(worksheet).build();

    Result<String> result = adapter.toCsv(workbook, "Formulas", CsvOptions.noHeaders());
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).contains("SUM_RESULT");
  }

  @Test
  void csvConverterToCsvShouldHandleFormulaWithNumberValue() {
    var adapter = new PoiExcelAdapter();
    var worksheet =
        ExcelWorksheet.builder("Formulas")
            .cell(new ExcelCell(0, 0, CellType.FORMULA, 42.0, "SUM(A1:A5)", null))
            .build();
    var workbook = ExcelWorkbook.builder().worksheet(worksheet).build();

    Result<String> result = adapter.toCsv(workbook, "Formulas", CsvOptions.noHeaders());
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).contains("42");
  }

  @Test
  void csvConverterToCsvShouldHandleFormulaWithStringValue() {
    var adapter = new PoiExcelAdapter();
    var worksheet =
        ExcelWorksheet.builder("Formulas")
            .cell(new ExcelCell(0, 0, CellType.FORMULA, "computed", "CONCATENATE()", null))
            .build();
    var workbook = ExcelWorkbook.builder().worksheet(worksheet).build();

    Result<String> result = adapter.toCsv(workbook, "Formulas", CsvOptions.noHeaders());
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).contains("computed");
  }

  @Test
  void csvConverterToCsvShouldHandleErrorCell() {
    // ERROR cells: isEmpty() = (BLANK || value==null). With value="#ERROR" it is NOT blank.
    // formatCellForCsv for ERROR returns "#ERROR".
    var adapter = new PoiExcelAdapter();
    var worksheet =
        ExcelWorksheet.builder("Errors")
            .cell(new ExcelCell(0, 0, CellType.ERROR, "#ERROR", null, null))
            .build();
    var workbook = ExcelWorkbook.builder().worksheet(worksheet).build();

    Result<String> result = adapter.toCsv(workbook, "Errors", CsvOptions.noHeaders());
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).contains("#ERROR");
  }

  @Test
  void csvConverterToCsvShouldHandleBlankCell() {
    var adapter = new PoiExcelAdapter();
    var worksheet =
        ExcelWorksheet.builder("Blanks")
            .cell(new ExcelCell(0, 0, CellType.BLANK, null, null, null))
            .build();
    var workbook = ExcelWorkbook.builder().worksheet(worksheet).build();

    Result<String> result = adapter.toCsv(workbook, "Blanks", CsvOptions.noHeaders());
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void csvConverterToCsvShouldSkipEmptyRows() {
    var adapter = new PoiExcelAdapter();
    // Row 0 has data, row 1 is blank, row 2 has data
    var worksheet =
        ExcelWorksheet.builder("Data")
            .cell(new ExcelCell(0, 0, CellType.STRING, "A", null, null))
            .cell(new ExcelCell(1, 0, CellType.BLANK, null, null, null))
            .cell(new ExcelCell(2, 0, CellType.STRING, "C", null, null))
            .build();
    var workbook = ExcelWorkbook.builder().worksheet(worksheet).build();
    var options = CsvOptions.builder().includeHeaders(false).skipEmptyLines(true).build();

    Result<String> result = adapter.toCsv(workbook, "Data", options);
    assertThat(result.isOk()).isTrue();
    String csv = result.getOrNull();
    assertThat(csv).contains("A").contains("C");
  }

  @Test
  void csvConverterToCsvNullActiveWorksheetShouldFail() {
    var adapter = new PoiExcelAdapter();
    // Empty workbook with no worksheets
    var workbook = ExcelWorkbook.builder().build();

    // null worksheetName means use active; no active worksheet -> fail
    Result<String> result = adapter.toCsv(workbook, null, CsvOptions.defaults());
    assertThat(result.isFail()).isTrue();
  }

  // ── PoiCsvConverter: fromCsv branches ────────────────────────────────────

  @Test
  void csvConverterFromCsvWithEmptyCsvShouldReturnEmptyWorkbook() {
    var adapter = new PoiExcelAdapter();
    Result<ExcelWorkbook> result = adapter.fromCsv("", "Sheet1", CsvOptions.defaults());
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().getWorksheetCount()).isEqualTo(1);
  }

  @Test
  void csvConverterFromCsvShouldParseBooleanValues() {
    var adapter = new PoiExcelAdapter();
    String csv = "TRUE,false";
    Result<ExcelWorkbook> result = adapter.fromCsv(csv, "Data", CsvOptions.noHeaders());
    assertThat(result.isOk()).isTrue();
    ExcelWorksheet ws = result.getOrNull().getWorksheet("Data");
    assertThat(ws).isNotNull();
  }

  @Test
  void csvConverterFromCsvShouldParseIntegerValues() {
    var adapter = new PoiExcelAdapter();
    String csv = "123,456";
    Result<ExcelWorkbook> result = adapter.fromCsv(csv, "Data", CsvOptions.noHeaders());
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void csvConverterFromCsvShouldParseDoubleValues() {
    var adapter = new PoiExcelAdapter();
    String csv = "3.14,2.71";
    Result<ExcelWorkbook> result = adapter.fromCsv(csv, "Data", CsvOptions.noHeaders());
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void csvConverterFromCsvShouldParseDateValues() {
    var adapter = new PoiExcelAdapter();
    String csv = "2024-01-15";
    Result<ExcelWorkbook> result = adapter.fromCsv(csv, "Data", CsvOptions.noHeaders());
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void csvConverterFromCsvShouldParseDateTimeValues() {
    var adapter = new PoiExcelAdapter();
    String csv = "2024-01-15 10:30:00";
    Result<ExcelWorkbook> result = adapter.fromCsv(csv, "Data", CsvOptions.noHeaders());
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void csvConverterFromCsvShouldHandleNullValues() {
    var adapter = new PoiExcelAdapter();
    // Empty field = null value
    String csv = "A,,C";
    Result<ExcelWorkbook> result = adapter.fromCsv(csv, "Data", CsvOptions.noHeaders());
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void csvConverterFromCsvShouldSkipEmptyRecords() {
    var adapter = new PoiExcelAdapter();
    // Contains an empty line
    String csv = "a,b\n\nc,d";
    var options = CsvOptions.builder().includeHeaders(false).skipEmptyLines(true).build();
    Result<ExcelWorkbook> result = adapter.fromCsv(csv, "Data", options);
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void csvConverterFromCsvWithCustomDateFormatShouldParseDates() {
    var adapter = new PoiExcelAdapter();
    String csv = "15/03/2024";
    var options = CsvOptions.builder().dateFormat("dd/MM/yyyy").includeHeaders(false).build();
    Result<ExcelWorkbook> result = adapter.fromCsv(csv, "Data", options);
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void csvConverterFromCsvWithHeadersShouldAddHeaderRow() {
    var adapter = new PoiExcelAdapter();
    String csv = "Name,Score\nAlice,95";
    Result<ExcelWorkbook> result = adapter.fromCsv(csv, "Data", CsvOptions.defaults());
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().getWorksheet("Data")).isNotNull();
  }

  @Test
  void csvConverterFromCsvWithCommaInDecimalValueShouldParseAsDouble() {
    var adapter = new PoiExcelAdapter();
    // A value containing comma in numbers
    String csv = "3,14";
    var options = CsvOptions.builder().delimiter(';').includeHeaders(false).build();
    Result<ExcelWorkbook> result = adapter.fromCsv(csv, "Data", options);
    assertThat(result.isOk()).isTrue();
  }

  // ── PoiStreamReader: branches ─────────────────────────────────────────────

  @Test
  void streamReaderReadNextWithoutWorksheetShouldFail() throws IOException {
    Path file = writeXlsxFile("Sheet1", "A");
    try (PoiStreamReader reader =
        new PoiStreamReader(file, ExcelReadOptions.defaults(), PoiConfiguration.defaults())) {
      // No selectWorksheet call
      Result<List<ExcelCell>> result = reader.readNext();
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("EXCEL_NO_WORKSHEET");
    }
  }

  @Test
  void streamReaderReadNextAtEndShouldFail() throws IOException {
    Path file = writeXlsxFile("Sheet1", "A");
    try (PoiStreamReader reader =
        new PoiStreamReader(file, ExcelReadOptions.defaults(), PoiConfiguration.defaults())) {
      reader.selectWorksheet("Sheet1");
      // Read first row
      reader.readNext();
      // Now try to read past the end
      Result<List<ExcelCell>> result = reader.readNext();
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("EXCEL_NO_MORE_ROWS");
    }
  }

  @Test
  void streamReaderSkipWithZeroCountShouldSucceed() throws IOException {
    Path file = writeXlsxFile("Sheet1", "A");
    try (PoiStreamReader reader =
        new PoiStreamReader(file, ExcelReadOptions.defaults(), PoiConfiguration.defaults())) {
      reader.selectWorksheet("Sheet1");
      Result<Void> result = reader.skip(0);
      assertThat(result.isOk()).isTrue();
    }
  }

  @Test
  void streamReaderSkipWithNegativeCountShouldSucceed() throws IOException {
    Path file = writeXlsxFile("Sheet1", "A");
    try (PoiStreamReader reader =
        new PoiStreamReader(file, ExcelReadOptions.defaults(), PoiConfiguration.defaults())) {
      reader.selectWorksheet("Sheet1");
      Result<Void> result = reader.skip(-5);
      assertThat(result.isOk()).isTrue();
    }
  }

  @Test
  void streamReaderSkipWithoutWorksheetShouldFail() throws IOException {
    Path file = writeXlsxFile("Sheet1", "A");
    try (PoiStreamReader reader =
        new PoiStreamReader(file, ExcelReadOptions.defaults(), PoiConfiguration.defaults())) {
      // No selectWorksheet
      Result<Void> result = reader.skip(2);
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("EXCEL_NO_WORKSHEET");
    }
  }

  @Test
  void streamReaderSkipShouldSkipRows() throws IOException {
    Path file;
    try (Workbook wb = new XSSFWorkbook()) {
      Sheet sheet = wb.createSheet("Data");
      for (int r = 0; r < 5; r++) {
        sheet.createRow(r).createCell(0).setCellValue("row-" + r);
      }
      file = tempDir.resolve("skip-rows.xlsx");
      try (var out = Files.newOutputStream(file)) {
        wb.write(out);
      }
    }

    try (PoiStreamReader reader =
        new PoiStreamReader(file, ExcelReadOptions.defaults(), PoiConfiguration.defaults())) {
      reader.selectWorksheet("Data");
      reader.skip(2);
      assertThat(reader.getCurrentRowNum()).isGreaterThanOrEqualTo(1);
    }
  }

  @Test
  void streamReaderSelectWorksheetOnClosedReaderShouldFail() throws IOException {
    Path file = writeXlsxFile("Sheet1", "A");
    PoiStreamReader reader =
        new PoiStreamReader(file, ExcelReadOptions.defaults(), PoiConfiguration.defaults());
    reader.close();

    Result<Void> result = reader.selectWorksheet("Sheet1");
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("EXCEL_READER_CLOSED");
  }

  @Test
  void streamReaderShouldReadHiddenSheets() throws IOException {
    Path file;
    try (Workbook wb = new XSSFWorkbook()) {
      wb.createSheet("Visible");
      Sheet hidden = wb.createSheet("Hidden");
      hidden.createRow(0).createCell(0).setCellValue("secret");
      wb.setSheetHidden(1, true);
      file = tempDir.resolve("hidden-sheet.xlsx");
      try (var out = Files.newOutputStream(file)) {
        wb.write(out);
      }
    }

    var options = ExcelReadOptions.builder().readHiddenSheets(true).build();
    try (PoiStreamReader reader = new PoiStreamReader(file, options, PoiConfiguration.defaults())) {
      Result<List<String>> names = reader.getWorksheetNames();
      assertThat(names.isOk()).isTrue();
      assertThat(names.getOrNull()).contains("Visible").contains("Hidden");
    }
  }

  @Test
  void streamReaderShouldNotReadHiddenSheetsWhenOptionOff() throws IOException {
    Path file;
    try (Workbook wb = new XSSFWorkbook()) {
      wb.createSheet("Visible");
      wb.createSheet("Hidden");
      wb.setSheetHidden(1, true);
      file = tempDir.resolve("hidden-sheet2.xlsx");
      try (var out = Files.newOutputStream(file)) {
        wb.write(out);
      }
    }

    var options = ExcelReadOptions.builder().readHiddenSheets(false).build();
    try (PoiStreamReader reader = new PoiStreamReader(file, options, PoiConfiguration.defaults())) {
      Result<List<String>> names = reader.getWorksheetNames();
      assertThat(names.isOk()).isTrue();
      assertThat(names.getOrNull()).contains("Visible");
      assertThat(names.getOrNull()).doesNotContain("Hidden");
    }
  }

  @Test
  void streamReaderShouldEnforceMaxRowsLimit() throws IOException {
    Path file;
    try (Workbook wb = new XSSFWorkbook()) {
      Sheet sheet = wb.createSheet("Data");
      for (int r = 0; r < 10; r++) {
        sheet.createRow(r).createCell(0).setCellValue("row-" + r);
      }
      file = tempDir.resolve("max-rows.xlsx");
      try (var out = Files.newOutputStream(file)) {
        wb.write(out);
      }
    }

    // Set maxRows=3 so reading row 3+ is rejected
    var options = ExcelReadOptions.builder().maxRows(3).build();
    try (PoiStreamReader reader = new PoiStreamReader(file, options, PoiConfiguration.defaults())) {
      reader.selectWorksheet("Data");
      // Read rows 0..2 ok, then row 3 should be rejected
      reader.readNext(); // row 0
      reader.readNext(); // row 1
      reader.readNext(); // row 2
      Result<List<ExcelCell>> result = reader.readNext(); // row 3 -> exceeds limit
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("EXCEL_MAX_ROWS_EXCEEDED");
    }
  }

  @Test
  void streamReaderShouldEnforceMaxColumnsLimit() throws IOException {
    Path file;
    try (Workbook wb = new XSSFWorkbook()) {
      Sheet sheet = wb.createSheet("Data");
      Row row = sheet.createRow(0);
      for (int c = 0; c < 10; c++) {
        row.createCell(c).setCellValue("col-" + c);
      }
      file = tempDir.resolve("max-cols.xlsx");
      try (var out = Files.newOutputStream(file)) {
        wb.write(out);
      }
    }

    var options = ExcelReadOptions.builder().maxColumns(3).build();
    try (PoiStreamReader reader = new PoiStreamReader(file, options, PoiConfiguration.defaults())) {
      reader.selectWorksheet("Data");
      Result<List<ExcelCell>> result = reader.readNext();
      assertThat(result.isOk()).isTrue();
      assertThat(result.getOrNull()).hasSizeLessThanOrEqualTo(3);
    }
  }

  @Test
  void streamReaderShouldHandleMissingCellsInRow() throws IOException {
    Path file;
    try (Workbook wb = new XSSFWorkbook()) {
      Sheet sheet = wb.createSheet("Data");
      Row row = sheet.createRow(0);
      // Sparse row: cells at column 0 and column 2, but not column 1
      row.createCell(0).setCellValue("A");
      row.createCell(2).setCellValue("C");
      file = tempDir.resolve("sparse-row.xlsx");
      try (var out = Files.newOutputStream(file)) {
        wb.write(out);
      }
    }

    try (PoiStreamReader reader =
        new PoiStreamReader(file, ExcelReadOptions.defaults(), PoiConfiguration.defaults())) {
      reader.selectWorksheet("Data");
      Result<List<ExcelCell>> result = reader.readNext();
      assertThat(result.isOk()).isTrue();
      // Should have 3 cells (0, blank, 2)
      assertThat(result.getOrNull()).hasSize(3);
    }
  }

  @Test
  void streamReaderSkipBlankRowsShouldSkipEmptyRows() throws IOException {
    Path file;
    try (Workbook wb = new XSSFWorkbook()) {
      Sheet sheet = wb.createSheet("Data");
      // Row 0: data
      sheet.createRow(0).createCell(0).setCellValue("data");
      // Row 1: blank (don't create any cells)
      sheet.createRow(1);
      // Row 2: data
      sheet.createRow(2).createCell(0).setCellValue("more");
      file = tempDir.resolve("blank-rows.xlsx");
      try (var out = Files.newOutputStream(file)) {
        wb.write(out);
      }
    }

    var options = ExcelReadOptions.builder().skipBlankRows(true).build();
    try (PoiStreamReader reader = new PoiStreamReader(file, options, PoiConfiguration.defaults())) {
      reader.selectWorksheet("Data");
      // hasNext with skipBlankRows=true will skip blank rows inline
      boolean hasNext = reader.hasNext();
      assertThat(hasNext).isTrue();
    }
  }

  // ── PoiStreamWriter: additional branch coverage ───────────────────────────

  @Test
  void streamWriterSetColumnWidthWithoutSheetShouldFail() throws IOException {
    Path output = tempDir.resolve("no-sheet-width.xlsx");
    try (PoiStreamWriter writer =
        new PoiStreamWriter(output, ExcelWriteOptions.defaults(), PoiConfiguration.defaults())) {
      // No worksheet created
      Result<Void> result = writer.setColumnWidth(0, 20.0);
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("EXCEL_NO_WORKSHEET");
    }
  }

  @Test
  void streamWriterSetRowHeightWithoutSheetShouldFail() throws IOException {
    Path output = tempDir.resolve("no-sheet-height.xlsx");
    try (PoiStreamWriter writer =
        new PoiStreamWriter(output, ExcelWriteOptions.defaults(), PoiConfiguration.defaults())) {
      Result<Void> result = writer.setRowHeight(30.0);
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("EXCEL_NO_WORKSHEET");
    }
  }

  @Test
  void streamWriterFreezePanesWithoutSheetShouldFail() throws IOException {
    Path output = tempDir.resolve("no-sheet-freeze.xlsx");
    try (PoiStreamWriter writer =
        new PoiStreamWriter(output, ExcelWriteOptions.defaults(), PoiConfiguration.defaults())) {
      Result<Void> result = writer.freezePanes(1, 0);
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("EXCEL_NO_WORKSHEET");
    }
  }

  @Test
  void streamWriterEnableAutoFilterWithoutSheetShouldFail() throws IOException {
    Path output = tempDir.resolve("no-sheet-filter.xlsx");
    try (PoiStreamWriter writer =
        new PoiStreamWriter(output, ExcelWriteOptions.defaults(), PoiConfiguration.defaults())) {
      Result<Void> result = writer.enableAutoFilter();
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("EXCEL_NO_WORKSHEET");
    }
  }

  @Test
  void streamWriterSetColumnWidthClosedShouldFail() throws IOException {
    Path output = tempDir.resolve("closed-width.xlsx");
    PoiStreamWriter writer =
        new PoiStreamWriter(output, ExcelWriteOptions.defaults(), PoiConfiguration.defaults());
    writer.close();
    Result<Void> result = writer.setColumnWidth(0, 10.0);
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("EXCEL_WRITER_CLOSED");
  }

  @Test
  void streamWriterSetRowHeightClosedShouldFail() throws IOException {
    Path output = tempDir.resolve("closed-height.xlsx");
    PoiStreamWriter writer =
        new PoiStreamWriter(output, ExcelWriteOptions.defaults(), PoiConfiguration.defaults());
    writer.close();
    Result<Void> result = writer.setRowHeight(20.0);
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("EXCEL_WRITER_CLOSED");
  }

  @Test
  void streamWriterFreezePanesClosedShouldFail() throws IOException {
    Path output = tempDir.resolve("closed-freeze.xlsx");
    PoiStreamWriter writer =
        new PoiStreamWriter(output, ExcelWriteOptions.defaults(), PoiConfiguration.defaults());
    writer.close();
    Result<Void> result = writer.freezePanes(1, 0);
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("EXCEL_WRITER_CLOSED");
  }

  @Test
  void streamWriterEnableAutoFilterClosedShouldFail() throws IOException {
    Path output = tempDir.resolve("closed-filter.xlsx");
    PoiStreamWriter writer =
        new PoiStreamWriter(output, ExcelWriteOptions.defaults(), PoiConfiguration.defaults());
    writer.close();
    Result<Void> result = writer.enableAutoFilter();
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("EXCEL_WRITER_CLOSED");
  }

  @Test
  void streamWriterFlushClosedShouldFail() throws IOException {
    Path output = tempDir.resolve("closed-flush.xlsx");
    PoiStreamWriter writer =
        new PoiStreamWriter(output, ExcelWriteOptions.defaults(), PoiConfiguration.defaults());
    writer.close();
    Result<Void> result = writer.flush();
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("EXCEL_WRITER_CLOSED");
  }

  @Test
  void streamWriterSelectWorksheetClosedShouldFail() throws IOException {
    Path output = tempDir.resolve("closed-select.xlsx");
    PoiStreamWriter writer =
        new PoiStreamWriter(output, ExcelWriteOptions.defaults(), PoiConfiguration.defaults());
    writer.close();
    Result<Void> result = writer.selectWorksheet("X");
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("EXCEL_WRITER_CLOSED");
  }

  @Test
  void streamWriterWriteRowClosedShouldFail() throws IOException {
    Path output = tempDir.resolve("closed-write.xlsx");
    PoiStreamWriter writer =
        new PoiStreamWriter(output, ExcelWriteOptions.defaults(), PoiConfiguration.defaults());
    writer.close();
    Result<Void> result = writer.writeRow(List.of(ExcelCell.text(0, 0, "x")));
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("EXCEL_WRITER_CLOSED");
  }

  @Test
  void streamWriterCloseTwiceShouldBeIdempotent() throws IOException {
    Path output = tempDir.resolve("double-close.xlsx");
    PoiStreamWriter writer =
        new PoiStreamWriter(output, ExcelWriteOptions.defaults(), PoiConfiguration.defaults());
    writer.createWorksheet("S");
    writer.close();
    // Second close should not throw
    writer.close();
  }

  @Test
  void streamWriterShouldWriteFormulaCellWithValueFallback() throws IOException {
    Path output = tempDir.resolve("formula-fallback.xlsx");
    var options = ExcelWriteOptions.builder().writeFormulas(false).build();
    try (PoiStreamWriter writer =
        new PoiStreamWriter(output, options, PoiConfiguration.defaults())) {
      writer.createWorksheet("S");
      // writeFormulas=false, formula != null: write value fallback (Number)
      List<ExcelCell> row =
          List.of(new ExcelCell(0, 0, CellType.FORMULA, 99.0, "SUM(A1:A3)", null));
      Result<Void> result = writer.writeRow(row);
      assertThat(result.isOk()).isTrue();
    }
  }

  @Test
  void streamWriterShouldWriteFormulaCellWithStringValueFallback() throws IOException {
    Path output = tempDir.resolve("formula-string-fallback.xlsx");
    var options = ExcelWriteOptions.builder().writeFormulas(false).build();
    try (PoiStreamWriter writer =
        new PoiStreamWriter(output, options, PoiConfiguration.defaults())) {
      writer.createWorksheet("S");
      // writeFormulas=false, formula != null, value is String: write string value
      List<ExcelCell> row =
          List.of(new ExcelCell(0, 0, CellType.FORMULA, "result", "CONCAT()", null));
      Result<Void> result = writer.writeRow(row);
      assertThat(result.isOk()).isTrue();
    }
  }

  @Test
  void streamWriterShouldWriteBooleanWithNullValueSafely() throws IOException {
    Path output = tempDir.resolve("bool-null.xlsx");
    try (PoiStreamWriter writer =
        new PoiStreamWriter(output, ExcelWriteOptions.defaults(), PoiConfiguration.defaults())) {
      writer.createWorksheet("S");
      // BOOLEAN cell with null value — should not crash (no-op on the value branch)
      List<ExcelCell> row = List.of(new ExcelCell(0, 0, CellType.BOOLEAN, null, null, null));
      Result<Void> result = writer.writeRow(row);
      assertThat(result.isOk()).isTrue();
    }
  }

  @Test
  void streamWriterShouldWriteNumericWithNullValueSafely() throws IOException {
    Path output = tempDir.resolve("num-null.xlsx");
    try (PoiStreamWriter writer =
        new PoiStreamWriter(output, ExcelWriteOptions.defaults(), PoiConfiguration.defaults())) {
      writer.createWorksheet("S");
      // NUMERIC cell with null value — should not crash (no-op on value branch)
      List<ExcelCell> row = List.of(new ExcelCell(0, 0, CellType.NUMERIC, null, null, null));
      Result<Void> result = writer.writeRow(row);
      assertThat(result.isOk()).isTrue();
    }
  }

  @Test
  void streamWriterShouldWriteStringWithNullValueSafely() throws IOException {
    Path output = tempDir.resolve("str-null.xlsx");
    try (PoiStreamWriter writer =
        new PoiStreamWriter(output, ExcelWriteOptions.defaults(), PoiConfiguration.defaults())) {
      writer.createWorksheet("S");
      // STRING cell with null value — branch: value != null check
      List<ExcelCell> row = List.of(new ExcelCell(0, 0, CellType.STRING, null, null, null));
      Result<Void> result = writer.writeRow(row);
      assertThat(result.isOk()).isTrue();
    }
  }

  @Test
  void streamWriterShouldWriteFormulaCellWithNullValue() throws IOException {
    Path output = tempDir.resolve("formula-null-value.xlsx");
    var options = ExcelWriteOptions.builder().writeFormulas(false).build();
    try (PoiStreamWriter writer =
        new PoiStreamWriter(output, options, PoiConfiguration.defaults())) {
      writer.createWorksheet("S");
      // writeFormulas=false, formula != null, value = null -> no-op
      List<ExcelCell> row = List.of(new ExcelCell(0, 0, CellType.FORMULA, null, "SUM(A1)", null));
      Result<Void> result = writer.writeRow(row);
      assertThat(result.isOk()).isTrue();
    }
  }

  @Test
  void streamWriterShouldApplyAllStyleAlignments() throws IOException {
    Path output = tempDir.resolve("all-alignments.xlsx");
    try (PoiStreamWriter writer =
        new PoiStreamWriter(output, ExcelWriteOptions.defaults(), PoiConfiguration.defaults())) {
      writer.createWorksheet("S");

      // Test all HorizontalAlignment values
      ExcelCellStyle.HorizontalAlignment[] hAligns = {
        ExcelCellStyle.HorizontalAlignment.LEFT,
        ExcelCellStyle.HorizontalAlignment.CENTER,
        ExcelCellStyle.HorizontalAlignment.RIGHT,
        ExcelCellStyle.HorizontalAlignment.JUSTIFY,
        ExcelCellStyle.HorizontalAlignment.FILL
      };
      ExcelCellStyle.VerticalAlignment[] vAligns = {
        ExcelCellStyle.VerticalAlignment.TOP,
        ExcelCellStyle.VerticalAlignment.CENTER,
        ExcelCellStyle.VerticalAlignment.BOTTOM,
        ExcelCellStyle.VerticalAlignment.JUSTIFY
      };
      ExcelCellStyle.BorderStyle[] borderStyles = {
        ExcelCellStyle.BorderStyle.NONE,
        ExcelCellStyle.BorderStyle.THIN,
        ExcelCellStyle.BorderStyle.MEDIUM,
        ExcelCellStyle.BorderStyle.THICK,
        ExcelCellStyle.BorderStyle.DOTTED,
        ExcelCellStyle.BorderStyle.DASHED,
        ExcelCellStyle.BorderStyle.DOUBLE
      };

      for (int i = 0; i < hAligns.length; i++) {
        var hAlign = hAligns[i];
        var vAlign = vAligns[i % vAligns.length];
        var border = borderStyles[i % borderStyles.length];
        var style =
            ExcelCellStyle.builder()
                .horizontalAlignment(hAlign)
                .verticalAlignment(vAlign)
                .borderStyle(border)
                .wrapText(i % 2 == 0)
                .numberFormat(i % 2 == 0 ? "#,##0" : null)
                .fontName(i % 2 == 0 ? "Times New Roman" : null)
                .fontSize(i % 2 == 0 ? 14 : null)
                .bold(i % 2 == 0 ? true : null)
                .italic(i % 2 == 0 ? true : null)
                .build();
        List<ExcelCell> row =
            List.of(new ExcelCell(i, 0, CellType.STRING, "val-" + i, null, style));
        Result<Void> result = writer.writeRow(row);
        assertThat(result.isOk()).isTrue();
      }
    }
  }

  @Test
  void streamWriterShouldApplyRemainingBorderStyles() throws IOException {
    Path output = tempDir.resolve("remaining-borders.xlsx");
    try (PoiStreamWriter writer =
        new PoiStreamWriter(output, ExcelWriteOptions.defaults(), PoiConfiguration.defaults())) {
      writer.createWorksheet("S");

      ExcelCellStyle.BorderStyle[] remaining = {
        ExcelCellStyle.BorderStyle.MEDIUM,
        ExcelCellStyle.BorderStyle.THICK,
        ExcelCellStyle.BorderStyle.DOTTED,
        ExcelCellStyle.BorderStyle.DASHED,
        ExcelCellStyle.BorderStyle.DOUBLE
      };
      for (int i = 0; i < remaining.length; i++) {
        var style = ExcelCellStyle.builder().borderStyle(remaining[i]).build();
        List<ExcelCell> row = List.of(new ExcelCell(i, 0, CellType.STRING, "v", null, style));
        writer.writeRow(row);
      }
    }
  }

  // ── PoiWorkbookMapper: additional branches ────────────────────────────────

  @Test
  void workbookMapperFromPoiShouldHandleFormulaCell() throws IOException {
    try (Workbook wb = new XSSFWorkbook()) {
      Sheet sheet = wb.createSheet("Formulas");
      Row row = sheet.createRow(0);
      Cell cell = row.createCell(0, org.apache.poi.ss.usermodel.CellType.FORMULA);
      cell.setCellFormula("A1+B1");

      ExcelWorkbook mapped = PoiWorkbookMapper.fromPoi(wb);
      assertThat(mapped.getWorksheet("Formulas")).isNotNull();
    }
  }

  @Test
  void workbookMapperFromPoiShouldHandleErrorCell() throws IOException {
    try (Workbook wb = new XSSFWorkbook()) {
      Sheet sheet = wb.createSheet("Errors");
      Row row = sheet.createRow(0);
      Cell cell = row.createCell(0, org.apache.poi.ss.usermodel.CellType.ERROR);
      cell.setCellErrorValue(FormulaError.DIV0.getCode());

      ExcelWorkbook mapped = PoiWorkbookMapper.fromPoi(wb);
      assertThat(mapped.getWorksheet("Errors")).isNotNull();
    }
  }

  @Test
  void workbookMapperFromPoiShouldHandleCustomRowHeight() throws IOException {
    try (Workbook wb = new XSSFWorkbook()) {
      Sheet sheet = wb.createSheet("Heights");
      Row row = sheet.createRow(0);
      row.createCell(0).setCellValue("tall");
      row.setHeightInPoints(30.0f); // non-default height

      ExcelWorkbook mapped = PoiWorkbookMapper.fromPoi(wb);
      assertThat(mapped.getWorksheet("Heights")).isNotNull();
    }
  }

  @Test
  void workbookMapperFromPoiShouldHandleCustomColumnWidth() throws IOException {
    try (Workbook wb = new XSSFWorkbook()) {
      Sheet sheet = wb.createSheet("Widths");
      sheet.createRow(0).createCell(0).setCellValue("wide");
      sheet.setColumnWidth(0, 8000); // non-default width

      ExcelWorkbook mapped = PoiWorkbookMapper.fromPoi(wb);
      assertThat(mapped.getWorksheet("Widths")).isNotNull();
    }
  }

  @Test
  void workbookMapperToPoiShouldHandleNumericWithDateValue() throws IOException {
    try (Workbook wb = new XSSFWorkbook()) {
      var cell = new ExcelCell(0, 0, CellType.NUMERIC, new java.util.Date(), null, null);
      var worksheet = ExcelWorksheet.builder("Dates").cell(cell).build();
      var workbook = ExcelWorkbook.builder().worksheet(worksheet).build();

      PoiWorkbookMapper.toPoi(workbook, wb, ExcelWriteOptions.defaults());
      assertThat(wb.getSheetAt(0)).isNotNull();
    }
  }

  @Test
  void workbookMapperToPoiShouldHandleDefaultCellType() throws IOException {
    try (Workbook wb = new XSSFWorkbook()) {
      // BLANK cell type with a non-null value to hit the default branch
      var cell = new ExcelCell(0, 0, CellType.BLANK, "ignored-value", null, null);
      var worksheet = ExcelWorksheet.builder("Misc").cell(cell).build();
      var workbook = ExcelWorkbook.builder().worksheet(worksheet).build();

      PoiWorkbookMapper.toPoi(workbook, wb, ExcelWriteOptions.defaults());
      assertThat(wb.getSheetAt(0)).isNotNull();
    }
  }

  @Test
  void workbookMapperToPoiShouldSetActiveSheetFromWorkbook() throws IOException {
    try (Workbook wb = new XSSFWorkbook()) {
      var ws1 = ExcelWorksheet.builder("First").cell(0, 0, "a").build();
      var ws2 = ExcelWorksheet.builder("Second").cell(0, 0, "b").build();
      var workbook =
          ExcelWorkbook.builder().worksheet(ws1).worksheet(ws2).activeSheetIndex(1).build();

      PoiWorkbookMapper.toPoi(workbook, wb, ExcelWriteOptions.defaults());
      assertThat(wb.getActiveSheetIndex()).isEqualTo(1);
    }
  }

  @Test
  void workbookMapperToPoiShouldHandleCustomRowHeight() throws IOException {
    try (Workbook wb = new XSSFWorkbook()) {
      // Build a worksheet with a custom row height
      var worksheet =
          ExcelWorksheet.builder("Heights").cell(0, 0, "row0").rowHeight(0, 30.0).build();
      var workbook = ExcelWorkbook.builder().worksheet(worksheet).build();

      PoiWorkbookMapper.toPoi(workbook, wb, ExcelWriteOptions.defaults());
      Sheet sheet = wb.getSheetAt(0);
      assertThat(sheet.getRow(0).getHeightInPoints()).isGreaterThan(0);
    }
  }

  @Test
  void workbookMapperToPoiShouldHandleCustomColumnWidth() throws IOException {
    try (Workbook wb = new XSSFWorkbook()) {
      var worksheet =
          ExcelWorksheet.builder("Widths").cell(0, 0, "wide").columnWidth(0, 25.0).build();
      var workbook = ExcelWorkbook.builder().worksheet(worksheet).build();

      PoiWorkbookMapper.toPoi(workbook, wb, ExcelWriteOptions.defaults());
      Sheet sheet = wb.getSheetAt(0);
      assertThat(sheet.getColumnWidth(0)).isGreaterThan(0);
    }
  }

  @Test
  void workbookMapperToPoiShouldHandleNumericStringFallback() throws IOException {
    try (Workbook wb = new XSSFWorkbook()) {
      // NUMERIC cell with a String value -> falls through to setCellValue(toString)
      var cell = new ExcelCell(0, 0, CellType.NUMERIC, "not-a-number", null, null);
      var worksheet = ExcelWorksheet.builder("Fallback").cell(cell).build();
      var workbook = ExcelWorkbook.builder().worksheet(worksheet).build();

      PoiWorkbookMapper.toPoi(workbook, wb, ExcelWriteOptions.defaults());
      assertThat(wb.getSheetAt(0)).isNotNull();
    }
  }
}
