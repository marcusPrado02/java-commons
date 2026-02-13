package com.marcusprado02.commons.testkit.matchers;

import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.result.Result;
import java.util.function.Consumer;
import org.assertj.core.api.AbstractAssert;

/**
 * Custom AssertJ assertion for {@link Result} objects.
 *
 * @param <T> the type of the success value
 */
public class ResultAssert<T> extends AbstractAssert<ResultAssert<T>, Result<T>> {

  private ResultAssert(Result<T> actual) {
    super(actual, ResultAssert.class);
  }

  public static <T> ResultAssert<T> assertThat(Result<T> actual) {
    return new ResultAssert<>(actual);
  }

  public ResultAssert<T> isSuccess() {
    isNotNull();
    if (!actual.isOk()) {
      failWithMessage(
          "Expected result to be success but was failure with problem: %s",
          actual.problemOrNull());
    }
    return this;
  }

  public ResultAssert<T> isFailure() {
    isNotNull();
    if (actual.isOk()) {
      failWithMessage("Expected result to be failure but was success with value: %s", actual.getOrNull());
    }
    return this;
  }

  public ResultAssert<T> hasValue(Consumer<T> valueAssertions) {
    isSuccess();
    valueAssertions.accept(actual.getOrNull());
    return this;
  }

  public ResultAssert<T> hasError(Consumer<Problem> errorAssertions) {
    isFailure();
    errorAssertions.accept(actual.problemOrNull());
    return this;
  }
}
