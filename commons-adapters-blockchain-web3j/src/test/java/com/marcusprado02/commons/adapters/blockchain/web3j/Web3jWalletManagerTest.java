package com.marcusprado02.commons.adapters.blockchain.web3j;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcusprado02.commons.ports.blockchain.BlockchainTransaction;
import java.math.BigInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Web3jWalletManagerTest {

  private Web3jWalletManager walletManager;

  @BeforeEach
  void setUp() {
    walletManager = Web3jWalletManager.create();
  }

  @Test
  void shouldCreateWallet() {
    // When
    var result = walletManager.createWallet();

    // Then
    assertThat(result.isOk()).isTrue();
    var wallet = result.getOrNull();
    assertThat(wallet).isNotNull();
    assertThat(wallet.address()).isNotNull();
    assertThat(wallet.address()).startsWith("0x");
    assertThat(wallet.address()).hasSize(42); // 0x + 40 hex chars
    assertThat(wallet.publicKey()).isNotNull();
    assertThat(wallet.publicKey()).startsWith("0x");
  }

  @Test
  void shouldLoadWalletFromPrivateKey() {
    // Given
    var privateKey = "0x4c0883a69102937d6231471b5dbb6204fe512961708279f2e4d6f7e0c5e13d8f";

    // When
    var result = walletManager.loadWallet(privateKey);

    // Then
    assertThat(result.isOk()).isTrue();
    var wallet = result.getOrNull();
    assertThat(wallet).isNotNull();
    assertThat(wallet.address()).isNotNull();
    // Known address for this private key
    assertThat(wallet.address().toLowerCase())
        .isEqualTo("0x36c72a25b00a3d45043d82e4e8fb43172316a00d");
  }

  @Test
  void shouldFailWithInvalidPrivateKey() {
    // Given
    var invalidPrivateKey = "invalid";

    // When
    var result = walletManager.loadWallet(invalidPrivateKey);

    // Then
    assertThat(result.isFail()).isTrue();
  }

  @Test
  void shouldSignTransaction() {
    // Given
    var privateKey = "0x4c0883a69102937d6231471b5dbb6204fe512961708279f2e4d6f7e0c5e13d8f";
    var transaction =
        BlockchainTransaction.builder()
            .hash("0x123")
            .from("0x36c72a25b00a3d45043d82e4e8fb43172316a00d")
            .to("0xFFcf8FDEE72ac11b5c542428B35EEF5769C409f0")
            .value(BigInteger.valueOf(1000))
            .gasPrice(BigInteger.valueOf(20000000000L))
            .gasLimit(BigInteger.valueOf(21000))
            .nonce(BigInteger.ZERO)
            .data("0x")
            .build();

    // When
    var result = walletManager.signTransaction(transaction, privateKey);

    // Then
    assertThat(result.isOk()).isTrue();
    var signedTx = result.getOrNull();
    assertThat(signedTx).isNotNull();
    assertThat(signedTx).startsWith("0x");
  }

  @Test
  void shouldSignData() {
    // Given
    var privateKey = "0x4c0883a69102937d6231471b5dbb6204fe512961708279f2e4d6f7e0c5e13d8f";
    var data = "Hello, Ethereum!";

    // When
    var result = walletManager.signData(data, privateKey);

    // Then
    assertThat(result.isOk()).isTrue();
    var signature = result.getOrNull();
    assertThat(signature).isNotNull();
    assertThat(signature).startsWith("0x");
    assertThat(signature).hasSize(132); // 0x + 130 hex chars (65 bytes)
  }

  @Test
  void shouldVerifySignature() {
    // Given
    var privateKey = "0x4c0883a69102937d6231471b5dbb6204fe512961708279f2e4d6f7e0c5e13d8f";
    var address = "0x36c72a25b00a3d45043d82e4e8fb43172316a00d";
    var data = "Hello, Ethereum!";

    // When - Sign data
    var signResult = walletManager.signData(data, privateKey);
    assertThat(signResult.isOk()).isTrue();
    var signature = signResult.getOrNull();

    // When - Verify signature
    var verifyResult = walletManager.verifySignature(data, signature, address);

    // Then
    assertThat(verifyResult.isOk()).isTrue();
    assertThat(verifyResult.getOrNull()).isTrue();
  }

  @Test
  void shouldFailVerificationWithWrongAddress() {
    // Given
    var privateKey = "0x4c0883a69102937d6231471b5dbb6204fe512961708279f2e4d6f7e0c5e13d8f";
    var wrongAddress = "0x0000000000000000000000000000000000000000";
    var data = "Hello, Ethereum!";

    // When - Sign data
    var signResult = walletManager.signData(data, privateKey);
    assertThat(signResult.isOk()).isTrue();
    var signature = signResult.getOrNull();

    // When - Verify with wrong address
    var verifyResult = walletManager.verifySignature(data, signature, wrongAddress);

    // Then
    assertThat(verifyResult.isOk()).isTrue();
    assertThat(verifyResult.getOrNull()).isFalse();
  }

  @Test
  void shouldExportAndImportKeystore() {
    // Given
    var privateKey = "0x4c0883a69102937d6231471b5dbb6204fe512961708279f2e4d6f7e0c5e13d8f";
    var password = "test-password";

    // When - Export keystore
    var exportResult = walletManager.exportKeystore(privateKey, password);
    assertThat(exportResult.isOk()).isTrue();
    var keystoreJson = exportResult.getOrNull();
    assertThat(keystoreJson).isNotNull();
    assertThat(keystoreJson).contains("\"address\"");
    assertThat(keystoreJson).contains("\"crypto\"");

    // When - Import keystore
    var importResult = walletManager.importKeystore(keystoreJson, password);

    // Then
    assertThat(importResult.isOk()).isTrue();
    var wallet = importResult.getOrNull();
    assertThat(wallet).isNotNull();
    assertThat(wallet.address().toLowerCase())
        .isEqualTo("0x36c72a25b00a3d45043d82e4e8fb43172316a00d");
  }

  @Test
  void shouldFailImportWithWrongPassword() {
    // Given
    var privateKey = "0x4c0883a69102937d6231471b5dbb6204fe512961708279f2e4d6f7e0c5e13d8f";
    var password = "test-password";
    var wrongPassword = "wrong-password";

    // When - Export keystore
    var exportResult = walletManager.exportKeystore(privateKey, password);
    assertThat(exportResult.isOk()).isTrue();
    var keystoreJson = exportResult.getOrNull();

    // When - Import with wrong password
    var importResult = walletManager.importKeystore(keystoreJson, wrongPassword);

    // Then
    assertThat(importResult.isFail()).isTrue();
  }
}
