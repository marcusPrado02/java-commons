package com.marcusprado02.commons.app.batch;

import com.marcusprado02.commons.kernel.result.Result;
import java.util.List;

/**
 * Strategy interface for writing chunks of items to a destination.
 *
 * <p>Implementations write processed items in batches for efficiency, typically within a
 * transaction boundary.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * public class CustomerDatabaseWriter implements ItemWriter<ValidatedCustomer> {
 *   private final CustomerRepository repository;
 *
 *   @Override
 *   public Result<Void> write(List<ValidatedCustomer> items) {
 *     try {
 *       repository.saveAll(items);
 *       return Result.ok(null);
 *     } catch (DataAccessException e) {
 *       return Result.failure(BatchJobError.of(
 *         BatchJobError.ITEM_WRITE_FAILED,
 *         "Failed to write " + items.size() + " customers",
 *         e
 *       ));
 *     }
 *   }
 * }
 * }</pre>
 *
 * @param <O> the type of items to write
 */
@FunctionalInterface
public interface ItemWriter<O> {

  /**
   * Writes a chunk of items.
   *
   * <p>All items in the chunk should be written atomically if possible. If writing fails, none of
   * the items should be persisted.
   *
   * @param items the items to write
   * @return result indicating success or error
   */
  Result<Void> write(List<O> items);

  /**
   * Closes resources used by this writer.
   *
   * <p>Called when the job completes or fails.
   */
  default void close() {
    // Default: no-op
  }
}
