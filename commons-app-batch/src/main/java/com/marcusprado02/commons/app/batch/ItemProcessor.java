package com.marcusprado02.commons.app.batch;

import com.marcusprado02.commons.kernel.result.Result;
import java.util.Optional;

/**
 * Strategy interface for processing individual items.
 *
 * <p>Implementations transform, validate, or enrich items during batch processing. Processors can
 * filter items by returning an empty Optional.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * public class CustomerValidator implements ItemProcessor<CustomerRecord, ValidatedCustomer> {
 *
 *   @Override
 *   public Result<Optional<ValidatedCustomer>> process(CustomerRecord item) {
 *     if (!isValid(item)) {
 *       // Filter out invalid items
 *       return Result.ok(Optional.empty());
 *     }
 *
 *     ValidatedCustomer validated = validate(item);
 *     return Result.ok(Optional.of(validated));
 *   }
 * }
 * }</pre>
 *
 * @param <I> the input item type
 * @param <O> the output item type
 */
@FunctionalInterface
public interface ItemProcessor<I, O> {

  /**
   * Processes an item.
   *
   * <p>Returns an empty Optional to filter out the item (it will not be written).
   *
   * @param item the item to process
   * @return result containing optional processed item or an error
   */
  Result<Optional<O>> process(I item);

  /**
   * Composes this processor with another processor.
   *
   * @param after the processor to apply after this one
   * @param <V> the type of output of the after processor
   * @return a composed processor
   */
  default <V> ItemProcessor<I, V> andThen(ItemProcessor<O, V> after) {
    return item ->
        this.process(item)
            .flatMap(opt -> opt.map(after::process).orElse(Result.ok(Optional.empty())));
  }
}
