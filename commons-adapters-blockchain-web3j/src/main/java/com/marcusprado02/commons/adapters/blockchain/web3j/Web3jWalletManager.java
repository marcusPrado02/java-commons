package com.marcusprado02.commons.adapters.blockchain.web3j;

import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.blockchain.BlockchainTransaction;
import com.marcusprado02.commons.ports.blockchain.Wallet;
import com.marcusprado02.commons.ports.blockchain.WalletManager;
import java.math.BigInteger;
import java.nio.file.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Keys;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.Sign;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.crypto.WalletUtils;
import org.web3j.utils.Numeric;

/**
 * Web3j implementation of WalletManager.
 *
 * <p>Provides wallet operations using Web3j crypto utilities.
 */
public final class Web3jWalletManager implements WalletManager {

  private static final Logger logger = LoggerFactory.getLogger(Web3jWalletManager.class);

  private Web3jWalletManager() {}

  /**
   * Creates a Web3jWalletManager.
   *
   * @return a new Web3jWalletManager
   */
  public static Web3jWalletManager create() {
    return new Web3jWalletManager();
  }

  @Override
  public Result<Wallet> createWallet() {
    try {
      var keyPair = Keys.createEcKeyPair();
      var credentials = Credentials.create(keyPair);
      return Result.ok(
          Wallet.builder()
              .address(credentials.getAddress())
              .publicKey(Numeric.toHexStringWithPrefix(keyPair.getPublicKey()))
              .balance(BigInteger.ZERO)
              .build());
    } catch (Exception e) {
      logger.error("Error creating wallet", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("BLOCKCHAIN.INTERNAL_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to create wallet: " + e.getMessage()));
    }
  }

  @Override
  public Result<Wallet> loadWallet(String privateKey) {
    try {
      var credentials = Credentials.create(privateKey);
      return Result.ok(
          Wallet.builder()
              .address(credentials.getAddress())
              .publicKey(Numeric.toHexStringWithPrefix(credentials.getEcKeyPair().getPublicKey()))
              .balance(BigInteger.ZERO)
              .build());
    } catch (Exception e) {
      logger.error("Error loading wallet from private key", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("BLOCKCHAIN.VALIDATION_ERROR"),
              ErrorCategory.BUSINESS,
              Severity.ERROR,
              "Invalid private key: " + e.getMessage()));
    }
  }

  @Override
  public Result<String> signTransaction(BlockchainTransaction transaction, String privateKey) {
    try {
      var credentials = Credentials.create(privateKey);

      // Create raw transaction
      var rawTransaction =
          RawTransaction.createTransaction(
              transaction.nonce(),
              transaction.gasPrice(),
              transaction.gasLimit(),
              transaction.to().orElse(""),
              transaction.value(),
              transaction.data());

      // Sign transaction
      var signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
      return Result.ok(Numeric.toHexString(signedMessage));
    } catch (Exception e) {
      logger.error("Error signing transaction", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("BLOCKCHAIN.INTERNAL_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to sign transaction: " + e.getMessage()));
    }
  }

  @Override
  public Result<String> signData(String data, String privateKey) {
    try {
      var credentials = Credentials.create(privateKey);
      var messageHash = org.web3j.crypto.Hash.sha3(data.getBytes());
      var signature = Sign.signMessage(messageHash, credentials.getEcKeyPair(), false);

      // Concatenate r, s, v into signature
      var r = Numeric.toHexStringNoPrefix(signature.getR());
      var s = Numeric.toHexStringNoPrefix(signature.getS());
      var v = Numeric.toHexStringNoPrefix(signature.getV());

      return Result.ok("0x" + r + s + v);
    } catch (Exception e) {
      logger.error("Error signing data", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("BLOCKCHAIN.INTERNAL_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to sign data: " + e.getMessage()));
    }
  }

  @Override
  public Result<Boolean> verifySignature(String data, String signature, String address) {
    try {
      // Parse signature
      var signatureBytes = Numeric.hexStringToByteArray(signature);
      if (signatureBytes.length != 65) {
        return Result.ok(false);
      }

      var r = new byte[32];
      var s = new byte[32];
      var v = signatureBytes[64];

      System.arraycopy(signatureBytes, 0, r, 0, 32);
      System.arraycopy(signatureBytes, 32, s, 0, 32);

      var signData = new Sign.SignatureData(v, r, s);

      // Hash the data
      var messageHash = org.web3j.crypto.Hash.sha3(data.getBytes());

      // Recover public key from signature
      var publicKeyBigInt = Sign.signedMessageHashToKey(messageHash, signData);
      var recoveredAddress = "0x" + Keys.getAddress(publicKeyBigInt);

      // Compare addresses (case-insensitive)
      return Result.ok(recoveredAddress.equalsIgnoreCase(address));
    } catch (Exception e) {
      logger.error("Error verifying signature", e);
      return Result.ok(false);
    }
  }

  @Override
  public Result<String> exportKeystore(String privateKey, String password) {
    try {
      var credentials = Credentials.create(privateKey);

      // Create temporary directory for keystore file
      var tempDir = Files.createTempDirectory("web3j-keystore");
      var keystoreFile =
          WalletUtils.generateWalletFile(
              password, credentials.getEcKeyPair(), tempDir.toFile(), false);

      // Read keystore file content
      var keystoreContent = Files.readString(tempDir.resolve(keystoreFile));

      // Clean up temporary files
      Files.delete(tempDir.resolve(keystoreFile));
      Files.delete(tempDir);

      return Result.ok(keystoreContent);
    } catch (Exception e) {
      logger.error("Error exporting keystore", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("BLOCKCHAIN.INTERNAL_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to export keystore: " + e.getMessage()));
    }
  }

  @Override
  public Result<Wallet> importKeystore(String keystoreJson, String password) {
    try {
      // Write keystore to temporary file
      var tempDir = Files.createTempDirectory("web3j-keystore");
      var keystoreFile = tempDir.resolve("keystore.json");
      Files.writeString(keystoreFile, keystoreJson);

      // Load credentials from keystore
      var credentials = WalletUtils.loadCredentials(password, keystoreFile.toFile());

      // Clean up temporary files
      Files.delete(keystoreFile);
      Files.delete(tempDir);

      return Result.ok(
          Wallet.builder()
              .address(credentials.getAddress())
              .publicKey(Numeric.toHexStringWithPrefix(credentials.getEcKeyPair().getPublicKey()))
              .balance(BigInteger.ZERO)
              .build());
    } catch (Exception e) {
      logger.error("Error importing keystore", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("BLOCKCHAIN.VALIDATION_ERROR"),
              ErrorCategory.BUSINESS,
              Severity.ERROR,
              "Failed to import keystore: " + e.getMessage()));
    }
  }
}
