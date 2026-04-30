package com.marcusprado02.commons.adapters.blockchain.web3j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;

@SuppressWarnings({"unchecked", "rawtypes"})
class Web3jBranchTest {

  // --- verifySignature: signatureBytes.length != 65 branch ---

  @Test
  void verifySignature_shortSignature_returnsFalse() {
    Web3jWalletManager walletManager = Web3jWalletManager.create();
    var result = walletManager.verifySignature("data", "0xdeadbeef", "0xAddress");
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isFalse();
  }

  // --- verifySignature: 65-byte invalid signature triggers exception catch ---

  @Test
  void verifySignature_invalidSignatureBytes_returnsFalse() {
    Web3jWalletManager walletManager = Web3jWalletManager.create();
    // 65 bytes = 130 hex chars, all zeros → unrecoverable signature → returns false
    String invalid65 = "0x" + "00".repeat(65);
    var result = walletManager.verifySignature("data", invalid65, "0xSomeAddress");
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isFalse();
  }

  // --- BlockchainClient.create(null) branch ---

  @Test
  void blockchainClient_createWithNull_throws() {
    assertThatThrownBy(() -> Web3jBlockchainClient.create(null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  // --- SmartContractClient.create(null) branch ---

  @Test
  void smartContractClient_createWithNull_throws() {
    assertThatThrownBy(() -> Web3jSmartContractClient.create(null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  // --- isConnected: normal response (not exception) ---

  @Test
  void isConnected_success_returnsTrue() throws IOException {
    Web3j web3j = mock(Web3j.class);
    Request req = mock(Request.class);
    Web3ClientVersion res = mock(Web3ClientVersion.class);
    when(web3j.web3ClientVersion()).thenReturn(req);
    when(req.send()).thenReturn(res);
    when(res.hasError()).thenReturn(false);

    Web3jBlockchainClient client = Web3jBlockchainClient.create(web3j);
    var result = client.isConnected();
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isTrue();
  }

  // --- WalletManager.signData: exception catch branch ---

  @Test
  void signData_invalidKey_returnsFail() {
    Web3jWalletManager walletManager = Web3jWalletManager.create();
    var result = walletManager.signData("data", "not-a-valid-key");
    assertThat(result.isFail()).isTrue();
  }

  // --- WalletManager.signTransaction: exception catch branch ---

  @Test
  void signTransaction_invalidKey_returnsFail() {
    Web3jWalletManager walletManager = Web3jWalletManager.create();
    var txn =
        com.marcusprado02.commons.ports.blockchain.BlockchainTransaction.builder()
            .hash("0x")
            .from("0x")
            .to("0xTo")
            .value(BigInteger.ZERO)
            .gasPrice(BigInteger.ONE)
            .gasLimit(BigInteger.valueOf(21000))
            .nonce(BigInteger.ZERO)
            .data("0x")
            .build();
    var result = walletManager.signTransaction(txn, "invalid-key");
    assertThat(result.isFail()).isTrue();
  }
}
