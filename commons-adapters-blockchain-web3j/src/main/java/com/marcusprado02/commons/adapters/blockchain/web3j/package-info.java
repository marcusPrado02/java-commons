/**
 * Web3j adapters for Ethereum blockchain integration.
 *
 * <p>This package provides implementations of blockchain port interfaces using the Web3j library
 * for interacting with Ethereum networks.
 *
 * <h2>Quick Start</h2>
 *
 * <pre>{@code
 * // Create Web3j instance
 * Web3j web3j = Web3j.build(new HttpService("http://localhost:8545"));
 *
 * // Create blockchain client
 * BlockchainClient client = Web3jBlockchainClient.create(web3j);
 *
 * // Get balance
 * var balanceResult = client.getBalance("0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb");
 * balanceResult.ifOk(balance ->
 *     System.out.println("Balance: " + balance + " wei"));
 * }</pre>
 *
 * <h2>Wallet Management</h2>
 *
 * <pre>{@code
 * // Create wallet manager
 * WalletManager walletManager = Web3jWalletManager.create();
 *
 * // Create new wallet
 * var walletResult = walletManager.createWallet();
 * walletResult.ifOk(wallet -> {
 *     System.out.println("Address: " + wallet.address());
 *     System.out.println("Public Key: " + wallet.publicKey());
 * });
 *
 * // Load wallet from private key
 * var loadResult = walletManager.loadWallet("0x...");
 *
 * // Sign transaction
 * var signResult = walletManager.signTransaction(transaction, privateKey);
 *
 * // Export to keystore
 * var keystoreResult = walletManager.exportKeystore(privateKey, "password");
 * }</pre>
 *
 * <h2>Smart Contracts</h2>
 *
 * <pre>{@code
 * // Create smart contract client
 * SmartContractClient contractClient = Web3jSmartContractClient.create(web3j);
 *
 * // Call read-only function
 * var contract = SmartContract.builder()
 *     .address("0x...")
 *     .abi("[{...}]")
 *     .build();
 *
 * var result = contractClient.callFunction(
 *     contract,
 *     "balanceOf",
 *     List.of("0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb")
 * );
 *
 * // Listen to events
 * var eventsResult = contractClient.getEvents(
 *     contract,
 *     "Transfer",
 *     BigInteger.ZERO,
 *     BigInteger.valueOf(1000)
 * );
 * }</pre>
 *
 * <h2>Network Configuration</h2>
 *
 * Connect to different Ethereum networks:
 *
 * <pre>{@code
 * // Mainnet
 * Web3j mainnet = Web3j.build(new HttpService("https://mainnet.infura.io/v3/YOUR-KEY"));
 *
 * // Sepolia testnet
 * Web3j sepolia = Web3j.build(new HttpService("https://sepolia.infura.io/v3/YOUR-KEY"));
 *
 * // Local Ganache
 * Web3j ganache = Web3j.build(new HttpService("http://localhost:8545"));
 * }</pre>
 *
 * <h2>Error Handling</h2>
 *
 * All operations return {@code Result<T>} for consistent error handling:
 *
 * <pre>{@code
 * var result = client.getBalance(address);
 *
 * if (result.isOk()) {
 *     BigInteger balance = result.getOrNull();
 *     // handle success
 * } else {
 *     Problem problem = result.problemOrNull();
 *     logger.error("Error: {}", problem.message());
 * }
 * }</pre>
 *
 * @see com.marcusprado02.commons.adapters.blockchain.web3j.Web3jBlockchainClient
 * @see com.marcusprado02.commons.adapters.blockchain.web3j.Web3jWalletManager
 * @see com.marcusprado02.commons.adapters.blockchain.web3j.Web3jSmartContractClient
 */
package com.marcusprado02.commons.adapters.blockchain.web3j;
