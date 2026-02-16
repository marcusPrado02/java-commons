# Commons Adapters - Excel Processing (Apache POI)

Apache POI implementation of the Excel processing port interface for robust Excel file handling in Java applications.

## Overview

This module provides a comprehensive implementation of the `ExcelPort` interface using Apache POI 5.2.5, supporting both legacy (.xls) and modern (.xlsx) Excel formats with advanced features like streaming operations, formula evaluation, and CSV conversion.

## Features

### Core Functionality
- **Complete Format Support**: Excel 2007+ (.xlsx), Excel 97-2003 (.xls), Excel Macro-Enabled (.xlsm)
- **Streaming Operations**: Memory-efficient processing using SXSSF for large files
- **Formula Support**: Full formula evaluation with Apache POI's formula engine
- **Rich Formatting**: Fonts, colors, borders, alignment, number formats, and cell styles
- **Password Protection**: Reading and writing encrypted Excel files
- **CSV Conversion**: Bidirectional Excel ↔ CSV conversion with Apache Commons CSV

### Advanced Features
- **Memory Optimization**: Configurable streaming window for large file processing
- **Date/Time Handling**: Automatic detection and conversion of date values
- **Data Type Detection**: Intelligent parsing of strings, numbers, dates, and booleans
- **File Validation**: Comprehensive format and structure validation
- **Error Handling**: Robust error handling with detailed error messages
- **Thread Safety**: Safe for concurrent operations

## Dependencies

### Core Dependencies
- **Apache POI 5.2.5**: Core Excel processing library
- **Apache POI OOXML 5.2.5**: Excel 2007+ format support
- **Apache Commons CSV 1.10.0**: CSV processing capabilities
- **Apache XMLBeans 5.1.1**: Enhanced OOXML performance (optional)

### Runtime Requirements
- Java 17+ (for records and switch expressions)
- Minimum 256MB heap space for basic operations
- Additional memory for large files (streaming recommended for files >50MB)

## Configuration

### PoiConfiguration Options
```java
// Default configuration (balanced features and performance)
PoiConfiguration config = PoiConfiguration.defaults();

// Performance optimized (minimal features, faster processing)
PoiConfiguration config = PoiConfiguration.performance();

// Memory optimized (reduced memory usage)
PoiConfiguration config = PoiConfiguration.memoryOptimized();

// Full features (all capabilities enabled)
PoiConfiguration config = PoiConfiguration.fullFeatures();

// Custom configuration
PoiConfiguration config = PoiConfiguration.builder()
    .enableFormulasEvaluation(true)
    .streamingRowAccessWindow(1000)
    .maxRows(1048576)
    .compressTemporaryFiles(true)
    .useSharedStrings(true)
    .readOnlyMode(false)
    .strictParsing(false)
    .build();
```

### Configuration Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `enableFormulasEvaluation` | `true` | Whether to evaluate formulas when reading |
| `streamingRowAccessWindow` | `500` | Number of rows kept in memory for SXSSF |
| `maxRows` | `1048576` | Maximum rows supported (Excel 2007+ limit) |
| `maxColumns` | `16384` | Maximum columns supported (Excel 2007+ limit) |
| `compressTemporaryFiles` | `true` | Whether to compress temp files during streaming |
| `useSharedStrings` | `true` | Use shared strings table for repeated text |
| `readOnlyMode` | `false` | Open files in read-only mode for performance |
| `strictParsing` | `false` | Use strict OOXML parsing (may fail on non-compliant files) |

## Usage Examples

### Basic Usage
```java
// Create adapter with default configuration
ExcelPort excelPort = new PoiExcelAdapter();

// Or with custom configuration
PoiConfiguration config = PoiConfiguration.memoryOptimized();
ExcelPort excelPort = new PoiExcelAdapter(config);
```

