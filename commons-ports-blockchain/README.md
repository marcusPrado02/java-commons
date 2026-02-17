# Commons Blockchain - Port Interfaces

Port interfaces for blockchain interactions following hexagonal architecture.

## Overview

This module defines the port interfaces for blockchain operations including:

- Connecting to blockchain networks (Ethereum)
- Querying blocks, transactions, and balances
- Managing wallets and signing transactions
- Deploying and interacting with smart contracts
- Listening to contract events

## Features

- **Blockchain Client**: Query blockchain state and send transactions
- **Wallet Manager**: Create and manage wallets with keystore support
- **Smart Contract Client**: Deploy and interact with smart contracts
- **Transaction Models**: Immutable transaction and receipt models
- **Result Pattern**: Consistent error handling with `Result<T>`

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-ports-blockchain</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Port Interfaces

### BlockchainClient

Provides methods for blockchain operations:

```java
public interface BlockchainClient {
    Result<BigInteger> getBlockNumber();
    Result<BigInteger> getBalance(String address);
    Result<BlockchainTransaction> getTransaction(String transactionHash);
    Result<TransactionReceipt> getTransactionReceipt(String transactionHash);
    Result<String> sendTransaction(String signedTransaction);
    Result<BigInteger> getTransactionCount(String address);
    Result<BigInteger> estimateGas(String from, String to, BigInteger value, String data);
    Result<BigInteger> getGasPrice();
    Result<String> call(String contractAddress, String functionCall);
    Result<String> getNetworkId();
    Result<Boolean> isConnected();
}
```

### WalletManager

Provides methods for wallet operations:

```java
public interface WalletManager {
    Result<Wallet> createWallet();
    Result<Wallet> loadWallet(String privateKey);
    Result<String> signTransaction(BlockchainTransaction transaction, String privateKey);
    Result<String> signData(String data, String privateKey);
    Result<Boolean> verifySignature(String data, String signature, String address);
    Result<String> exportKeystore(String privateKey, String password);
    Result<Wallet> importKeystore(String keystoreJson, String password);
}
```

### SmartContractClient

Provides methods for smart contract operations:

```java
public interface SmartContractClient {
    Result<SmartContract> deployContract(
        String bytecode,
        String abi,
        List<Object> constructorParams,
        String from,
        BigInteger gasLimit,
        BigInteger gasPrice
    );

    Result<List<Object>> callFunction(
        SmartContract contract,
        String functionName,
        List<Object> params
    );

    Result<TransactionReceipt> sendTransaction(
        SmartContract contract,
        String functionName,
        List<Object> params,
        String from,
        BigInteger gasLimit,
        BigInteger gasPrice,
        BigInteger value
    );

    Result<List<ContractEvent>> getEvents(
        SmartContract contract,
        String eventName,
        BigInteger fromBlock,
        BigInteger toBlock
    );

    Result<String> encodeFunctionCall(String functionName, List<Object> params);
    Result<List<Object>> decodeFunctionOutput(String functionName, String encodedOutput);
}
```

## Domain Models

### BlockchainTransaction

Represents a blockchain transaction:

```java
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
    TransactionStatus status
) {
    public enum TransactionStatus {
        PENDING, SUCCESS, FAILED
    }
}
```

### TransactionReceipt

Represents a transaction receipt:

```java
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
    List<Log> logs
) {
    public record Log(
        String address,
        List<String> topics,
        String data,
        BigInteger blockNumber,
        String blockHash
    ) {}
}
```

### Wallet

Represents a blockchain wallet:

```java
public record Wallet(
    String address,
    String publicKey,
    BigInteger balance
) {}
```

### SmartContract

Represents a deployed smart contract:

```java
public record SmartContract(
    String address,
    String abi,
    String bytecode
) {}
```

## Usage Examples

### Query Blockchain

```java
BlockchainClient client = // get from DI

// Get current block
var blockResult = client.getBlockNumber();
blockResult.ifOk(block ->
    System.out.println("Current block: " + block));

// Get balance
var balanceResult = client.getBalance("0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb");
balanceResult.ifOk(balance ->
    System.out.println("Balance: " + balance + " wei"));

// Get transaction
var txResult = client.getTransaction("0x123...");
txResult.ifOk(tx -> {
    System.out.println("From: " + tx.from());
    System.out.println("To: " + tx.to());
    System.out.println("Status: " + tx.status());
});
```

### Manage Wallets

```java
WalletManager manager = // get from DI

// Create wallet
var walletResult = manager.createWallet();
walletResult.ifOk(wallet ->
    System.out.println("Address: " + wallet.address()));

// Load wallet
var loadResult = manager.loadWallet(privateKey);

// Sign transaction
var transaction = BlockchainTransaction.builder()
    .from("0x...")
    .to("0x...")
    .value(BigInteger.valueOf(1000))
    .build();

var signResult = manager.signTransaction(transaction, privateKey);
```

### Smart Contracts

```java
SmartContractClient contractClient = // get from DI

// Call function
var contract = SmartContract.builder()
    .address("0x...")
    .abi("[...]")
    .build();

var result = contractClient.callFunction(contract, "balanceOf", List.of(address));
result.ifOk(values ->
    System.out.println("Balance: " + values.get(0)));

// Listen to events
var events = contractClient.getEvents(
    contract,
    "Transfer",
    BigInteger.ZERO,
    BigInteger.valueOf(1000)
);
```

## Error Handling

All operations return `Result<T>`:

```java
var result = client.getBalance(address);

if (result.isOk()) {
    var balance = result.getOrNull();
    // handle success
} else {
    var problem = result.problemOrNull();
    // handle error
}
```

## Implementing an Adapter

To implement these ports for a specific blockchain platform:

1. Add dependency to this module
2. Implement the three port interfaces
3. Use appropriate blockchain client library
4. Return `Result<T>` for all operations
5. Use `Problem` for error reporting

Example:

```java
public class MyBlockchainClient implements BlockchainClient {

    @Override
    public Result<BigInteger> getBalance(String address) {
        try {
            // Call blockchain API
            var balance = api.getBalance(address);
            return Result.ok(balance);
        } catch (Exception e) {
            return Result.fail(Problem.of(
                ErrorCode.EXTERNAL_SERVICE_ERROR,
                ErrorCategory.EXTERNAL,
                Severity.ERROR,
                "Failed to get balance: " + e.getMessage()
            ));
        }
    }

    // Implement other methods...
}
```

## Available Implementations

- **Web3j**: `commons-adapters-blockchain-web3j` - Ethereum integration using Web3j library

## License

This project is licensed under the terms of the LICENSE file in the repository root.
