# Commons Ports - Excel Processing

Port interface for Excel spreadsheet processing and generation in Java applications.

## Overview

This module provides platform-agnostic abstractions for working with Excel files (.xlsx, .xls), including reading, writing, streaming operations, and CSV conversion. It follows the Result pattern for type-safe error handling and supports both in-memory and streaming operations for handling large files.

## Features

- **Multiple Formats**: Support for Excel 2007+ (.xlsx) and Excel 97-2003 (.xls)
- **Streaming Operations**: Memory-efficient processing of large Excel files
- **CSV Conversion**: Bidirectional conversion between Excel and CSV formats
- **Rich Formatting**: Cell styles, fonts, colors, borders, and alignment
- **Formula Support**: Reading and writing Excel formulas
- **Data Types**: Text, numbers, booleans, dates, and formulas
- **File Validation**: Structure and format validation
- **Password Protection**: Support for encrypted Excel files
- **Type Safety**: Result pattern for error handling

## Core Interfaces

### ExcelPort
Main interface for Excel operations:
```java
public interface ExcelPort {
    Result<ExcelWorkbook> readWorkbook(Path filePath);
    Result<Void> writeWorkbook(ExcelWorkbook workbook, Path filePath, ExcelWriteOptions options);
    Result<ExcelStreamReader> createStreamReader(Path filePath, ExcelReadOptions options);
    Result<ExcelStreamWriter> createStreamWriter(Path filePath, ExcelWriteOptions options);
    Result<String> toCsv(ExcelWorkbook workbook, String worksheetName, CsvOptions options);
    Result<ExcelWorkbook> fromCsv(String csvContent, String worksheetName, CsvOptions options);
}
```

### ExcelStreamReader
For memory-efficient reading of large files:
```java
public interface ExcelStreamReader extends AutoCloseable {
    Result<List<String>> getWorksheetNames();
    Result<Void> selectWorksheet(String worksheetName);
    boolean hasNext();
    Result<List<ExcelCell>> readNext();
    int getCurrentRowNum();
}
```

### ExcelStreamWriter
For memory-efficient writing of large files:
```java
public interface ExcelStreamWriter extends AutoCloseable {
    Result<Void> createWorksheet(String name);
    Result<Void> writeRow(List<ExcelCell> cells);
    Result<Void> writeRow(Object... values);
    Result<Void> setColumnWidth(int column, double width);
    Result<Void> freezePanes(int row, int column);
}
```

## Data Models

### ExcelWorkbook
Represents a complete Excel workbook:
```java
public record ExcelWorkbook(
    List<ExcelWorksheet> worksheets,
    int activeSheetIndex,
    Map<String, String> properties,
    String author,
    String title,
    String subject,
    String comments
) {
    public ExcelWorksheet getWorksheet(String name);
    public ExcelWorksheet getActiveWorksheet();
    public List<String> getWorksheetNames();
}
```

### ExcelWorksheet
Represents a single worksheet:
```java
public record ExcelWorksheet(
    String name,
    Map<String, ExcelCell> cells,
    Map<Integer, Double> columnWidths,
    Map<Integer, Double> rowHeights,
    int frozenRows,
    int frozenColumns,
    boolean autoFilter,
    String printArea
) {
    public ExcelCell getCell(int row, int column);
    public List<ExcelCell> getRow(int row);
    public String getUsedRange();
}
```

### ExcelCell
Represents a single cell with data and formatting:
```java
public record ExcelCell(
    int row,
    int column,
    CellType cellType,
    Object value,
    String formula,
    ExcelCellStyle style
) {
    public static ExcelCell text(int row, int column, String text);
    public static ExcelCell number(int row, int column, Number number);
    public static ExcelCell date(int row, int column, LocalDate date);
    public static ExcelCell formula(int row, int column, String formula);
    
    public String getStringValue();
    public double getNumericValue();
    public boolean getBooleanValue();
    public String getAddress(); // Returns A1 notation
}
```

### ExcelCellStyle
Comprehensive cell formatting:
```java
public record ExcelCellStyle(
    String fontName,
    Integer fontSize,
    Boolean bold,
    Boolean italic,
    Color fontColor,
    Color backgroundColor,
    HorizontalAlignment horizontalAlignment,
    VerticalAlignment verticalAlignment,
    String numberFormat,
    BorderStyle borderStyle
) {
    public static ExcelCellStyle headerStyle();
    public static ExcelCellStyle currencyStyle();
    public static ExcelCellStyle dateStyle();
}
```

