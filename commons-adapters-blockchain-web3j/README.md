# Commons Blockchain - Web3j Adapter

Web3j adapter implementation for Ethereum blockchain integration.

## Features

- **Blockchain Client**: Connect to Ethereum nodes (mainnet, testnet, local)
- **Wallet Management**: Create, load, and manage wallets with keystore support
- **Smart Contracts**: Deploy and interact with smart contracts
- **Transaction Signing**: Sign transactions and arbitrary data
- **Event Listening**: Listen to smart contract events
- **Result Pattern**: Consistent error handling with `Result<T>`

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-adapters-blockchain-web3j</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Quick Start

### Blockchain Client

```java
import com.marcusprado02.commons.adapters.blockchain.web3j.Web3jBlockchainClient;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

// Create Web3j instance
Web3j web3j = Web3j.build(new HttpService("http://localhost:8545"));

// Create blockchain client
BlockchainClient client = Web3jBlockchainClient.create(web3j);

// Get current block number
var blockResult = client.getBlockNumber();
blockResult.ifOk(blockNumber ->
    System.out.println("Current block: " + blockNumber));

// Get balance
var balanceResult = client.getBalance("0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb");
balanceResult.ifOk(balance ->
    System.out.println("Balance: " + balance + " wei"));

// Get transaction
var txResult = client.getTransaction("0x123...");
txResult.ifOk(tx -> {
    System.out.println("From: " + tx.from());
    System.out.println("To: " + tx.to());
    System.out.println("Value: " + tx.value());
});
```

### Wallet Management

```java
import com.marcusprado02.commons.adapters.blockchain.web3j.Web3jWalletManager;

// Create wallet manager
WalletManager walletManager = Web3jWalletManager.create();

// Create new wallet
var walletResult = walletManager.createWallet();
walletResult.ifOk(wallet -> {
    System.out.println("Address: " + wallet.address());
    System.out.println("Public Key: " + wallet.publicKey());
});

// Load wallet from private key
var loadResult = walletManager.loadWallet("0x4c0883a69102937d...");
loadResult.ifOk(wallet ->
    System.out.println("Loaded wallet: " + wallet.address()));

// Sign transaction
var transaction = BlockchainTransaction.builder()
    .from("0x...")
    .to("0x...")
    .value(BigInteger.valueOf(1000))
    .gasPrice(BigInteger.valueOf(20000000000L))
    .gasLimit(BigInteger.valueOf(21000))
    .nonce(BigInteger.ZERO)
    .data("0x")
    .build();

var signResult = walletManager.signTransaction(transaction, privateKey);
signResult.ifOk(signedTx -> {
    // Send signed transaction
    client.sendTransaction(signedTx);
});
```

### Keystore Management

```java
// Export wallet to encrypted keystore
var keystoreResult = walletManager.exportKeystore(privateKey, "password");
keystoreResult.ifOk(keystoreJson -> {
    // Save keystore to file
    Files.writeString(Path.of("wallet.json"), keystoreJson);
});

// Import wallet from keystore
var importResult = walletManager.importKeystore(keystoreJson, "password");
importResult.ifOk(wallet ->
    System.out.println("Imported wallet: " + wallet.address()));
```

### Sign and Verify Data

```java
// Sign arbitrary data
var data = "Hello, Ethereum!";
var signResult = walletManager.signData(data, privateKey);
signResult.ifOk(signature ->
    System.out.println("Signature: " + signature));

// Verify signature
var verifyResult = walletManager.verifySignature(data, signature, address);
verifyResult.ifOk(isValid ->
    System.out.println("Signature valid: " + isValid));
```

### Smart Contracts

```java
import com.marcusprado02.commons.adapters.blockchain.web3j.Web3jSmartContractClient;

// Create smart contract client
SmartContractClient contractClient = Web3jSmartContractClient.create(web3j);

// Create contract reference
var contract = SmartContract.builder()
    .address("0x123...")
    .abi("[{\"constant\":true,\"inputs\":[],\"name\":\"name\",\"outputs\":[{\"name\":\"\",\"type\":\"string\"}],\"type\":\"function\"}]")
    .build();

// Call read-only function
var nameResult = contractClient.callFunction(contract, "name", List.of());
nameResult.ifOk(result ->
    System.out.println("Contract name: " + result.get(0)));

// Listen to events
var eventsResult = contractClient.getEvents(
    contract,
    "Transfer",
    BigInteger.ZERO,
    BigInteger.valueOf(1000)
);
eventsResult.ifOk(events -> {
    for (var event : events) {
        System.out.println("Event: " + event.eventName());
        System.out.println("Block: " + event.blockNumber());
        System.out.println("Tx: " + event.transactionHash());
    }
});

// Encode function call
var encodeResult = contractClient.encodeFunctionCall("transfer", List.of(
    "0x...",  // recipient
    BigInteger.valueOf(100)  // amount
));
encodeResult.ifOk(encoded ->
    System.out.println("Encoded: " + encoded));
```

