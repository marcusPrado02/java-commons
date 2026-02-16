package com.marcusprado02.commons.app.batch;

import com.marcusprado02.commons.kernel.result.Result;
import java.util.Optional;

/**
 * Strategy interface for reading items from a data source in chunks.
 *
 * <p>Implementations provide paginated or streaming access to data for batch processing. Readers
 * should be stateful, tracking their current position for restart capabilities.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * public class CustomerFileReader implements ItemReader<CustomerRecord> {
 *   private final BufferedReader reader;
 *   private long position = 0;
 *
 *   @Override
 *   public Result<Optional<CustomerRecord>> read() {
 *     try {
 *       String line = reader.readLine();
 *       if (line == null) {
 *         return Result.ok(Optional.empty());
 *       }
 *       position++;
 *       return Result.ok(Optional.of(parseCustomer(line)));
 *     } catch (IOException e) {
 *       return Result.failure(BatchJobError.of(
 *         BatchJobError.ITEM_READ_FAILED,
 *         "Failed to read customer at line " + position,
 *         e
 *       ));
 *     }
 *   }
 *
 *   @Override
 *   public long getCurrentPosition() {
 *     return position;
 *   }
 * }
 * }</pre>
 *
 * @param <I> the type of items to read
 */
@FunctionalInterface
public interface ItemReader<I> {

  /**
   * Reads the next item from the data source.
   *
   * <p>Returns an empty Optional when there are no more items to read.
   *
   * @return result containing optional item or an error
   */
  Result<Optional<I>> read();

  /**
   * Gets the current position in the data source.
   *
   * <p>This is used for restart capabilities to resume from the last successful position.
   *
   * @return the current position
   */
  default long getCurrentPosition() {
    return 0L;
  }

  /**
   * Moves the reader to a specific position.
   *
   * <p>Used when restarting a job to skip already processed items.
   *
   * @param position the position to seek to
   */
  default void seekTo(long position) {
    // Default: no-op
  }

  /**
   * Closes resources used by this reader.
   *
   * <p>Called when the job completes or fails.
   */
  default void close() {
    // Default: no-op
  }
}