### Reading Excel Files
```java
// Read entire workbook
Result<ExcelWorkbook> result = excelPort.readWorkbook(Paths.get("data.xlsx"));

result.onSuccess(workbook -> {
    System.out.println("Loaded workbook with " + workbook.getWorksheetCount() + " sheets");
    
    ExcelWorksheet sheet = workbook.getActiveWorksheet();
    System.out.println("Active sheet: " + sheet.name());
    System.out.println("Used range: " + sheet.getUsedRange());
    
    // Access specific cell
    ExcelCell cell = sheet.getCell("A1");
    if (cell != null) {
        System.out.println("A1 value: " + cell.getStringValue());
    }
});

// Read with options
ExcelReadOptions options = ExcelReadOptions.builder()
    .readAllSheets(true)
    .readFormulas(true)
    .skipBlankRows(true)
    .maxRows(10000)
    .build();

Result<ExcelWorkbook> result = excelPort.readWorkbook(
    Files.newInputStream(Paths.get("data.xlsx")),
    "data.xlsx"
);
```

### Writing Excel Files
```java
// Create workbook with data
ExcelWorkbook workbook = ExcelWorkbook.builder()
    .title("Sales Report Q4 2024")
    .author("Sales Department")
    .worksheet(
        ExcelWorksheet.builder("Sales Data")
            // Header row with styling
            .cell(ExcelCell.text(0, 0, "Product", ExcelCellStyle.headerStyle()))
            .cell(ExcelCell.text(0, 1, "Q4 Sales", ExcelCellStyle.headerStyle()))
            .cell(ExcelCell.text(0, 2, "Growth %", ExcelCellStyle.headerStyle()))
            
            // Data rows
            .cell(ExcelCell.text(1, 0, "Widget Pro"))
            .cell(ExcelCell.number(1, 1, 125000.00, ExcelCellStyle.currencyStyle()))
            .cell(ExcelCell.number(1, 2, 0.15, ExcelCellStyle.percentageStyle()))
            
            .cell(ExcelCell.text(2, 0, "Gadget Max"))
            .cell(ExcelCell.number(2, 1, 89500.00, ExcelCellStyle.currencyStyle()))
            .cell(ExcelCell.number(2, 2, -0.05, ExcelCellStyle.percentageStyle()))
            
            // Formula cell
            .cell(ExcelCell.formula(3, 1, "SUM(B2:B3)", ExcelCellStyle.currencyStyle()))
            
            // Configuration
            .columnWidth(0, 15.0)  // Product column wider
            .columnWidth(1, 12.0)  // Sales column
            .columnWidth(2, 10.0)  // Growth column
            .freezePanes(1, 0)     // Freeze header row
            .autoFilter(true)      // Enable filtering
            .build())
    .build();

// Write with options
ExcelWriteOptions options = ExcelWriteOptions.builder()
    .format(ExcelWriteOptions.ExcelFormat.XLSX)
    .compress(true)
    .autoSizeColumns(false) // We set widths manually
    .creator("My Application v1.0")
    .build();

Result<Void> result = excelPort.writeWorkbook(workbook, Paths.get("sales-report.xlsx"), options);
```

### Streaming Operations for Large Files

#### Streaming Reader
```java
// Read large file efficiently
ExcelReadOptions options = ExcelReadOptions.streaming();
Result<ExcelStreamReader> readerResult = excelPort.createStreamReader(
    Paths.get("large-dataset.xlsx"), 
    options
);

readerResult.onSuccess(reader -> {
    try {
        reader.selectFirstWorksheet();
        
        int recordCount = 0;
        while (reader.hasNext()) {
            Result<List<ExcelCell>> rowResult = reader.readNext();
            
            rowResult.onSuccess(cells -> {
                // Process row efficiently
                processRow(cells);
                recordCount++;
                
                if (recordCount % 1000 == 0) {
                    System.out.println("Processed " + recordCount + " records");
                }
            });
        }
        
        System.out.println("Total records processed: " + recordCount);
    } finally {
        reader.close();
    }
});

private void processRow(List<ExcelCell> cells) {
    // Extract data from cells
    String id = cells.get(0).getStringValue();
    String name = cells.get(1).getStringValue();
    double amount = cells.get(2).getNumericValue();
    LocalDate date = (LocalDate) cells.get(3).value();
    
    // Process business logic
    saveToDatabase(id, name, amount, date);
}
```

