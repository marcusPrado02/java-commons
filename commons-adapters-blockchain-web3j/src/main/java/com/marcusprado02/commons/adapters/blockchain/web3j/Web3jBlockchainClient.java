package com.marcusprado02.commons.adapters.blockchain.web3j;

import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.blockchain.BlockchainClient;
import com.marcusprado02.commons.ports.blockchain.BlockchainTransaction;
import com.marcusprado02.commons.ports.blockchain.TransactionReceipt;
import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;

/**
 * Web3j implementation of BlockchainClient.
 *
 * <p>Provides blockchain operations using Web3j library for Ethereum.
 */
public final class Web3jBlockchainClient implements BlockchainClient {

  private static final Logger logger = LoggerFactory.getLogger(Web3jBlockchainClient.class);
  private final Web3j web3j;

  private Web3jBlockchainClient(Web3j web3j) {
    this.web3j = web3j;
  }

  /**
   * Creates a Web3jBlockchainClient.
   *
   * @param web3j the Web3j instance
   * @return a new Web3jBlockchainClient
   */
  public static Web3jBlockchainClient create(Web3j web3j) {
    if (web3j == null) {
      throw new IllegalArgumentException("Web3j instance cannot be null");
    }
    return new Web3jBlockchainClient(web3j);
  }

  @Override
  public Result<BigInteger> getBlockNumber() {
    try {
      var response = web3j.ethBlockNumber().send();
      if (response.hasError()) {
        return Result.fail(
            Problem.of(
                ErrorCode.of("BLOCKCHAIN.NETWORK_ERROR"),
                ErrorCategory.TECHNICAL,
                Severity.ERROR,
                "Failed to get block number: " + response.getError().getMessage()));
      }
      return Result.ok(response.getBlockNumber());
    } catch (IOException e) {
      logger.error("Error getting block number", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("BLOCKCHAIN.NETWORK_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Network error: " + e.getMessage()));
    }
  }

  @Override
  public Result<BigInteger> getBalance(String address) {
    try {
      var response = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
      if (response.hasError()) {
        return Result.fail(
            Problem.of(
                ErrorCode.of("BLOCKCHAIN.NETWORK_ERROR"),
                ErrorCategory.TECHNICAL,
                Severity.ERROR,
                "Failed to get balance: " + response.getError().getMessage()));
      }
      return Result.ok(response.getBalance());
    } catch (IOException e) {
      logger.error("Error getting balance for address: {}", address, e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("BLOCKCHAIN.NETWORK_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Network error: " + e.getMessage()));
    }
  }

  @Override
  public Result<BlockchainTransaction> getTransaction(String transactionHash) {
    try {
      var response = web3j.ethGetTransactionByHash(transactionHash).send();
      if (response.hasError()) {
        return Result.fail(
            Problem.of(
                ErrorCode.of("BLOCKCHAIN.NETWORK_ERROR"),
                ErrorCategory.TECHNICAL,
                Severity.ERROR,
                "Failed to get transaction: " + response.getError().getMessage()));
      }

      var tx = response.getTransaction();
      if (tx.isEmpty()) {
        return Result.fail(
            Problem.of(
                ErrorCode.of("BLOCKCHAIN.NOT_FOUND"),
                ErrorCategory.BUSINESS,
                Severity.WARNING,
                "Transaction not found: " + transactionHash));
      }

      var transaction = tx.get();
      var status = BlockchainTransaction.TransactionStatus.PENDING;
      Optional<Instant> timestamp = Optional.empty();

      // Check if transaction is mined
      if (transaction.getBlockNumber() != null) {
        var receiptResult = getTransactionReceipt(transactionHash);
        if (receiptResult.isOk()) {
          var receipt = receiptResult.getOrNull();
          status =
              receipt.status()
                  ? BlockchainTransaction.TransactionStatus.SUCCESS
                  : BlockchainTransaction.TransactionStatus.FAILED;

          // Get block timestamp
          try {
            var blockResponse =
                web3j
                    .ethGetBlockByNumber(
                        org.web3j.protocol.core.DefaultBlockParameter.valueOf(
                            transaction.getBlockNumber()),
                        false)
                    .send();
            if (!blockResponse.hasError() && blockResponse.getBlock() != null) {
              timestamp =
                  Optional.of(
                      Instant.ofEpochSecond(
                          blockResponse.getBlock().getTimestamp().longValueExact()));
            }
          } catch (Exception e) {
            logger.warn("Could not get block timestamp", e);
          }
        }
      }

      return Result.ok(
          BlockchainTransaction.builder()
              .hash(transaction.getHash())
              .from(transaction.getFrom())
              .to(transaction.getTo())
              .value(transaction.getValue())
              .gasPrice(transaction.getGasPrice())
              .gasLimit(transaction.getGas())
              .nonce(transaction.getNonce())
              .data(transaction.getInput())
              .blockNumber(transaction.getBlockNumber())
              .blockHash(transaction.getBlockHash())
              .timestamp(timestamp.orElse(null))
              .status(status)
              .build());
    } catch (IOException e) {
      logger.error("Error getting transaction: {}", transactionHash, e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("BLOCKCHAIN.NETWORK_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Network error: " + e.getMessage()));
    }
  }

