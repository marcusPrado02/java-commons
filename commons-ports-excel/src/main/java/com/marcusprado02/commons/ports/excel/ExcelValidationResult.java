package com.marcusprado02.commons.ports.excel;

import java.util.List;

/**
 * Result of Excel file validation.
 *
 * @param isValid whether the file is valid
 * @param format detected file format (XLSX, XLS, CSV, etc.)
 * @param version Excel version/compatibility level
 * @param worksheetCount number of worksheets
 * @param isEncrypted whether the file is password protected
 * @param hasFormulas whether the file contains formulas
 * @param hasMacros whether the file contains macros
 * @param warnings list of validation warnings
 * @param errors list of validation errors
 * @since 0.1.0
 */
public record ExcelValidationResult(
    boolean isValid,
    String format,
    String version,
    int worksheetCount,
    boolean isEncrypted,
    boolean hasFormulas,
    boolean hasMacros,
    List<String> warnings,
    List<String> errors) {

  public ExcelValidationResult {
    warnings = warnings == null ? List.of() : List.copyOf(warnings);
    errors = errors == null ? List.of() : List.copyOf(errors);
  }

  /**
   * Creates a successful validation result.
   *
   * @param format detected format
   * @param worksheetCount number of sheets
   * @return validation result
   */
  public static ExcelValidationResult valid(String format, int worksheetCount) {
    return new ExcelValidationResult(
        true, format, null, worksheetCount, false, false, false, List.of(), List.of());
  }

  /**
   * Creates a failed validation result.
   *
   * @param errors list of errors
   * @return validation result
   */
  public static ExcelValidationResult invalid(List<String> errors) {
    return new ExcelValidationResult(
        false, null, null, 0, false, false, false, List.of(), errors);
  }

  /**
   * Creates a failed validation result with a single error.
   *
   * @param error error message
   * @return validation result
   */
  public static ExcelValidationResult invalid(String error) {
    return invalid(List.of(error));
  }

  /**
   * Checks if there are any warnings.
   *
   * @return true if warnings exist
   */
  public boolean hasWarnings() {
    return !warnings.isEmpty();
  }

  /**
   * Checks if there are any errors.
   *
   * @return true if errors exist
   */
  public boolean hasErrors() {
    return !errors.isEmpty();
  }
}