#### Streaming Writer
```java
// Write large dataset efficiently
ExcelWriteOptions options = ExcelWriteOptions.builder()
    .format(ExcelWriteOptions.ExcelFormat.XLSX)
    .compress(true)
    .build();

Result<ExcelStreamWriter> writerResult = excelPort.createStreamWriter(
    Paths.get("large-export.xlsx"), 
    options
);

writerResult.onSuccess(writer -> {
    try {
        writer.createWorksheet("Data Export");
        
        // Write header
        writer.writeRow("ID", "Name", "Amount", "Date", "Category");
        writer.freezePanes(1, 0);
        
        // Set column widths
        writer.setColumnWidth(0, 10);  // ID
        writer.setColumnWidth(1, 25);  // Name
        writer.setColumnWidth(2, 12);  // Amount
        writer.setColumnWidth(3, 12);  // Date
        writer.setColumnWidth(4, 15);  // Category
        
        // Write data rows (example: from database)
        ResultSet rs = executeQuery("SELECT id, name, amount, date, category FROM sales_data");
        int rowCount = 0;
        
        while (rs.next()) {
            writer.writeRow(
                rs.getString("id"),
                rs.getString("name"),
                rs.getDouble("amount"),
                rs.getDate("date").toLocalDate(),
                rs.getString("category")
            );
            
            rowCount++;
            
            // Flush periodically to manage memory
            if (rowCount % 5000 == 0) {
                writer.flush();
                System.out.println("Written " + rowCount + " rows");
            }
        }
        
        System.out.println("Export complete: " + rowCount + " rows");
    } finally {
        writer.close();
    }
});
```

### CSV Conversion
```java
// Excel to CSV
Result<String> csvResult = excelPort.toCsv(
    workbook, 
    "Sales Data",  // worksheet name
    CsvOptions.builder()
        .delimiter(';')
        .includeHeaders(true)
        .dateFormat("dd/MM/yyyy")
        .numberFormat("%.2f")
        .encoding("UTF-8")
        .build()
);

csvResult.onSuccess(csvContent -> {
    Files.writeString(Paths.get("sales-data.csv"), csvContent, StandardCharsets.UTF_8);
});

// CSV to Excel
String csvData = Files.readString(Paths.get("input.csv"));
Result<ExcelWorkbook> workbookResult = excelPort.fromCsv(
    csvData, 
    "Imported Data",
    CsvOptions.defaults()
);

workbookResult.onSuccess(importedWorkbook -> {
    ExcelWriteOptions options = ExcelWriteOptions.defaults();
    excelPort.writeWorkbook(importedWorkbook, Paths.get("converted.xlsx"), options);
});
```

### File Validation
```java
// Validate Excel file before processing
Result<ExcelValidationResult> validation = excelPort.validateFile(Paths.get("unknown-file.xlsx"));

validation.onSuccess(result -> {
    if (result.isValid()) {
        System.out.println("Valid Excel file:");
        System.out.println("  Format: " + result.format());
        System.out.println("  Version: " + result.version());
        System.out.println("  Worksheets: " + result.worksheetCount());
        System.out.println("  Encrypted: " + result.isEncrypted());
        System.out.println("  Has Formulas: " + result.hasFormulas());
        System.out.println("  Has Macros: " + result.hasMacros());
        
        if (result.hasWarnings()) {
            System.out.println("Warnings:");
            result.warnings().forEach(warning -> System.out.println("  - " + warning));
        }
    } else {
        System.out.println("Invalid Excel file:");
        result.errors().forEach(error -> System.out.println("  - " + error));
    }
});
```

