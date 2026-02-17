package com.marcusprado02.commons.ports.blockchain;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Optional;

/**
 * Represents a blockchain transaction.
 *
 * @param hash transaction hash
 * @param from sender address
 * @param to recipient address (empty for contract creation)
 * @param value amount transferred in wei
 * @param gasPrice gas price in wei
 * @param gasLimit gas limit
 * @param nonce transaction nonce
 * @param data transaction data/input
 * @param blockNumber block number (empty if pending)
 * @param blockHash block hash (empty if pending)
 * @param timestamp transaction timestamp (empty if pending)
 * @param status transaction status (SUCCESS, FAILED, PENDING)
 */
public record BlockchainTransaction(
    String hash,
    String from,
    Optional<String> to,
    BigInteger value,
    BigInteger gasPrice,
    BigInteger gasLimit,
    BigInteger nonce,
    String data,
    Optional<BigInteger> blockNumber,
    Optional<String> blockHash,
    Optional<Instant> timestamp,
    TransactionStatus status) {

  public BlockchainTransaction {
    to = to == null ? Optional.empty() : to;
    blockNumber = blockNumber == null ? Optional.empty() : blockNumber;
    blockHash = blockHash == null ? Optional.empty() : blockHash;
    timestamp = timestamp == null ? Optional.empty() : timestamp;
  }

  /** Transaction status. */
  public enum TransactionStatus {
    PENDING,
    SUCCESS,
    FAILED
  }

  /**
   * Checks if transaction is pending.
   *
   * @return true if pending
   */
  public boolean isPending() {
    return status == TransactionStatus.PENDING;
  }

  /**
   * Checks if transaction is confirmed.
   *
   * @return true if confirmed (success or failed)
   */
  public boolean isConfirmed() {
    return status != TransactionStatus.PENDING;
  }

  /**
   * Checks if transaction is successful.
   *
   * @return true if success
   */
  public boolean isSuccess() {
    return status == TransactionStatus.SUCCESS;
  }

  /**
   * Creates a builder for BlockchainTransaction.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for BlockchainTransaction. */
  public static class Builder {
    private String hash;
    private String from;
    private Optional<String> to = Optional.empty();
    private BigInteger value = BigInteger.ZERO;
    private BigInteger gasPrice;
    private BigInteger gasLimit;
    private BigInteger nonce;
    private String data = "0x";
    private Optional<BigInteger> blockNumber = Optional.empty();
    private Optional<String> blockHash = Optional.empty();
    private Optional<Instant> timestamp = Optional.empty();
    private TransactionStatus status = TransactionStatus.PENDING;

    public Builder hash(String hash) {
      this.hash = hash;
      return this;
    }

    public Builder from(String from) {
      this.from = from;
      return this;
    }

    public Builder to(String to) {
      this.to = Optional.ofNullable(to);
      return this;
    }

    public Builder value(BigInteger value) {
      this.value = value;
      return this;
    }

    public Builder gasPrice(BigInteger gasPrice) {
      this.gasPrice = gasPrice;
      return this;
    }

    public Builder gasLimit(BigInteger gasLimit) {
      this.gasLimit = gasLimit;
      return this;
    }

    public Builder nonce(BigInteger nonce) {
      this.nonce = nonce;
      return this;
    }

    public Builder data(String data) {
      this.data = data;
      return this;
    }

    public Builder blockNumber(BigInteger blockNumber) {
      this.blockNumber = Optional.ofNullable(blockNumber);
      return this;
    }

    public Builder blockHash(String blockHash) {
      this.blockHash = Optional.ofNullable(blockHash);
      return this;
    }

    public Builder timestamp(Instant timestamp) {
      this.timestamp = Optional.ofNullable(timestamp);
      return this;
    }

    public Builder status(TransactionStatus status) {
      this.status = status;
      return this;
    }

    public BlockchainTransaction build() {
      return new BlockchainTransaction(
          hash,
          from,
          to,
          value,
          gasPrice,
          gasLimit,
          nonce,
          data,
          blockNumber,
          blockHash,
          timestamp,
          status);
    }
  }
}
