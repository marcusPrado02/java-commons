package com.marcusprado02.commons.testkit;

import java.util.Random;
import java.util.UUID;

/**
 * Utilities for generating random test data.
 *
 * <p>Provides methods to generate random strings, numbers, booleans, etc.
 */
public final class RandomData {

  private static final Random RANDOM = new Random();
  private static final String ALPHANUMERIC =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

  private RandomData() {}

  /** Generates a random integer between min (inclusive) and max (exclusive). */
  public static int randomInt(int min, int max) {
    return RANDOM.nextInt(max - min) + min;
  }

  /** Generates a random positive integer between 1 and max (exclusive). */
  public static int randomInt(int max) {
    return randomInt(1, max);
  }

  /** Generates a random long between min (inclusive) and max (exclusive). */
  public static long randomLong(long min, long max) {
    return RANDOM.nextLong(max - min) + min;
  }

  /** Generates a random long between 1 and max (exclusive). */
  public static long randomLong(long max) {
    return randomLong(1, max);
  }

  /** Generates a random double between 0.0 (inclusive) and 1.0 (exclusive). */
  public static double randomDouble() {
    return RANDOM.nextDouble();
  }

  /** Generates a random double between min and max. */
  public static double randomDouble(double min, double max) {
    return min + (max - min) * RANDOM.nextDouble();
  }

  /** Generates a random boolean. */
  public static boolean randomBoolean() {
    return RANDOM.nextBoolean();
  }

  /** Generates a random alphanumeric string of the given length. */
  public static String randomString(int length) {
    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
    }
    return sb.toString();
  }

  /** Generates a random alphanumeric string of length 10. */
  public static String randomString() {
    return randomString(10);
  }

  /** Generates a random email address. */
  public static String randomEmail() {
    return randomString(8).toLowerCase() + "@test.com";
  }

  /** Generates a random UUID string. */
  public static String randomUuid() {
    return UUID.randomUUID().toString();
  }

  /** Picks a random element from the given array. */
  @SafeVarargs
  public static <T> T randomFrom(T... values) {
    if (values == null || values.length == 0) {
      throw new IllegalArgumentException("Array must not be empty");
    }
    return values[RANDOM.nextInt(values.length)];
  }

  /** Picks a random enum value. */
  public static <E extends Enum<E>> E randomEnum(Class<E> enumClass) {
    E[] values = enumClass.getEnumConstants();
    return values[RANDOM.nextInt(values.length)];
  }

  /** Sets a custom seed for reproducible random data generation. */
  public static void setSeed(long seed) {
    RANDOM.setSeed(seed);
  }
}
