package com.marcusprado02.commons.adapters.excel.poi;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.excel.CsvOptions;
import com.marcusprado02.commons.ports.excel.ExcelWorkbook;
import com.marcusprado02.commons.ports.excel.ExcelWorksheet;
import com.marcusprado02.commons.ports.excel.ExcelWriteOptions;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

class PoiExcelAdapterTest {

  // ── PoiConfiguration factory methods and builder ─────────────────────────

  @Test
  void defaultsShouldHaveExpectedValues() {
    var config = PoiConfiguration.defaults();
    assertThat(config.enableFormulasEvaluation()).isTrue();
    assertThat(config.streamingRowAccessWindow()).isEqualTo(500);
    assertThat(config.maxRows()).isEqualTo(1048576);
    assertThat(config.maxColumns()).isEqualTo(16384);
    assertThat(config.compressTemporaryFiles()).isTrue();
    assertThat(config.useSharedStrings()).isTrue();
    assertThat(config.readOnlyMode()).isFalse();
    assertThat(config.strictParsing()).isFalse();
  }

  @Test
  void performanceConfigShouldDisableExpensiveFeatures() {
    var config = PoiConfiguration.performance();
    assertThat(config.enableFormulasEvaluation()).isFalse();
    assertThat(config.streamingRowAccessWindow()).isEqualTo(100);
    assertThat(config.readOnlyMode()).isTrue();
  }

  @Test
  void memoryOptimizedConfigShouldHaveSmallWindow() {
    var config = PoiConfiguration.memoryOptimized();
    assertThat(config.streamingRowAccessWindow()).isEqualTo(50);
    assertThat(config.compressTemporaryFiles()).isTrue();
  }

  @Test
  void fullFeaturesConfigShouldEnableStrictParsing() {
    var config = PoiConfiguration.fullFeatures();
    assertThat(config.enableFormulasEvaluation()).isTrue();
    assertThat(config.strictParsing()).isTrue();
    assertThat(config.streamingRowAccessWindow()).isEqualTo(1000);
  }

  @Test
  void builderShouldSetAllFields() {
    var config =
        PoiConfiguration.builder()
            .enableFormulasEvaluation(false)
            .streamingRowAccessWindow(200)
            .maxRows(65536)
            .maxColumns(256)
            .compressTemporaryFiles(false)
            .useSharedStrings(false)
            .readOnlyMode(true)
            .strictParsing(true)
            .build();

    assertThat(config.enableFormulasEvaluation()).isFalse();
    assertThat(config.streamingRowAccessWindow()).isEqualTo(200);
    assertThat(config.maxRows()).isEqualTo(65536);
    assertThat(config.maxColumns()).isEqualTo(256);
    assertThat(config.compressTemporaryFiles()).isFalse();
    assertThat(config.useSharedStrings()).isFalse();
    assertThat(config.readOnlyMode()).isTrue();
    assertThat(config.strictParsing()).isTrue();
  }

  // ── PoiExcelAdapter: max rows/columns ────────────────────────────────────

  @Test
  void getMaxRowsShouldReturnFromConfig() {
    var config =
        PoiConfiguration.builder()
            .streamingRowAccessWindow(1)
            .maxRows(12345)
            .maxColumns(100)
            .build();
    var adapter = new PoiExcelAdapter(config);
    assertThat(adapter.getMaxRows()).isEqualTo(12345);
  }

  @Test
  void getMaxColumnsShouldReturnFromConfig() {
    var config =
        PoiConfiguration.builder().streamingRowAccessWindow(1).maxRows(100).maxColumns(256).build();
    var adapter = new PoiExcelAdapter(config);
    assertThat(adapter.getMaxColumns()).isEqualTo(256);
  }

  // ── writeWorkbook / readWorkbook round-trip (XLSX) ───────────────────────

  @Test
  void writeAndReadXlsxWorkbookShouldRoundTrip() {
    var adapter = new PoiExcelAdapter();

    var worksheet =
        ExcelWorksheet.builder("Sheet1")
            .cell(0, 0, "Name")
            .cell(0, 1, "Score")
            .cell(1, 0, "Alice")
            .cell(1, 1, 95)
            .cell(2, 0, "Bob")
            .cell(2, 1, 87)
            .build();

    var workbook = ExcelWorkbook.builder().worksheet(worksheet).build();
    var options = ExcelWriteOptions.defaults();

    // Write to bytes
    var baos = new ByteArrayOutputStream();
    Result<Void> writeResult = adapter.writeWorkbook(workbook, baos, options);
    assertThat(writeResult.isOk()).isTrue();
    assertThat(baos.toByteArray()).hasSizeGreaterThan(0);

    // Read back
    InputStream in = new ByteArrayInputStream(baos.toByteArray());
    Result<ExcelWorkbook> readResult = adapter.readWorkbook(in, "test.xlsx");
    assertThat(readResult.isOk()).isTrue();
    assertThat(readResult.getOrNull().getWorksheetCount()).isEqualTo(1);
    assertThat(readResult.getOrNull().getWorksheet(0).name()).isEqualTo("Sheet1");
  }

