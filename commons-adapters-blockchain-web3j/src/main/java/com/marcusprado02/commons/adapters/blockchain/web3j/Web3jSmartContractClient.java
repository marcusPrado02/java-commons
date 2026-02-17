package com.marcusprado02.commons.adapters.blockchain.web3j;

import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.blockchain.SmartContract;
import com.marcusprado02.commons.ports.blockchain.SmartContractClient;
import com.marcusprado02.commons.ports.blockchain.TransactionReceipt;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.request.Transaction;

/**
 * Web3j implementation of SmartContractClient.
 *
 * <p>Provides smart contract operations using Web3j library.
 */
public final class Web3jSmartContractClient implements SmartContractClient {

  private static final Logger logger = LoggerFactory.getLogger(Web3jSmartContractClient.class);
  private final Web3j web3j;

  private Web3jSmartContractClient(Web3j web3j) {
    this.web3j = web3j;
  }

  /**
   * Creates a Web3jSmartContractClient.
   *
   * @param web3j the Web3j instance
   * @return a new Web3jSmartContractClient
   */
  public static Web3jSmartContractClient create(Web3j web3j) {
    if (web3j == null) {
      throw new IllegalArgumentException("Web3j instance cannot be null");
    }
    return new Web3jSmartContractClient(web3j);
  }