### Working with Formulas
```java
// Create workbook with formulas
ExcelWorkbook workbook = ExcelWorkbook.builder()
    .worksheet(
        ExcelWorksheet.builder("Calculations")
            // Input data
            .cell(ExcelCell.number(0, 0, 100))
            .cell(ExcelCell.number(1, 0, 200))
            .cell(ExcelCell.number(2, 0, 300))
            
            // Formula cells
            .cell(ExcelCell.formula(3, 0, "SUM(A1:A3)"))           // Simple sum
            .cell(ExcelCell.formula(4, 0, "AVERAGE(A1:A3)"))       // Average
            .cell(ExcelCell.formula(5, 0, "IF(A4>500,\"High\",\"Normal\")")) // Conditional
            .cell(ExcelCell.formula(6, 0, "A4*0.1"))               // Calculation
            
            .build())
    .build();

// When reading, formulas are evaluated automatically
Result<ExcelWorkbook> result = excelPort.readWorkbook(filePath);
result.onSuccess(readWorkbook -> {
    ExcelCell sumCell = readWorkbook.getActiveWorksheet().getCell(3, 0);
    System.out.println("Formula: " + sumCell.formula());        // "SUM(A1:A3)"
    System.out.println("Value: " + sumCell.getNumericValue()); // 600.0
});
```

### Advanced Styling
```java
// Create custom styles
ExcelCellStyle titleStyle = ExcelCellStyle.builder()
    .fontName("Arial")
    .fontSize(16)
    .bold(true)
    .fontColor(Color.BLUE)
    .backgroundColor(Color.LIGHT_GRAY)
    .horizontalAlignment(HorizontalAlignment.CENTER)
    .borderStyle(BorderStyle.THICK)
    .build();

ExcelCellStyle dataStyle = ExcelCellStyle.builder()
    .fontName("Calibri")
    .fontSize(11)
    .numberFormat("#,##0.00")
    .horizontalAlignment(HorizontalAlignment.RIGHT)
    .borderStyle(BorderStyle.THIN)
    .build();

ExcelCellStyle dateStyle = ExcelCellStyle.builder()
    .numberFormat("dd/mm/yyyy")
    .horizontalAlignment(HorizontalAlignment.CENTER)
    .build();

// Apply styles to cells
ExcelWorksheet worksheet = ExcelWorksheet.builder("Styled Report")
    .cell(ExcelCell.text(0, 0, "Financial Report Q4", titleStyle))
    .cell(ExcelCell.number(1, 0, 1234.56, dataStyle))
    .cell(ExcelCell.date(2, 0, LocalDate.now(), dateStyle))
    .build();
```

## Performance Considerations

### Memory Usage
- **Small files (<10MB)**: In-memory operations are efficient
- **Medium files (10-50MB)**: Consider using `ExcelReadOptions` with row/column limits
- **Large files (>50MB)**: Always use streaming operations (`ExcelStreamReader`/`ExcelStreamWriter`)

### Optimization Tips
```java
// For performance-critical applications
PoiConfiguration config = PoiConfiguration.builder()
    .enableFormulasEvaluation(false)        // Skip formula evaluation
    .streamingRowAccessWindow(100)          // Smaller memory window
    .compressTemporaryFiles(false)          // Faster but more disk space
    .useSharedStrings(false)               // Trade memory for speed
    .readOnlyMode(true)                    // Faster file access
    .build();

ExcelReadOptions readOptions = ExcelReadOptions.builder()
    .skipBlankRows(true)                   // Skip empty rows
    .maxRows(50000)                        // Limit data size
    .readFormulas(false)                   // Don't read formula text
    .build();

ExcelWriteOptions writeOptions = ExcelWriteOptions.builder()
    .format(ExcelFormat.XLSX)              // XLSX is generally faster
    .compress(false)                       // Faster writing
    .writeFormulas(false)                  // Write values only
    .autoSizeColumns(false)                // Skip auto-sizing
    .build();
```

