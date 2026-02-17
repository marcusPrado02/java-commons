package com.marcusprado02.commons.ports.blockchain;

import com.marcusprado02.commons.kernel.result.Result;

/**
 * Port interface for wallet management.
 *
 * <p>Provides methods for creating, loading, and managing blockchain wallets.
 */
public interface WalletManager {

  /**
   * Creates a new wallet with a randomly generated private key.
   *
   * @return the result containing the new wallet
   */
  Result<Wallet> createWallet();

  /**
   * Loads a wallet from a private key.
   *
   * @param privateKey the private key (hex string)
   * @return the result containing the loaded wallet
   */
  Result<Wallet> loadWallet(String privateKey);

  /**
   * Signs a transaction.
   *
   * @param transaction the transaction to sign
   * @param privateKey the private key for signing
   * @return the result containing the signed transaction hex
   */
  Result<String> signTransaction(BlockchainTransaction transaction, String privateKey);

  /**
   * Signs arbitrary data.
   *
   * @param data the data to sign
   * @param privateKey the private key for signing
   * @return the result containing the signature
   */
  Result<String> signData(String data, String privateKey);

  /**
   * Verifies a signature.
   *
   * @param data the original data
   * @param signature the signature
   * @param address the address that should have signed
   * @return the result containing true if signature is valid
   */
  Result<Boolean> verifySignature(String data, String signature, String address);

  /**
   * Exports a wallet to an encrypted keystore format.
   *
   * @param privateKey the private key
   * @param password the encryption password
   * @return the result containing the keystore JSON
   */
  Result<String> exportKeystore(String privateKey, String password);

  /**
   * Imports a wallet from an encrypted keystore.
   *
   * @param keystoreJson the keystore JSON
   * @param password the decryption password
   * @return the result containing the wallet
   */
  Result<Wallet> importKeystore(String keystoreJson, String password);
}
