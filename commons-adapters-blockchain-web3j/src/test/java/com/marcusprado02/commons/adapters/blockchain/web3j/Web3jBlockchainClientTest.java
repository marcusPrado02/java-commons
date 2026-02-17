package com.marcusprado02.commons.adapters.blockchain.web3j;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

@Testcontainers
class Web3jBlockchainClientTest {

  @Container
  private static final GenericContainer<?> ganache =
      new GenericContainer<>(DockerImageName.parse("trufflesuite/ganache:v7.9.1"))
          .withExposedPorts(8545)
          .withCommand("--deterministic", "--accounts", "10", "--defaultBalanceEther", "100");

  private static Web3j web3j;
  private static Web3jBlockchainClient client;

  // Ganache deterministic address with funds
  private static final String TEST_ADDRESS = "0x90F8bf6A479f320ead074411a4B0e7944Ea8c9C1";

  @BeforeAll
  static void setUp() {
    var rpcUrl = "http://" + ganache.getHost() + ":" + ganache.getMappedPort(8545);
    web3j = Web3j.build(new HttpService(rpcUrl));
    client = Web3jBlockchainClient.create(web3j);
  }

  @AfterAll
  static void tearDown() {
    if (web3j != null) {
      web3j.shutdown();
    }
  }

  @Test
  void shouldConnectToGanache() {
    // When
    var result = client.isConnected();

    // Then
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isTrue();
  }

  @Test
  void shouldGetNetworkId() {
    // When
    var result = client.getNetworkId();

    // Then
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isNotNull();
  }

  @Test
  void shouldGetBlockNumber() {
    // When
    var result = client.getBlockNumber();

    // Then
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isNotNull();
    assertThat(result.getOrNull()).isGreaterThanOrEqualTo(BigInteger.ZERO);
  }

  @Test
  void shouldGetBalance() {
    // When
    var result = client.getBalance(TEST_ADDRESS);

    // Then
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isNotNull();
    // Ganache gives 100 ETH by default
    assertThat(result.getOrNull()).isGreaterThan(BigInteger.ZERO);
  }

  @Test
  void shouldGetTransactionCount() {
    // When
    var result = client.getTransactionCount(TEST_ADDRESS);

    // Then
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isNotNull();
    assertThat(result.getOrNull()).isGreaterThanOrEqualTo(BigInteger.ZERO);
  }

  @Test
  void shouldGetGasPrice() {
    // When
    var result = client.getGasPrice();

    // Then
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isNotNull();
    assertThat(result.getOrNull()).isGreaterThan(BigInteger.ZERO);
  }

  @Test
  void shouldEstimateGas() {
    // Given
    var from = TEST_ADDRESS;
    var to = "0xFFcf8FDEE72ac11b5c542428B35EEF5769C409f0";
    var value = BigInteger.valueOf(1000);
    var data = "0x";

    // When
    var result = client.estimateGas(from, to, value, data);

    // Then
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isNotNull();
    assertThat(result.getOrNull()).isGreaterThan(BigInteger.ZERO);
  }

  @Test
  void shouldFailForInvalidAddress() {
    // Given
    var invalidAddress = "invalid";

    // When
    var result = client.getBalance(invalidAddress);

    // Then
    assertThat(result.isFail()).isTrue();
  }

  @Test
  void shouldFailForNonexistentTransaction() {
    // Given
    var nonexistentTxHash = "0x0000000000000000000000000000000000000000000000000000000000000000";

    // When
    var result = client.getTransaction(nonexistentTxHash);

    // Then
    assertThat(result.isFail()).isTrue();
  }
}