## Network Configuration

### Mainnet

```java
Web3j mainnet = Web3j.build(
    new HttpService("https://mainnet.infura.io/v3/YOUR-PROJECT-ID")
);
```

### Testnets

```java
// Sepolia
Web3j sepolia = Web3j.build(
    new HttpService("https://sepolia.infura.io/v3/YOUR-PROJECT-ID")
);

// Goerli
Web3j goerli = Web3j.build(
    new HttpService("https://goerli.infura.io/v3/YOUR-PROJECT-ID")
);
```

### Local Ganache

```java
Web3j ganache = Web3j.build(
    new HttpService("http://localhost:8545")
);
```

### WebSocket Connection

```java
Web3j web3j = Web3j.build(
    new WebSocketService("wss://mainnet.infura.io/ws/v3/YOUR-PROJECT-ID", true)
);
```

## Transaction Flow

Complete transaction flow with signing and sending:

```java
// 1. Get current nonce
var nonceResult = client.getTransactionCount(fromAddress);
var nonce = nonceResult.getOrNull();

// 2. Get gas price
var gasPriceResult = client.getGasPrice();
var gasPrice = gasPriceResult.getOrNull();

// 3. Estimate gas
var gasResult = client.estimateGas(
    fromAddress,
    toAddress,
    value,
    "0x"
);
var gasLimit = gasResult.getOrNull();

// 4. Create transaction
var transaction = BlockchainTransaction.builder()
    .from(fromAddress)
    .to(toAddress)
    .value(value)
    .gasPrice(gasPrice)
    .gasLimit(gasLimit)
    .nonce(nonce)
    .data("0x")
    .build();

// 5. Sign transaction
var signResult = walletManager.signTransaction(transaction, privateKey);
var signedTx = signResult.getOrNull();

// 6. Send transaction
var sendResult = client.sendTransaction(signedTx);
sendResult.ifOk(txHash -> {
    System.out.println("Transaction hash: " + txHash);

    // 7. Wait for confirmation (poll receipt)
    var receiptResult = client.getTransactionReceipt(txHash);
    receiptResult.ifOk(receipt -> {
        if (receipt.status()) {
            System.out.println("Transaction successful!");
        } else {
            System.out.println("Transaction failed!");
        }
    });
});
```

## Testing with Ganache

Use Testcontainers to run integration tests with Ganache:

```java
@Testcontainers
class BlockchainTest {

    @Container
    private static final GenericContainer<?> ganache =
        new GenericContainer<>(DockerImageName.parse("trufflesuite/ganache:v7.9.1"))
            .withExposedPorts(8545)
            .withCommand("--deterministic", "--accounts", "10");

    private static Web3j web3j;
    private static Web3jBlockchainClient client;

    @BeforeAll
    static void setUp() {
        var rpcUrl = "http://" + ganache.getHost() + ":" + ganache.getMappedPort(8545);
        web3j = Web3j.build(new HttpService(rpcUrl));
        client = Web3jBlockchainClient.create(web3j);
    }

    @Test
    void testGetBalance() {
        // Ganache deterministic address with funds
        var address = "0x90F8bf6A479f320ead074411a4B0e7944Ea8c9C1";

        var result = client.getBalance(address);

        assertThat(result.isOk()).isTrue();
        assertThat(result.getOrNull()).isGreaterThan(BigInteger.ZERO);
    }
}
```

## Error Handling

All operations return `Result<T>` which can be either success or failure:

