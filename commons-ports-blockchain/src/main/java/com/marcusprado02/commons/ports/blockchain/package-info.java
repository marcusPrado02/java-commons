/**
 * Blockchain port interfaces for Ethereum and smart contract interactions.
 *
 * <p>This package provides a set of port interfaces following the hexagonal architecture pattern
 * for blockchain operations including:
 *
 * <ul>
 *   <li>Connecting to blockchain networks
 *   <li>Querying blocks, transactions, and balances
 *   <li>Deploying and interacting with smart contracts
 *   <li>Managing wallets and signing transactions
 *   <li>Listening to contract events
 * </ul>
 *
 * <h2>Quick Start</h2>
 *
 * <pre>{@code
 * // Get blockchain client from your DI container
 * BlockchainClient client = ...;
 *
 * // Query balance
 * var balanceResult = client.getBalance("0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb");
 * balanceResult.ifOk(balance ->
 *     System.out.println("Balance: " + balance + " wei"));
 *
 * // Get current block number
 * var blockResult = client.getBlockNumber();
 * blockResult.ifOk(blockNumber ->
 *     System.out.println("Current block: " + blockNumber));
 * }</pre>
 *
 * <h2>Working with Smart Contracts</h2>
 *
 * <pre>{@code
 * SmartContractClient contractClient = ...;
 * WalletManager walletManager = ...;
 *
 * // Deploy a contract
 * String bytecode = "0x...";
 * String abi = "[{...}]";
 * var deployResult = contractClient.deployContract(
 *     bytecode,
 *     abi,
 *     List.of("Constructor", "Params"),
 *     fromAddress,
 *     gasLimit,
 *     gasPrice
 * );
 *
 * deployResult.ifOk(contract -> {
 *     // Call a read-only function
 *     var result = contractClient.callFunction(
 *         contract,
 *         "getValue",
 *         List.of()
 *     );
 *
 *     // Send a transaction to modify state
 *     var txResult = contractClient.sendTransaction(
 *         contract,
 *         "setValue",
 *         List.of(42),
 *         fromAddress,
 *         gasLimit,
 *         gasPrice,
 *         BigInteger.ZERO
 *     );
 * });
 * }</pre>
 *
 * <h2>Wallet Management</h2>
 *
 * <pre>{@code
 * WalletManager walletManager = ...;
 *
 * // Create a new wallet
 * var walletResult = walletManager.createWallet();
 * walletResult.ifOk(wallet ->
 *     System.out.println("Address: " + wallet.address()));
 *
 * // Load wallet from private key
 * var loadResult = walletManager.loadWallet(privateKey);
 *
 * // Sign a transaction
 * var signResult = walletManager.signTransaction(transaction, privateKey);
 *
 * // Export to keystore
 * var keystoreResult = walletManager.exportKeystore(privateKey, password);
 * }</pre>
 *
 * <h2>Error Handling</h2>
 *
 * All operations return {@code Result<T>} which can be either successful or contain an error. Use
 * the Result API to handle errors gracefully:
 *
 * <pre>{@code
 * var result = client.getBalance(address);
 *
 * // Pattern matching style
 * if (result.isOk()) {
 *     BigInteger balance = result.getOrNull();
 *     // handle success
 * } else {
 *     Problem problem = result.problemOrNull();
 *     // handle error
 * }
 *
 * // Functional style
 * result
 *     .ifOk(balance -> System.out.println("Balance: " + balance))
 *     .ifFail(problem -> System.err.println("Error: " + problem.message()));
 * }</pre>
 *
 * @see com.marcusprado02.commons.ports.blockchain.BlockchainClient
 * @see com.marcusprado02.commons.ports.blockchain.SmartContractClient
 * @see com.marcusprado02.commons.ports.blockchain.WalletManager
 */
package com.marcusprado02.commons.ports.blockchain;
