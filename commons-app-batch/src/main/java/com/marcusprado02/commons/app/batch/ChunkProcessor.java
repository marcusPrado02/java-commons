package com.marcusprado02.commons.app.batch;

import com.marcusprado02.commons.kernel.result.Result;

/**
 * Orchestrates chunk-oriented processing of items.
 *
 * <p>A chunk processor coordinates reading, processing, and writing items in configurable chunks,
 * with support for retry and skip policies.
 *
 * <h2>Processing Flow</h2>
 *
 * <ol>
 *   <li>Read items up to chunk size
 *   <li>Process each item (with filtering)
 *   <li>Write the chunk of processed items
 *   <li>Handle errors according to retry/skip policies
 * </ol>
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * ChunkProcessor<CustomerRecord, ValidatedCustomer> processor =
 *     new SimpleChunkProcessor<>(
 *         customerReader,
 *         customerValidator,
 *         customerWriter,
 *         100  // chunk size
 *     );
 *
 * Result<ChunkProcessingResult> result =
 *     processor.processChunk(context);
 * }</pre>
 *
 * @param <I> the input item type
 * @param <O> the output item type
 */
public interface ChunkProcessor<I, O> {

  /**
   * Processes a single chunk of items.
   *
   * @param context the execution context
   * @return result containing chunk processing statistics or an error
   */
  Result<ChunkProcessingResult> processChunk(BatchJobContext context);

  /**
   * Gets the chunk size.
   *
   * @return the number of items to process per chunk
   */
  int getChunkSize();

  /**
   * Checks if there are more items to process.
   *
   * @return true if more items are available
   */
  boolean hasMoreItems();

  /** Closes resources used by reader, processor, and writer. */
  default void close() {
    // Default: no-op
  }
}
