package com.marcusprado02.commons.ports.blockchain;

import java.math.BigInteger;
import java.util.List;

/**
 * Represents a transaction receipt.
 *
 * @param transactionHash transaction hash
 * @param blockNumber block number
 * @param blockHash block hash
 * @param from sender address
 * @param to recipient address
 * @param gasUsed gas used
 * @param cumulativeGasUsed cumulative gas used in block
 * @param contractAddress created contract address (if applicable)
 * @param status transaction status (1 = success, 0 = failed)
 * @param logs event logs
 */
public record TransactionReceipt(
    String transactionHash,
    BigInteger blockNumber,
    String blockHash,
    String from,
    String to,
    BigInteger gasUsed,
    BigInteger cumulativeGasUsed,
    String contractAddress,
    boolean status,
    List<Log> logs) {

  public TransactionReceipt {
    logs = logs == null ? List.of() : List.copyOf(logs);
  }

  /** Represents an event log. */
  public record Log(
      String address, List<String> topics, String data, BigInteger blockNumber, String blockHash) {

    public Log {
      topics = topics == null ? List.of() : List.copyOf(topics);
    }
  }

  /**
   * Creates a builder for TransactionReceipt.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for TransactionReceipt. */
  public static class Builder {
    private String transactionHash;
    private BigInteger blockNumber;
    private String blockHash;
    private String from;
    private String to;
    private BigInteger gasUsed;
    private BigInteger cumulativeGasUsed;
    private String contractAddress;
    private boolean status;
    private List<Log> logs = List.of();

    public Builder transactionHash(String transactionHash) {
      this.transactionHash = transactionHash;
      return this;
    }

    public Builder blockNumber(BigInteger blockNumber) {
      this.blockNumber = blockNumber;
      return this;
    }

    public Builder blockHash(String blockHash) {
      this.blockHash = blockHash;
      return this;
    }

    public Builder from(String from) {
      this.from = from;
      return this;
    }

    public Builder to(String to) {
      this.to = to;
      return this;
    }

    public Builder gasUsed(BigInteger gasUsed) {
      this.gasUsed = gasUsed;
      return this;
    }

    public Builder cumulativeGasUsed(BigInteger cumulativeGasUsed) {
      this.cumulativeGasUsed = cumulativeGasUsed;
      return this;
    }

    public Builder contractAddress(String contractAddress) {
      this.contractAddress = contractAddress;
      return this;
    }

    public Builder status(boolean status) {
      this.status = status;
      return this;
    }

    public Builder logs(List<Log> logs) {
      this.logs = logs;
      return this;
    }

    public TransactionReceipt build() {
      return new TransactionReceipt(
          transactionHash,
          blockNumber,
          blockHash,
          from,
          to,
          gasUsed,
          cumulativeGasUsed,
          contractAddress,
          status,
          logs);
    }
  }
}