  @Override
  public Result<SmartContract> deployContract(
      String bytecode,
      String abi,
      List<Object> constructorParams,
      String from,
      BigInteger gasLimit,
      BigInteger gasPrice) {
    try {
      // For deployment, we need credentials - this is a limitation
      // In a real implementation, you'd need to integrate with WalletManager
      return Result.fail(
          Problem.of(
              ErrorCode.of("BLOCKCHAIN.NOT_IMPLEMENTED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Contract deployment requires transaction signing. Use a full transaction flow with WalletManager."));
    } catch (Exception e) {
      logger.error("Error deploying contract", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("BLOCKCHAIN.INTERNAL_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to deploy contract: " + e.getMessage()));
    }
  }

  @Override
  public Result<List<Object>> callFunction(
      SmartContract contract, String functionName, List<Object> params) {
    try {
      // Convert params to Web3j types
      var inputParameters = convertToWeb3jTypes(params);
      List<TypeReference<?>> outputParameters = Collections.emptyList();

      // Create function
      var function = new Function(functionName, inputParameters, outputParameters);
      var encodedFunction = FunctionEncoder.encode(function);

      // Call contract
      var transaction =
          Transaction.createEthCallTransaction(null, contract.address(), encodedFunction);
      var response = web3j.ethCall(transaction, DefaultBlockParameterName.LATEST).send();

      if (response.hasError()) {
        return Result.fail(
            Problem.of(
                ErrorCode.of("BLOCKCHAIN.NETWORK_ERROR"),
                ErrorCategory.TECHNICAL,
                Severity.ERROR,
                "Failed to call function: " + response.getError().getMessage()));
      }

      // Decode response
      var returnValues =
          FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
      var results = returnValues.stream().map(Type::getValue).toList();

      return Result.ok(results);
    } catch (IOException e) {
      logger.error("Error calling contract function: {}", functionName, e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("BLOCKCHAIN.NETWORK_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Network error: " + e.getMessage()));
    } catch (Exception e) {
      logger.error("Error calling contract function: {}", functionName, e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("BLOCKCHAIN.INTERNAL_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to call function: " + e.getMessage()));
    }
  }

  @Override
  public Result<TransactionReceipt> sendTransaction(
      SmartContract contract,
      String functionName,
      List<Object> params,
      String from,
      BigInteger gasLimit,
      BigInteger gasPrice,
      BigInteger value) {
    try {
      // This requires transaction signing - needs integration with WalletManager
      return Result.fail(
          Problem.of(
              ErrorCode.of("BLOCKCHAIN.NOT_IMPLEMENTED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Contract transaction requires signing. Use a full transaction flow with WalletManager."));
    } catch (Exception e) {
      logger.error("Error sending contract transaction", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("BLOCKCHAIN.INTERNAL_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to send transaction: " + e.getMessage()));
    }
  }

  @Override
  public Result<List<ContractEvent>> getEvents(
      SmartContract contract, String eventName, BigInteger fromBlock, BigInteger toBlock) {
    try {
      // Create event signature
      // Note: This is a simplified implementation
      // In a real scenario, you'd parse the ABI to get event parameters
      var event = new Event(eventName, Collections.emptyList());
      var eventSignature = EventEncoder.encode(event);

      // Create filter
      var filter =
          new EthFilter(
              DefaultBlockParameter.valueOf(fromBlock),
              DefaultBlockParameter.valueOf(toBlock),
              contract.address());
      filter.addSingleTopic(eventSignature);

      // Get logs
      var response = web3j.ethGetLogs(filter).send();
      if (response.hasError()) {
        return Result.fail(
            Problem.of(
                ErrorCode.of("BLOCKCHAIN.NETWORK_ERROR"),
                ErrorCategory.TECHNICAL,
                Severity.ERROR,
                "Failed to get events: " + response.getError().getMessage()));
      }

      var events =
          response.getLogs().stream()
              .map(
                  log -> {
                    var ethLog = (org.web3j.protocol.core.methods.response.EthLog.LogObject) log;
                    var parameters = new ArrayList<ContractEvent.EventParameter>();

                    // Parse event parameters from topics and data
                    // This is simplified - real implementation would decode based on ABI
                    for (int i = 0; i < ethLog.getTopics().size(); i++) {
                      parameters.add(
                          new ContractEvent.EventParameter(
                              "param" + i, ethLog.getTopics().get(i), true));
                    }

                    return new ContractEvent(
                        eventName,
                        ethLog.getBlockNumber(),
                        ethLog.getTransactionHash(),
                        parameters);
                  })
              .toList();

      return Result.ok(events);
    } catch (IOException e) {
      logger.error("Error getting contract events: {}", eventName, e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("BLOCKCHAIN.NETWORK_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Network error: " + e.getMessage()));
    } catch (Exception e) {
      logger.error("Error getting contract events: {}", eventName, e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("BLOCKCHAIN.INTERNAL_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to get events: " + e.getMessage()));
    }
  }

  @Override
  public Result<String> encodeFunctionCall(String functionName, List<Object> params) {
    try {
      var inputParameters = convertToWeb3jTypes(params);
      var function = new Function(functionName, inputParameters, Collections.emptyList());
      var encoded = FunctionEncoder.encode(function);
      return Result.ok(encoded);
    } catch (Exception e) {
      logger.error("Error encoding function call: {}", functionName, e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("BLOCKCHAIN.INTERNAL_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to encode function: " + e.getMessage()));
    }
  }

  @Override
  public Result<List<Object>> decodeFunctionOutput(String functionName, String encodedOutput) {
    try {
      // This is simplified - in reality you'd need the function ABI to decode properly
      List<TypeReference<?>> outputParameters = Collections.emptyList();
      var function = new Function(functionName, Collections.emptyList(), outputParameters);
      var returnValues =
          FunctionReturnDecoder.decode(encodedOutput, function.getOutputParameters());
      var results = returnValues.stream().map(Type::getValue).toList();
      return Result.ok(results);
    } catch (Exception e) {
      logger.error("Error decoding function output: {}", functionName, e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("BLOCKCHAIN.INTERNAL_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to decode output: " + e.getMessage()));
    }
  }

  private List<Type> convertToWeb3jTypes(List<Object> params) {
    // Simplified conversion - in reality you'd need type information from ABI
    var types = new ArrayList<Type>();
    for (var param : params) {
      if (param instanceof BigInteger bigInt) {
        types.add(new org.web3j.abi.datatypes.generated.Uint256(bigInt));
      } else if (param instanceof String str) {
        types.add(new org.web3j.abi.datatypes.Utf8String(str));
      } else if (param instanceof Boolean bool) {
        types.add(new org.web3j.abi.datatypes.Bool(bool));
      }
      // Add more type conversions as needed
    }
    return types;
  }
}
