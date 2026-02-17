package com.marcusprado02.commons.ports.blockchain;

import com.marcusprado02.commons.kernel.result.Result;
import java.math.BigInteger;
import java.util.List;

/**
 * Port interface for smart contract interactions.
 *
 * <p>Provides methods for deploying contracts, calling functions, and listening to events.
 */
public interface SmartContractClient {

  /**
   * Deploys a smart contract.
   *
   * @param bytecode contract bytecode
   * @param abi contract ABI
   * @param constructorParams constructor parameters
   * @param from deployer address
   * @param gasLimit gas limit
   * @param gasPrice gas price
   * @return the result containing the deployed contract
   */
  Result<SmartContract> deployContract(
      String bytecode,
      String abi,
      List<Object> constructorParams,
      String from,
      BigInteger gasLimit,
      BigInteger gasPrice);

  /**
   * Calls a contract function (read-only, no transaction).
   *
   * @param contract the smart contract
   * @param functionName function name
   * @param params function parameters
   * @return the result containing the function return values
   */
  Result<List<Object>> callFunction(
      SmartContract contract, String functionName, List<Object> params);

  /**
   * Sends a transaction to a contract function (write operation).
   *
   * @param contract the smart contract
   * @param functionName function name
   * @param params function parameters
   * @param from sender address
   * @param gasLimit gas limit
   * @param gasPrice gas price
   * @param value amount to send in wei
   * @return the result containing the transaction receipt
   */
  Result<TransactionReceipt> sendTransaction(
      SmartContract contract,
      String functionName,
      List<Object> params,
      String from,
      BigInteger gasLimit,
      BigInteger gasPrice,
      BigInteger value);

  /**
   * Listens to contract events.
   *
   * @param contract the smart contract
   * @param eventName event name
   * @param fromBlock starting block number
   * @param toBlock ending block number
   * @return the result containing the list of event logs
   */
  Result<List<ContractEvent>> getEvents(
      SmartContract contract, String eventName, BigInteger fromBlock, BigInteger toBlock);

  /**
   * Encodes function call data.
   *
   * @param functionName function name
   * @param params function parameters
   * @return the result containing the encoded data
   */
  Result<String> encodeFunctionCall(String functionName, List<Object> params);

  /**
   * Decodes function output.
   *
   * @param functionName function name
   * @param encodedOutput encoded output data
   * @return the result containing decoded output values
   */
  Result<List<Object>> decodeFunctionOutput(String functionName, String encodedOutput);

  /** Represents a contract event. */
  record ContractEvent(
      String eventName,
      BigInteger blockNumber,
      String transactionHash,
      List<EventParameter> parameters) {

    public ContractEvent {
      parameters = parameters == null ? List.of() : List.copyOf(parameters);
    }

    /** Event parameter with name and value. */
    public record EventParameter(String name, Object value, boolean indexed) {}
  }
}