```java
var result = client.getBalance(address);

// Pattern matching style
if (result.isOk()) {
    BigInteger balance = result.getOrNull();
    System.out.println("Balance: " + balance);
} else {
    Problem problem = result.problemOrNull();
    System.err.println("Error: " + problem.message());
    System.err.println("Code: " + problem.code());
    System.err.println("Category: " + problem.category());
}

// Functional style
result
    .ifOk(balance -> System.out.println("Balance: " + balance))
    .ifFail(problem -> System.err.println("Error: " + problem.message()));

// Map and flatMap
var doubled = result.map(balance -> balance.multiply(BigInteger.TWO));
```

## Gas Management

### Get Current Gas Price

```java
var gasPriceResult = client.getGasPrice();
gasPriceResult.ifOk(gasPrice ->
    System.out.println("Gas price: " + gasPrice + " wei"));
```

### Estimate Gas for Transaction

```java
var gasResult = client.estimateGas(
    fromAddress,
    toAddress,
    value,
    data
);
gasResult.ifOk(gas ->
    System.out.println("Estimated gas: " + gas));
```

### Calculate Transaction Cost

```java
var gasPriceResult = client.getGasPrice();
var gasResult = client.estimateGas(from, to, value, data);

if (gasPriceResult.isOk() && gasResult.isOk()) {
    var gasPrice = gasPriceResult.getOrNull();
    var gas = gasResult.getOrNull();
    var cost = gasPrice.multiply(gas);
    System.out.println("Transaction cost: " + cost + " wei");
}
```

## Security Best Practices

1. **Never hardcode private keys** - Use environment variables or secure vaults
2. **Use keystore files** - Encrypt private keys with strong passwords
3. **Validate addresses** - Always validate Ethereum addresses before use
4. **Check transaction receipts** - Always verify transaction success
5. **Use appropriate networks** - Test on testnets before mainnet
6. **Monitor gas prices** - Check gas prices to avoid overpaying
7. **Set gas limits** - Always set reasonable gas limits
8. **Handle errors** - Always check Result status and handle failures

## Performance Considerations

- **Connection Pooling**: Web3j uses HTTP connection pooling by default
- **Batch Requests**: For multiple queries, consider batching
- **WebSocket**: Use WebSocket for real-time event subscriptions
- **Caching**: Cache frequently accessed data (balance, nonce)
- **Async Operations**: Web3j supports async calls with `sendAsync()`

## Common Issues

### Connection Refused

```
Problem: Connection to Ethereum node refused
Solution: Check node URL and ensure node is running
```

### Insufficient Funds

```
Problem: Transaction fails with "insufficient funds"
Solution: Ensure sender has enough balance for value + gas
```

### Nonce Too Low

```
Problem: Transaction fails with "nonce too low"
Solution: Get current nonce with getTransactionCount()
```

### Gas Limit Too Low

```
Problem: Transaction fails with "out of gas"
Solution: Increase gas limit or use estimateGas()
```

## Integration with Spring Boot

```java
@Configuration
public class BlockchainConfig {

    @Value("${blockchain.rpc.url}")
    private String rpcUrl;

    @Bean
    public Web3j web3j() {
        return Web3j.build(new HttpService(rpcUrl));
    }

    @Bean
    public BlockchainClient blockchainClient(Web3j web3j) {
        return Web3jBlockchainClient.create(web3j);
    }

    @Bean
    public WalletManager walletManager() {
        return Web3jWalletManager.create();
    }

    @Bean
    public SmartContractClient smartContractClient(Web3j web3j) {
        return Web3jSmartContractClient.create(web3j);
    }

    @PreDestroy
    public void shutdown() {
        web3j().shutdown();
    }
}
```

## Comparison with Other Libraries

| Feature | Web3j | Ethers.js | Web3.py |
|---------|-------|-----------|---------|
| Language | Java | JavaScript | Python |
| Type Safety | ✅ Strong | ⚠️ Weak | ⚠️ Weak |
| Contract Wrappers | ✅ Yes | ✅ Yes | ❌ No |
| Performance | ✅ High | ✅ High | ⚠️ Medium |
| Async Support | ✅ Yes | ✅ Yes | ✅ Yes |
| Spring Integration | ✅ Native | ❌ No | ❌ No |

## References

- [Web3j Documentation](https://docs.web3j.io/)
- [Ethereum JSON-RPC API](https://ethereum.org/en/developers/docs/apis/json-rpc/)
- [Ganache](https://trufflesuite.com/ganache/)
- [Infura](https://infura.io/)
- [Etherscan](https://etherscan.io/)

## License

This project is licensed under the terms of the LICENSE file in the repository root.
