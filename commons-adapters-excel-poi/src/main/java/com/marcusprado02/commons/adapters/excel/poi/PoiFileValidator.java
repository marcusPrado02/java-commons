package com.marcusprado02.commons.adapters.excel.poi;

import com.marcusprado02.commons.ports.excel.ExcelValidationResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Utility class for validating Excel file format and structure using Apache POI.
 *
 * @since 0.1.0
 */
public final class PoiFileValidator {

  private PoiFileValidator() {
    // Utility class
  }

  /**
   * Validates an Excel file and returns detailed validation information.
   *
   * @param filePath path to Excel file
   * @return validation result
   */
  public static ExcelValidationResult validate(Path filePath) {
    List<String> warnings = new ArrayList<>();
    List<String> errors = new ArrayList<>();

    try {
      // Check if file exists and is readable
      if (!Files.exists(filePath)) {
        return ExcelValidationResult.invalid("File does not exist: " + filePath);
      }

      if (!Files.isReadable(filePath)) {
        return ExcelValidationResult.invalid("File is not readable: " + filePath);
      }

      // Check file size
      long fileSize = Files.size(filePath);
      if (fileSize == 0) {
        return ExcelValidationResult.invalid("File is empty");
      }

      if (fileSize > 100 * 1024 * 1024) { // 100 MB
        warnings.add("Large file size: " + (fileSize / 1024 / 1024) + " MB");
      }

      // Detect file format by extension
      String fileName = filePath.getFileName().toString().toLowerCase();
      String detectedFormat = detectFormat(fileName);

      if (detectedFormat == null) {
        warnings.add("Unrecognized file extension, attempting to detect format by content");
      }

      // Try to open and validate the workbook
      ValidationInfo info = validateWorkbook(filePath);

      if (info.isEncrypted && !info.canOpen) {
        errors.add("File is encrypted and requires a password");
      }

      if (!info.canOpen) {
        errors.add("Cannot open file - may be corrupted or unsupported format");
      }

      if (info.worksheetCount == 0) {
        warnings.add("No worksheets found in the workbook");
      }

      // Additional validations
      if (info.hasLargeWorksheets) {
        warnings.add("Contains large worksheets that may impact performance");
      }

      if (info.hasMacros) {
        warnings.add("File contains macros - may require special handling");
      }

      if (info.hasExternalReferences) {
        warnings.add("File contains external references that may not resolve");
      }

      // Create result
      boolean isValid = errors.isEmpty() && info.canOpen;

      return new ExcelValidationResult(
        isValid,
        info.actualFormat != null ? info.actualFormat : detectedFormat,
        info.version,
        info.worksheetCount,
        info.isEncrypted,
        info.hasFormulas,
        info.hasMacros,
        warnings,
        errors
      );

    } catch (Exception e) {
      return ExcelValidationResult.invalid("Validation failed: " + e.getMessage());
    }
  }

  /**
   * Detects Excel format based on file extension.
   */
  private static String detectFormat(String fileName) {
    if (fileName.endsWith(".xlsx")) {
      return "XLSX";
    } else if (fileName.endsWith(".xls")) {
      return "XLS";
    } else if (fileName.endsWith(".xlsm")) {
      return "XLSM";
    } else if (fileName.endsWith(".xlsb")) {
      return "XLSB";
    } else if (fileName.endsWith(".csv")) {
      return "CSV";
    }
    return null;
  }

  /**
   * Validates workbook by attempting to open it and gather information.
   */
  private static ValidationInfo validateWorkbook(Path filePath) {
    ValidationInfo info = new ValidationInfo();

    Workbook workbook = null;
    try {
      // Try to open without password first
      workbook = WorkbookFactory.create(filePath.toFile());
      info.canOpen = true;

      // Gather workbook information
      info.worksheetCount = workbook.getNumberOfSheets();
      info.actualFormat = determineActualFormat(workbook);
      info.hasFormulas = checkForFormulas(workbook);
      info.hasMacros = checkForMacros(workbook);
      info.hasExternalReferences = checkForExternalReferences(workbook);
      info.hasLargeWorksheets = checkForLargeWorksheets(workbook);

      // Try to determine version
      info.version = determineVersion(workbook);

    } catch (EncryptedDocumentException e) {
      info.isEncrypted = true;
      info.canOpen = false;
    } catch (IOException e) {
      info.canOpen = false;
    } catch (Exception e) {
      info.canOpen = false;
    } finally {
      if (workbook != null) {
        try {
          workbook.close();
        } catch (IOException e) {
          // Ignore close errors
        }
      }
    }

    // If couldn't open normally, try to detect encryption
    if (!info.canOpen && !info.isEncrypted) {
      info.isEncrypted = checkIfEncrypted(filePath);
    }

    return info;
  }

