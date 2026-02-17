package com.marcusprado02.commons.ports.blockchain;

import java.math.BigInteger;

/**
 * Represents a blockchain wallet.
 *
 * @param address wallet address
 * @param publicKey public key (optional)
 * @param balance current balance in wei
 */
public record Wallet(String address, String publicKey, BigInteger balance) {

  /**
   * Creates a builder for Wallet.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for Wallet. */
  public static class Builder {
    private String address;
    private String publicKey;
    private BigInteger balance = BigInteger.ZERO;

    public Builder address(String address) {
      this.address = address;
      return this;
    }

    public Builder publicKey(String publicKey) {
      this.publicKey = publicKey;
      return this;
    }

    public Builder balance(BigInteger balance) {
      this.balance = balance;
      return this;
    }

    public Wallet build() {
      return new Wallet(address, publicKey, balance);
    }
  }
}