  @Override
  public Result<TransactionReceipt> getTransactionReceipt(String transactionHash) {
    try {
      var response = web3j.ethGetTransactionReceipt(transactionHash).send();
      if (response.hasError()) {
        return Result.fail(
            Problem.of(
                ErrorCode.of("BLOCKCHAIN.NETWORK_ERROR"),
                ErrorCategory.TECHNICAL,
                Severity.ERROR,
                "Failed to get transaction receipt: " + response.getError().getMessage()));
      }

      var receipt = response.getTransactionReceipt();
      if (receipt.isEmpty()) {
        return Result.fail(
            Problem.of(
                ErrorCode.of("BLOCKCHAIN.NOT_FOUND"),
                ErrorCategory.BUSINESS,
                Severity.WARNING,
                "Transaction receipt not found: " + transactionHash));
      }

      var r = receipt.get();
      var logs =
          r.getLogs().stream()
              .map(
                  log ->
                      new TransactionReceipt.Log(
                          log.getAddress(),
                          log.getTopics(),
                          log.getData(),
                          log.getBlockNumber(),
                          log.getBlockHash()))
              .toList();

      return Result.ok(
          TransactionReceipt.builder()
              .transactionHash(r.getTransactionHash())
              .blockNumber(r.getBlockNumber())
              .blockHash(r.getBlockHash())
              .from(r.getFrom())
              .to(r.getTo())
              .gasUsed(r.getGasUsed())
              .cumulativeGasUsed(r.getCumulativeGasUsed())
              .contractAddress(r.getContractAddress())
              .status(r.isStatusOK())
              .logs(logs)
              .build());
    } catch (IOException e) {
      logger.error("Error getting transaction receipt: {}", transactionHash, e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("BLOCKCHAIN.NETWORK_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Network error: " + e.getMessage()));
    }
  }

  @Override
  public Result<String> sendTransaction(String signedTransaction) {
    try {
      var response = web3j.ethSendRawTransaction(signedTransaction).send();
      if (response.hasError()) {
        return Result.fail(
            Problem.of(
                ErrorCode.of("BLOCKCHAIN.NETWORK_ERROR"),
                ErrorCategory.TECHNICAL,
                Severity.ERROR,
                "Failed to send transaction: " + response.getError().getMessage()));
      }
      return Result.ok(response.getTransactionHash());
    } catch (IOException e) {
      logger.error("Error sending transaction", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("BLOCKCHAIN.NETWORK_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Network error: " + e.getMessage()));
    }
  }

  @Override
  public Result<BigInteger> getTransactionCount(String address) {
    try {
      var response = web3j.ethGetTransactionCount(address, DefaultBlockParameterName.LATEST).send();
      if (response.hasError()) {
        return Result.fail(
            Problem.of(
                ErrorCode.of("BLOCKCHAIN.NETWORK_ERROR"),
                ErrorCategory.TECHNICAL,
                Severity.ERROR,
                "Failed to get transaction count: " + response.getError().getMessage()));
      }
      return Result.ok(response.getTransactionCount());
    } catch (IOException e) {
      logger.error("Error getting transaction count for address: {}", address, e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("BLOCKCHAIN.NETWORK_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Network error: " + e.getMessage()));
    }
  }

  @Override
  public Result<BigInteger> estimateGas(String from, String to, BigInteger value, String data) {
    try {
      var transaction =
          Transaction.createFunctionCallTransaction(from, null, null, null, to, value, data);
      var response = web3j.ethEstimateGas(transaction).send();
      if (response.hasError()) {
        return Result.fail(
            Problem.of(
                ErrorCode.of("BLOCKCHAIN.NETWORK_ERROR"),
                ErrorCategory.TECHNICAL,
                Severity.ERROR,
                "Failed to estimate gas: " + response.getError().getMessage()));
      }
      return Result.ok(response.getAmountUsed());
    } catch (IOException e) {
      logger.error("Error estimating gas", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("BLOCKCHAIN.NETWORK_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Network error: " + e.getMessage()));
    }
  }

  @Override
  public Result<BigInteger> getGasPrice() {
    try {
      var response = web3j.ethGasPrice().send();
      if (response.hasError()) {
        return Result.fail(
            Problem.of(
                ErrorCode.of("BLOCKCHAIN.NETWORK_ERROR"),
                ErrorCategory.TECHNICAL,
                Severity.ERROR,
                "Failed to get gas price: " + response.getError().getMessage()));
      }
      return Result.ok(response.getGasPrice());
    } catch (IOException e) {
      logger.error("Error getting gas price", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("BLOCKCHAIN.NETWORK_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Network error: " + e.getMessage()));
    }
  }

  @Override
  public Result<String> call(String contractAddress, String functionCall) {
    try {
      var transaction = Transaction.createEthCallTransaction(null, contractAddress, functionCall);
      var response = web3j.ethCall(transaction, DefaultBlockParameterName.LATEST).send();
      if (response.hasError()) {
        return Result.fail(
            Problem.of(
                ErrorCode.of("BLOCKCHAIN.NETWORK_ERROR"),
                ErrorCategory.TECHNICAL,
                Severity.ERROR,
                "Failed to call contract: " + response.getError().getMessage()));
      }
      return Result.ok(response.getValue());
    } catch (IOException e) {
      logger.error("Error calling contract", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("BLOCKCHAIN.NETWORK_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Network error: " + e.getMessage()));
    }
  }

  @Override
  public Result<String> getNetworkId() {
    try {
      var response = web3j.netVersion().send();
      if (response.hasError()) {
        return Result.fail(
            Problem.of(
                ErrorCode.of("BLOCKCHAIN.NETWORK_ERROR"),
                ErrorCategory.TECHNICAL,
                Severity.ERROR,
                "Failed to get network ID: " + response.getError().getMessage()));
      }
      return Result.ok(response.getNetVersion());
    } catch (IOException e) {
      logger.error("Error getting network ID", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("BLOCKCHAIN.NETWORK_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Network error: " + e.getMessage()));
    }
  }

  @Override
  public Result<Boolean> isConnected() {
    try {
      var response = web3j.web3ClientVersion().send();
      return Result.ok(!response.hasError());
    } catch (Exception e) {
      logger.error("Error checking connection", e);
      return Result.ok(false);
    }
  }
}
