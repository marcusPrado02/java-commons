package com.marcusprado02.commons.kernel.result;

import java.util.Objects;
import java.util.function.Function;

public sealed interface Either<L, R> permits Either.Left, Either.Right {

  boolean isLeft();

  default boolean isRight() {
    return !isLeft();
  }

  L left();

  R right();

  static <L, R> Either<L, R> left(L value) {
    return new Left<>(Objects.requireNonNull(value));
  }

  static <L, R> Either<L, R> right(R value) {
    return new Right<>(Objects.requireNonNull(value));
  }

  default <U> Either<L, U> mapRight(Function<R, U> fn) {
    Objects.requireNonNull(fn);
    return isRight() ? Either.right(fn.apply(right())) : Either.left(left());
  }

  default <U> Either<U, R> mapLeft(Function<L, U> fn) {
    Objects.requireNonNull(fn);
    return isLeft() ? Either.left(fn.apply(left())) : Either.right(right());
  }

  record Left<L, R>(L value) implements Either<L, R> {
    @Override
    public boolean isLeft() {
      return true;
    }

    @Override
    public L left() {
      return value;
    }

    @Override
    public R right() {
      throw new IllegalStateException("No right value");
    }
  }

  record Right<L, R>(R value) implements Either<L, R> {
    @Override
    public boolean isLeft() {
      return false;
    }

    @Override
    public L left() {
      throw new IllegalStateException("No left value");
    }

    @Override
    public R right() {
      return value;
    }
  }
}