  /**
   * Determines the actual format of an opened workbook.
   */
  private static String determineActualFormat(Workbook workbook) {
    if (workbook instanceof XSSFWorkbook) {
      return "XLSX";
    } else if (workbook instanceof HSSFWorkbook) {
      return "XLS";
    } else {
      return "UNKNOWN";
    }
  }

  /**
   * Checks if the workbook contains any formulas.
   */
  private static boolean checkForFormulas(Workbook workbook) {
    try {
      for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
        Sheet sheet = workbook.getSheetAt(i);
        if (sheet != null) {
          // Check first few rows for formulas (performance optimization)
          int maxRowsToCheck = Math.min(sheet.getLastRowNum() + 1, 100);
          for (int rowNum = 0; rowNum < maxRowsToCheck; rowNum++) {
            var row = sheet.getRow(rowNum);
            if (row != null) {
              for (var cell : row) {
                if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.FORMULA) {
                  return true;
                }
              }
            }
          }
        }
      }
    } catch (Exception e) {
      // Ignore errors during formula detection
    }
    return false;
  }

  /**
   * Checks if the workbook contains macros (simplified check).
   */
  private static boolean checkForMacros(Workbook workbook) {
    try {
      // For XLSX files, macros would be in XLSM format
      // For XLS files, check for macro modules (simplified)
      if (workbook instanceof HSSFWorkbook) {
        // HSSFWorkbook has some methods to check for macros, but simplified here
        return false; // Could implement more detailed check
      }
      return false;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Checks if the workbook has external references.
   */
  private static boolean checkForExternalReferences(Workbook workbook) {
    try {
      // External reference checking not implemented for now
      // Note: Different POI implementations may have different ways to check for external refs
      return false;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Checks if the workbook has large worksheets that might impact performance.
   */
  private static boolean checkForLargeWorksheets(Workbook workbook) {
    try {
      for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
        Sheet sheet = workbook.getSheetAt(i);
        if (sheet != null) {
          int lastRow = sheet.getLastRowNum();
          if (lastRow > 10000) { // More than 10k rows
            return true;
          }
        }
      }
    } catch (Exception e) {
      // Ignore errors
    }
    return false;
  }

  /**
   * Determines the version/compatibility level of the workbook.
   */
  private static String determineVersion(Workbook workbook) {
    if (workbook instanceof XSSFWorkbook) {
      return "Excel 2007+";
    } else if (workbook instanceof HSSFWorkbook) {
      return "Excel 97-2003";
    } else {
      return "Unknown";
    }
  }

  /**
   * Checks if a file is encrypted (when normal open fails).
   */
  private static boolean checkIfEncrypted(Path filePath) {
    try (var opcPackage = OPCPackage.open(filePath.toFile(), PackageAccess.READ)) {
      return false; // If we can open the OPC package, it's not encrypted
    } catch (Exception e) {
      // If we can't open the OPC package, it might be encrypted
      String message = e.getMessage();
      return message != null && (
        message.contains("password") ||
        message.contains("encrypted") ||
        message.contains("protected")
      );
    }
  }

  /**
   * Helper class to hold validation information.
   */
  private static class ValidationInfo {
    boolean canOpen = false;
    boolean isEncrypted = false;
    String actualFormat;
    String version;
    int worksheetCount = 0;
    boolean hasFormulas = false;
    boolean hasMacros = false;
    boolean hasExternalReferences = false;
    boolean hasLargeWorksheets = false;
  }
}
