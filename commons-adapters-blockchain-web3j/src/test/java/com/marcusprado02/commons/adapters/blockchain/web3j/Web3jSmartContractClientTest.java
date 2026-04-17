package com.marcusprado02.commons.adapters.blockchain.web3j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marcusprado02.commons.ports.blockchain.SmartContract;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthLog;

@SuppressWarnings({"unchecked", "rawtypes"})
class Web3jSmartContractClientTest {

  private Web3j web3j;
  private Web3jSmartContractClient client;

  @BeforeEach
  void setUp() {
    web3j = mock(Web3j.class);
    client = Web3jSmartContractClient.create(web3j);
  }

  @Test
  void shouldThrowOnNullWeb3j() {
    assertThatThrownBy(() -> Web3jSmartContractClient.create(null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldAlwaysFailDeployContract() {
    var result =
        client.deployContract(
            "0x608060405234801561001057600080fd5b50",
            "[]",
            Collections.emptyList(),
            "0xabc",
            BigInteger.valueOf(21000),
            BigInteger.valueOf(20_000_000_000L));
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("BLOCKCHAIN.NOT_IMPLEMENTED");
  }

  @Test
  void shouldAlwaysFailSendTransaction() {
    var contract = SmartContract.builder().address("0x123").abi("[]").build();
    var result =
        client.sendTransaction(
            contract,
            "transfer",
            Collections.emptyList(),
            "0xabc",
            BigInteger.valueOf(21000),
            BigInteger.valueOf(20_000_000_000L),
            BigInteger.ZERO);
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("BLOCKCHAIN.NOT_IMPLEMENTED");
  }

  @Test
  void shouldEncodeEmptyFunctionCall() {
    var result = client.encodeFunctionCall("myFunction", Collections.emptyList());
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).startsWith("0x");
  }

  @Test
  void shouldEncodeFunctionCallWithMixedParamTypes() {
    // Covers the BigInteger, String, and Boolean branches in convertToWeb3jTypes
    var result =
        client.encodeFunctionCall(
            "transfer", List.of(BigInteger.valueOf(1000), "0xAddress", Boolean.TRUE));
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).startsWith("0x");
  }

  @Test
  void shouldDecodeEmptyFunctionOutput() {
    var result = client.decodeFunctionOutput("myFunction", "0x");
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isEmpty();
  }

  @Test
  void shouldReturnFailOnCallFunctionIOException() throws IOException {
    var contract = SmartContract.builder().address("0x123").abi("[]").build();
    Request request = mock(Request.class);
    when(web3j.ethCall(any(), any())).thenReturn(request);
    when(request.send()).thenThrow(new IOException("network error"));

    var result = client.callFunction(contract, "myFunction", Collections.emptyList());
    assertThat(result.isFail()).isTrue();
  }

  @Test
  void shouldReturnFailOnCallFunctionResponseError() throws IOException {
    var contract = SmartContract.builder().address("0x123").abi("[]").build();
    Request request = mock(Request.class);
    EthCall response = mock(EthCall.class);
    Response.Error error = mock(Response.Error.class);
    when(web3j.ethCall(any(), any())).thenReturn(request);
    when(request.send()).thenReturn(response);
    when(response.hasError()).thenReturn(true);
    when(response.getError()).thenReturn(error);
    when(error.getMessage()).thenReturn("contract error");

    var result = client.callFunction(contract, "myFunction", Collections.emptyList());
    assertThat(result.isFail()).isTrue();
  }

  @Test
  void shouldReturnOkOnCallFunctionSuccess() throws IOException {
    var contract = SmartContract.builder().address("0x123").abi("[]").build();
    Request request = mock(Request.class);
    EthCall response = mock(EthCall.class);
    when(web3j.ethCall(any(), any())).thenReturn(request);
    when(request.send()).thenReturn(response);
    when(response.hasError()).thenReturn(false);
    when(response.getValue()).thenReturn("0x");

    var result = client.callFunction(contract, "myFunction", Collections.emptyList());
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isEmpty();
  }

  @Test
  void shouldReturnFailOnGetEventsIOException() throws IOException {
    var contract = SmartContract.builder().address("0x123").abi("[]").build();
    Request request = mock(Request.class);
    when(web3j.ethGetLogs(any())).thenReturn(request);
    when(request.send()).thenThrow(new IOException("network error"));

    var result = client.getEvents(contract, "Transfer", BigInteger.ZERO, BigInteger.ONE);
    assertThat(result.isFail()).isTrue();
  }

  @Test
  void shouldReturnFailOnGetEventsResponseError() throws IOException {
    var contract = SmartContract.builder().address("0x123").abi("[]").build();
    Request request = mock(Request.class);
    EthLog response = mock(EthLog.class);
    Response.Error error = mock(Response.Error.class);
    when(web3j.ethGetLogs(any())).thenReturn(request);
    when(request.send()).thenReturn(response);
    when(response.hasError()).thenReturn(true);
    when(response.getError()).thenReturn(error);
    when(error.getMessage()).thenReturn("log error");

    var result = client.getEvents(contract, "Transfer", BigInteger.ZERO, BigInteger.ONE);
    assertThat(result.isFail()).isTrue();
  }

  @Test
  void shouldReturnOkOnGetEventsWithEmptyLogs() throws IOException {
    var contract = SmartContract.builder().address("0x123").abi("[]").build();
    Request request = mock(Request.class);
    EthLog response = mock(EthLog.class);
    when(web3j.ethGetLogs(any())).thenReturn(request);
    when(request.send()).thenReturn(response);
    when(response.hasError()).thenReturn(false);
    when(response.getLogs()).thenReturn(Collections.emptyList());

    var result = client.getEvents(contract, "Transfer", BigInteger.ZERO, BigInteger.ONE);
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isEmpty();
  }
}