### Streaming Best Practices
- Process data row-by-row without accumulating in memory
- Use appropriate streaming window size (100-1000 rows)
- Call `flush()` periodically when writing
- Always close streams in finally blocks or use try-with-resources
- Monitor memory usage and adjust window size accordingly

## Error Handling

All operations return `Result<T>` for comprehensive error handling:

```java
Result<ExcelWorkbook> result = excelPort.readWorkbook(filePath);

// Pattern matching (Java 17+)
switch (result) {
    case Result.Success<ExcelWorkbook> success -> {
        ExcelWorkbook workbook = success.value();
        processWorkbook(workbook);
    }
    case Result.Failure<ExcelWorkbook> failure -> {
        Problem problem = failure.problem();
        handleError(problem.getMessage(), problem.getCause());
    }
}

// Traditional approach
result
    .onSuccess(workbook -> {
        // Process successful result
        System.out.println("Successfully loaded " + workbook.getWorksheetCount() + " sheets");
    })
    .onFailure(error -> {
        // Handle specific error types
        if (error.getMessage().contains("password")) {
            promptForPassword();
        } else if (error.getMessage().contains("corrupted")) {
            notifyFileCorruption();
        } else {
            logGenericError(error);
        }
    });
```

## Thread Safety

- **Adapter Instance**: Thread-safe for concurrent operations
- **Configuration**: Immutable and thread-safe
- **Workbook Objects**: Immutable value objects, safe for sharing
- **Stream Objects**: Not thread-safe, use one per thread
- **POI Workbooks**: Not thread-safe, handled internally by the adapter

## Troubleshooting

### Common Issues

1. **OutOfMemoryError with large files**
   ```java
   // Solution: Use streaming operations
   ExcelStreamReader reader = excelPort.createStreamReader(filePath, options).value();
   ```

2. **Formula evaluation errors**
   ```java
   // Solution: Disable formula evaluation for performance
   PoiConfiguration config = PoiConfiguration.builder()
       .enableFormulasEvaluation(false)
       .build();
   ```

3. **Encrypted file errors**
   ```java
   // Solution: Provide password in read options
   ExcelReadOptions options = ExcelReadOptions.builder()
       .password("your-password")
       .build();
   ```

4. **Date formatting issues**
   ```java
   // Solution: Specify date format explicitly
   CsvOptions csvOptions = CsvOptions.builder()
       .dateFormat("dd/MM/yyyy")
       .build();
   ```

### Performance Monitoring
```java
// Monitor memory usage
long beforeMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
Result<ExcelWorkbook> result = excelPort.readWorkbook(filePath);
long afterMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
System.out.println("Memory used: " + (afterMemory - beforeMemory) / 1024 / 1024 + " MB");

// Monitor processing time
long startTime = System.currentTimeMillis();
// ... Excel operations ...
long endTime = System.currentTimeMillis();
System.out.println("Processing time: " + (endTime - startTime) + " ms");
```

## Compatibility

### Excel Versions
- **Excel 2007+**: Full support (.xlsx, .xlsm)
- **Excel 97-2003**: Full support (.xls)
- **Excel Online**: Compatible with generated files
- **LibreOffice Calc**: Compatible with standard features
- **Google Sheets**: Compatible when uploaded

### Java Versions
- **Minimum**: Java 17 (for records and enhanced switch)
- **Recommended**: Java 21 LTS
- **Tested**: Java 17, 21

### Dependencies Compatibility
- Apache POI 5.2.5 (latest stable)
- Compatible with Spring Boot 3.x
- Compatible with Jakarta EE 9+
- No conflicts with other Apache Commons libraries

This adapter provides a robust, feature-rich implementation of Excel processing capabilities while maintaining the clean abstractions defined by the port interface. It's suitable for both simple file operations and complex enterprise-grade Excel processing requirements.