## Configuration Options

### ExcelReadOptions
```java
var options = ExcelReadOptions.builder()
    .readAllSheets(true)
    .readFormulas(true)
    .skipBlankRows(true)
    .maxRows(10000)
    .password("secret")
    .build();
```

### ExcelWriteOptions
```java
var options = ExcelWriteOptions.builder()
    .format(ExcelFormat.XLSX)
    .compress(true)
    .password("secret")
    .autoSizeColumns(true)
    .creator("My Application")
    .build();
```

### CsvOptions
```java
var options = CsvOptions.builder()
    .delimiter(';')
    .includeHeaders(true)
    .encoding("UTF-8")
    .dateFormat("dd/MM/yyyy")
    .build();
```

## Usage Examples

### Reading an Excel File
```java
ExcelPort excelPort = // ... implementation

Result<ExcelWorkbook> result = excelPort.readWorkbook(Paths.get("data.xlsx"));

result.onSuccess(workbook -> {
    ExcelWorksheet sheet = workbook.getActiveWorksheet();
    ExcelCell cell = sheet.getCell(0, 0); // A1
    System.out.println("A1 value: " + cell.getStringValue());
    
    // Process all rows
    for (int row = 0; row <= sheet.getLastRowNum(); row++) {
        List<ExcelCell> rowCells = sheet.getRow(row);
        // Process row...
    }
});
```

### Writing an Excel File
```java
var workbook = ExcelWorkbook.builder()
    .title("Sales Report")
    .author("Sales Team")
    .worksheet(
        ExcelWorksheet.builder("Sales Data")
            .cell(ExcelCell.text(0, 0, "Product", ExcelCellStyle.headerStyle()))
            .cell(ExcelCell.text(0, 1, "Sales", ExcelCellStyle.headerStyle()))
            .cell(ExcelCell.text(1, 0, "Widget A"))
            .cell(ExcelCell.number(1, 1, 1500.00, ExcelCellStyle.currencyStyle()))
            .autoFilter(true)
            .freezePanes(1, 0)
            .build())
    .build();

ExcelWriteOptions options = ExcelWriteOptions.defaults();
Result<Void> result = excelPort.writeWorkbook(workbook, Paths.get("sales.xlsx"), options);
```

### Streaming Large Files
```java
// Reading large file
Result<ExcelStreamReader> readerResult = excelPort.createStreamReader(
    Paths.get("large-data.xlsx"), 
    ExcelReadOptions.streaming()
);

readerResult.onSuccess(reader -> {
    reader.selectFirstWorksheet();
    while (reader.hasNext()) {
        Result<List<ExcelCell>> row = reader.readNext();
        row.onSuccess(cells -> {
            // Process row efficiently
            cells.forEach(cell -> System.out.print(cell.getStringValue() + "\t"));
            System.out.println();
        });
    }
    reader.close();
});

// Writing large file
Result<ExcelStreamWriter> writerResult = excelPort.createStreamWriter(
    Paths.get("output.xlsx"), 
    ExcelWriteOptions.defaults()
);

writerResult.onSuccess(writer -> {
    writer.createWorksheet("Data");
    
    // Write header
    writer.writeRow("ID", "Name", "Value", "Date");
    
    // Write data rows
    for (int i = 0; i < 100000; i++) {
        writer.writeRow(i, "Item " + i, Math.random() * 1000, LocalDate.now());
    }
    
    writer.setColumnWidth(1, 20.0); // Name column wider
    writer.freezePanes(1, 0); // Freeze header row
    writer.close();
});
```

### CSV Conversion
```java
// Excel to CSV
Result<String> csvResult = excelPort.toCsv(
    workbook, 
    "Sheet1", 
    CsvOptions.semicolon()
);

csvResult.onSuccess(csvContent -> {
    Files.writeString(Paths.get("output.csv"), csvContent);
});

// CSV to Excel
String csvData = Files.readString(Paths.get("input.csv"));
Result<ExcelWorkbook> workbookResult = excelPort.fromCsv(
    csvData, 
    "Data", 
    CsvOptions.defaults()
);
```

### File Validation
```java
Result<ExcelValidationResult> validation = excelPort.validateFile(Paths.get("unknown.xlsx"));

validation.onSuccess(result -> {
    if (result.isValid()) {
        System.out.println("Format: " + result.format());
        System.out.println("Worksheets: " + result.worksheetCount());
        System.out.println("Encrypted: " + result.isEncrypted());
        System.out.println("Has Formulas: " + result.hasFormulas());
    } else {
        result.errors().forEach(System.out::println);
    }
});
```

