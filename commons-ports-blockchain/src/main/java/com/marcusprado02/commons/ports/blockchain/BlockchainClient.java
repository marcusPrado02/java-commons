package com.marcusprado02.commons.ports.blockchain;

import com.marcusprado02.commons.kernel.result.Result;
import java.math.BigInteger;

/**
 * Port interface for blockchain interactions.
 *
 * <p>Provides methods for connecting to blockchain networks, querying blocks and transactions, and
 * submitting transactions.
 */
public interface BlockchainClient {

  /**
   * Gets the current block number.
   *
   * @return the result containing the current block number
   */
  Result<BigInteger> getBlockNumber();

  /**
   * Gets the balance of an address.
   *
   * @param address the address to query
   * @return the result containing the balance in wei
   */
  Result<BigInteger> getBalance(String address);

  /**
   * Gets a transaction by hash.
   *
   * @param transactionHash the transaction hash
   * @return the result containing the transaction
   */
  Result<BlockchainTransaction> getTransaction(String transactionHash);

  /**
   * Gets a transaction receipt.
   *
   * @param transactionHash the transaction hash
   * @return the result containing the transaction receipt with status and logs
   */
  Result<TransactionReceipt> getTransactionReceipt(String transactionHash);

  /**
   * Sends a signed transaction.
   *
   * @param signedTransaction the signed transaction hex
   * @return the result containing the transaction hash
   */
  Result<String> sendTransaction(String signedTransaction);

  /**
   * Gets the transaction count (nonce) for an address.
   *
   * @param address the address
   * @return the result containing the transaction count
   */
  Result<BigInteger> getTransactionCount(String address);

  /**
   * Estimates gas for a transaction.
   *
   * @param from sender address
   * @param to recipient address
   * @param value amount in wei
   * @param data transaction data
   * @return the result containing estimated gas
   */
  Result<BigInteger> estimateGas(String from, String to, BigInteger value, String data);

  /**
   * Gets the current gas price.
   *
   * @return the result containing gas price in wei
   */
  Result<BigInteger> getGasPrice();

  /**
   * Calls a contract function (read-only, no transaction).
   *
   * @param contractAddress contract address
   * @param functionCall encoded function call
   * @return the result containing the function output
   */
  Result<String> call(String contractAddress, String functionCall);

  /**
   * Gets blockchain network version/chain ID.
   *
   * @return the result containing the network ID
   */
  Result<String> getNetworkId();

  /**
   * Checks if the client is connected to the network.
   *
   * @return the result containing true if connected
   */
  Result<Boolean> isConnected();
}