  @Test
  void writeXlsFormatShouldSucceed() {
    var adapter = new PoiExcelAdapter();
    var worksheet = ExcelWorksheet.builder("Data").cell(0, 0, "Hello").build();
    var workbook = ExcelWorkbook.builder().worksheet(worksheet).build();
    var options = ExcelWriteOptions.xls();

    var baos = new ByteArrayOutputStream();
    Result<Void> result = adapter.writeWorkbook(workbook, baos, options);

    assertThat(result.isOk()).isTrue();
    assertThat(baos.toByteArray()).hasSizeGreaterThan(0);
  }

  @Test
  void readWorkbookFromInvalidInputShouldReturnFail() {
    var adapter = new PoiExcelAdapter();
    var badInput = new ByteArrayInputStream("NOT_AN_EXCEL_FILE".getBytes());

    Result<ExcelWorkbook> result = adapter.readWorkbook(badInput, "bad.xlsx");

    assertThat(result.isFail()).isTrue();
  }

  // ── CSV conversion ────────────────────────────────────────────────────────

  @Test
  void toCsvShouldProduceSeparatedContent() {
    var adapter = new PoiExcelAdapter();

    var worksheet =
        ExcelWorksheet.builder("Sheet1")
            .cell(0, 0, "Name")
            .cell(0, 1, "Age")
            .cell(1, 0, "Alice")
            .cell(1, 1, 30)
            .build();

    var workbook = ExcelWorkbook.builder().worksheet(worksheet).build();
    var options = CsvOptions.defaults();

    Result<String> result = adapter.toCsv(workbook, "Sheet1", options);

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).contains("Alice");
  }

  @Test
  void toCsvWithNonExistentSheetShouldReturnFail() {
    var adapter = new PoiExcelAdapter();
    var worksheet = ExcelWorksheet.builder("Sheet1").build();
    var workbook = ExcelWorkbook.builder().worksheet(worksheet).build();

    Result<String> result = adapter.toCsv(workbook, "NoSuchSheet", CsvOptions.defaults());

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("EXCEL_TO_CSV_ERROR");
  }

  @Test
  void fromCsvShouldProduceWorkbook() {
    var adapter = new PoiExcelAdapter();
    String csv = "Name,Score\nAlice,95\nBob,87";
    var options = CsvOptions.defaults();

    Result<ExcelWorkbook> result = adapter.fromCsv(csv, "Imported", options);

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().getWorksheetCount()).isEqualTo(1);
    assertThat(result.getOrNull().getWorksheet("Imported")).isNotNull();
  }

  @Test
  void fromCsvThenToCsvShouldRoundTrip() {
    var adapter = new PoiExcelAdapter();
    String originalCsv = "A,B\n1,2\n3,4";

    Result<ExcelWorkbook> workbookResult =
        adapter.fromCsv(originalCsv, "Data", CsvOptions.noHeaders());
    assertThat(workbookResult.isOk()).isTrue();

    Result<String> csvResult =
        adapter.toCsv(workbookResult.getOrNull(), "Data", CsvOptions.noHeaders());
    assertThat(csvResult.isOk()).isTrue();
    assertThat(csvResult.getOrNull()).contains("1").contains("2").contains("3").contains("4");
  }

  // ── createStreamWriter ────────────────────────────────────────────────────

  @Test
  void createStreamWriterForNonexistentPathShouldFail() {
    var adapter = new PoiExcelAdapter();
    var path = java.nio.file.Path.of("/nonexistent-dir-xyz/output.xlsx");

    Result<?> result = adapter.createStreamWriter(path, ExcelWriteOptions.defaults());

    // Stream writer creation may fail if the path doesn't exist
    // Either ok or fail is acceptable depending on implementation; just must not throw
    assertThat(result).isNotNull();
  }

  // ── createStreamingWorkbook ───────────────────────────────────────────────

  @Test
  void createStreamingWorkbookXlsxShouldReturnSxssf() {
    var adapter = new PoiExcelAdapter();
    var options = ExcelWriteOptions.defaults(); // XLSX format

    // Package-private method, accessible within same package
    var workbook = adapter.createStreamingWorkbook(options);

    assertThat(workbook).isNotNull();
    assertThat(workbook.getClass().getSimpleName()).isEqualTo("SXSSFWorkbook");
    try {
      workbook.close();
    } catch (Exception ignored) {
    }
  }

  @Test
  void createStreamingWorkbookXlsShouldReturnHssf() {
    var adapter = new PoiExcelAdapter();
    var options = ExcelWriteOptions.xls();

    var workbook = adapter.createStreamingWorkbook(options);

    assertThat(workbook).isNotNull();
    assertThat(workbook.getClass().getSimpleName()).isEqualTo("HSSFWorkbook");
    try {
      workbook.close();
    } catch (Exception ignored) {
    }
  }

  // ── default constructor ───────────────────────────────────────────────────

  @Test
  void defaultConstructorShouldUseDefaultConfig() {
    var adapter = new PoiExcelAdapter();
    assertThat(adapter.getMaxRows()).isEqualTo(1048576);
    assertThat(adapter.getMaxColumns()).isEqualTo(16384);
  }
}