### Advanced Cell Operations
```java
// Create formatted cells
var headerStyle = ExcelCellStyle.builder()
    .bold(true)
    .backgroundColor(Color.LIGHT_GRAY)
    .horizontalAlignment(HorizontalAlignment.CENTER)
    .borderStyle(BorderStyle.THIN)
    .build();

var currencyStyle = ExcelCellStyle.builder()
    .numberFormat("$#,##0.00")
    .horizontalAlignment(HorizontalAlignment.RIGHT)
    .build();

var worksheet = ExcelWorksheet.builder("Financial Report")
    .cell(ExcelCell.text(0, 0, "Account", headerStyle))
    .cell(ExcelCell.text(0, 1, "Balance", headerStyle))
    .cell(ExcelCell.text(1, 0, "Cash"))
    .cell(ExcelCell.number(1, 1, 25000.50, currencyStyle))
    .cell(ExcelCell.formula(2, 1, "SUM(B1:B2)", currencyStyle))
    .build();

// Working with dates
var dateStyle = ExcelCellStyle.dateStyle();
var dateTimeStyle = ExcelCellStyle.dateTimeStyle();

worksheet = ExcelWorksheet.builder("Dates")
    .cell(ExcelCell.date(0, 0, LocalDate.now(), dateStyle))
    .cell(ExcelCell.dateTime(1, 0, LocalDateTime.now(), dateTimeStyle))
    .build();
```

## Cell Types

The port supports all major Excel data types:

- **BLANK**: Empty cells
- **STRING**: Text values
- **NUMERIC**: Numbers, dates, and times
- **BOOLEAN**: True/false values  
- **FORMULA**: Excel formulas (=SUM(A1:A10))
- **ERROR**: Error values (#DIV/0!, #VALUE!, etc.)

## Column and Row Operations

```java
// Column operations
String columnLetter = ExcelCell.columnToLetter(0); // "A"
int columnIndex = ExcelCell.letterToColumn("AA"); // 26

// Cell addressing
ExcelCell cell = ExcelCell.text(0, 0, "Hello");
String address = cell.getAddress(); // "A1"

// Range operations
String range = worksheet.getUsedRange(); // "A1:C10"
```

## Error Handling

All operations return `Result<T>` for type-safe error handling:

```java
Result<ExcelWorkbook> result = excelPort.readWorkbook(filePath);

result
    .onSuccess(workbook -> {
        // Process successful result
        System.out.println("Loaded " + workbook.getWorksheetCount() + " sheets");
    })
    .onFailure(error -> {
        // Handle error
        System.err.println("Failed to load Excel file: " + error.getMessage());
    });

// Or use pattern matching (Java 17+)
switch (result) {
    case Result.Success<ExcelWorkbook> success -> processWorkbook(success.value());
    case Result.Failure<ExcelWorkbook> failure -> handleError(failure.problem());
}
```

## Memory Considerations

- **In-Memory Operations**: Suitable for files up to ~10MB or ~100K cells
- **Streaming Operations**: For larger files, use `ExcelStreamReader`/`ExcelStreamWriter`
- **Batch Processing**: Process data in chunks when possible
- **Resource Management**: Always close streams and readers

## Thread Safety

- **Data Models**: All value objects (`ExcelWorkbook`, `ExcelWorksheet`, `ExcelCell`) are immutable and thread-safe
- **Port Interface**: Implementations should be thread-safe for concurrent operations
- **Streaming**: Each stream instance should be used by a single thread

## Dependencies

This module depends on:
- `commons-kernel-result` - Result pattern for error handling
- `commons-kernel-errors` - Common error definitions

## Implementation Notes

Implementations should:
1. Support both .xlsx (OOXML) and .xls (BIFF) formats
2. Handle large files efficiently with streaming APIs
3. Preserve formatting information when possible
4. Support password-protected files
5. Validate file structure and report meaningful errors
6. Follow Excel's data type conversion rules
7. Handle formula evaluation appropriately

For streaming operations:
- Minimize memory usage by processing rows one at a time
- Support worksheet selection without loading all sheets
- Provide accurate row counting and navigation
- Handle errors gracefully during streaming

The port abstraction allows different Excel processing libraries (Apache POI, FastExcel, etc.) to be used interchangeably while maintaining the same API contract.
