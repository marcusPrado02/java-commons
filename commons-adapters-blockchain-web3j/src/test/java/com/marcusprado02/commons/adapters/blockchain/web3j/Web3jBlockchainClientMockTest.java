package com.marcusprado02.commons.adapters.blockchain.web3j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.EthTransaction;
import org.web3j.protocol.core.methods.response.NetVersion;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

@SuppressWarnings({"unchecked", "rawtypes"})
class Web3jBlockchainClientMockTest {

  private Web3j web3j;
  private Web3jBlockchainClient client;

  @BeforeEach
  void setUp() {
    web3j = mock(Web3j.class);
    client = Web3jBlockchainClient.create(web3j);
  }

  // ─── getBlockNumber ──────────────────────────────────────────────────────────

  @Test
  void shouldReturnFailOnGetBlockNumberIOException() throws IOException {
    Request req = mock(Request.class);
    when(web3j.ethBlockNumber()).thenReturn(req);
    when(req.send()).thenThrow(new IOException("connection refused"));

    assertThat(client.getBlockNumber().isFail()).isTrue();
  }

  @Test
  void shouldReturnFailOnGetBlockNumberResponseError() throws IOException {
    Request req = mock(Request.class);
    EthBlockNumber res = mock(EthBlockNumber.class);
    Response.Error err = mock(Response.Error.class);
    when(web3j.ethBlockNumber()).thenReturn(req);
    when(req.send()).thenReturn(res);
    when(res.hasError()).thenReturn(true);
    when(res.getError()).thenReturn(err);
    when(err.getMessage()).thenReturn("rpc error");

    assertThat(client.getBlockNumber().isFail()).isTrue();
  }

  // ─── getBalance ──────────────────────────────────────────────────────────────

  @Test
  void shouldReturnFailOnGetBalanceIOException() throws IOException {
    Request req = mock(Request.class);
    when(web3j.ethGetBalance(any(), any())).thenReturn(req);
    when(req.send()).thenThrow(new IOException("connection refused"));

    assertThat(client.getBalance("0x123").isFail()).isTrue();
  }

  @Test
  void shouldReturnFailOnGetBalanceResponseError() throws IOException {
    Request req = mock(Request.class);
    EthGetBalance res = mock(EthGetBalance.class);
    Response.Error err = mock(Response.Error.class);
    when(web3j.ethGetBalance(any(), any())).thenReturn(req);
    when(req.send()).thenReturn(res);
    when(res.hasError()).thenReturn(true);
    when(res.getError()).thenReturn(err);
    when(err.getMessage()).thenReturn("rpc error");

    assertThat(client.getBalance("0x123").isFail()).isTrue();
  }

  // ─── getTransactionCount ─────────────────────────────────────────────────────

  @Test
  void shouldReturnFailOnGetTransactionCountIOException() throws IOException {
    Request req = mock(Request.class);
    when(web3j.ethGetTransactionCount(any(), any())).thenReturn(req);
    when(req.send()).thenThrow(new IOException("connection refused"));

    assertThat(client.getTransactionCount("0x123").isFail()).isTrue();
  }

  @Test
  void shouldReturnFailOnGetTransactionCountResponseError() throws IOException {
    Request req = mock(Request.class);
    EthGetTransactionCount res = mock(EthGetTransactionCount.class);
    Response.Error err = mock(Response.Error.class);
    when(web3j.ethGetTransactionCount(any(), any())).thenReturn(req);
    when(req.send()).thenReturn(res);
    when(res.hasError()).thenReturn(true);
    when(res.getError()).thenReturn(err);
    when(err.getMessage()).thenReturn("rpc error");

    assertThat(client.getTransactionCount("0x123").isFail()).isTrue();
  }

  // ─── getGasPrice ─────────────────────────────────────────────────────────────

  @Test
  void shouldReturnFailOnGetGasPriceIOException() throws IOException {
    Request req = mock(Request.class);
    when(web3j.ethGasPrice()).thenReturn(req);
    when(req.send()).thenThrow(new IOException("connection refused"));

    assertThat(client.getGasPrice().isFail()).isTrue();
  }

  @Test
  void shouldReturnFailOnGetGasPriceResponseError() throws IOException {
    Request req = mock(Request.class);
    EthGasPrice res = mock(EthGasPrice.class);
    Response.Error err = mock(Response.Error.class);
    when(web3j.ethGasPrice()).thenReturn(req);
    when(req.send()).thenReturn(res);
    when(res.hasError()).thenReturn(true);
    when(res.getError()).thenReturn(err);
    when(err.getMessage()).thenReturn("rpc error");

    assertThat(client.getGasPrice().isFail()).isTrue();
  }

  // ─── estimateGas ─────────────────────────────────────────────────────────────

  @Test
  void shouldReturnFailOnEstimateGasIOException() throws IOException {
    Request req = mock(Request.class);
    when(web3j.ethEstimateGas(any())).thenReturn(req);
    when(req.send()).thenThrow(new IOException("connection refused"));

    assertThat(client.estimateGas("0xfrom", "0xto", BigInteger.ONE, "0x").isFail()).isTrue();
  }

  @Test
  void shouldReturnFailOnEstimateGasResponseError() throws IOException {
    Request req = mock(Request.class);
    EthEstimateGas res = mock(EthEstimateGas.class);
    Response.Error err = mock(Response.Error.class);
    when(web3j.ethEstimateGas(any())).thenReturn(req);
    when(req.send()).thenReturn(res);
    when(res.hasError()).thenReturn(true);
    when(res.getError()).thenReturn(err);
    when(err.getMessage()).thenReturn("rpc error");

    assertThat(client.estimateGas("0xfrom", "0xto", BigInteger.ONE, "0x").isFail()).isTrue();
  }

  // ─── call ────────────────────────────────────────────────────────────────────

  @Test
  void shouldReturnFailOnCallIOException() throws IOException {
    Request req = mock(Request.class);
    when(web3j.ethCall(any(), any())).thenReturn(req);
    when(req.send()).thenThrow(new IOException("connection refused"));

    assertThat(client.call("0xcontract", "0xdata").isFail()).isTrue();
  }

  @Test
  void shouldReturnFailOnCallResponseError() throws IOException {
    Request req = mock(Request.class);
    EthCall res = mock(EthCall.class);
    Response.Error err = mock(Response.Error.class);
    when(web3j.ethCall(any(), any())).thenReturn(req);
    when(req.send()).thenReturn(res);
    when(res.hasError()).thenReturn(true);
    when(res.getError()).thenReturn(err);
    when(err.getMessage()).thenReturn("rpc error");

    assertThat(client.call("0xcontract", "0xdata").isFail()).isTrue();
  }

  @Test
  void shouldReturnOkOnCallSuccess() throws IOException {
    Request req = mock(Request.class);
    EthCall res = mock(EthCall.class);
    when(web3j.ethCall(any(), any())).thenReturn(req);
    when(req.send()).thenReturn(res);
    when(res.hasError()).thenReturn(false);
    when(res.getValue()).thenReturn("0xresult");

    var result = client.call("0xcontract", "0xdata");
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isEqualTo("0xresult");
  }

  // ─── sendTransaction ─────────────────────────────────────────────────────────

  @Test
  void shouldReturnFailOnSendTransactionIOException() throws IOException {
    Request req = mock(Request.class);
    when(web3j.ethSendRawTransaction(any())).thenReturn(req);
    when(req.send()).thenThrow(new IOException("connection refused"));

    assertThat(client.sendTransaction("0xsignedhex").isFail()).isTrue();
  }

  @Test
  void shouldReturnFailOnSendTransactionResponseError() throws IOException {
    Request req = mock(Request.class);
    EthSendTransaction res = mock(EthSendTransaction.class);
    Response.Error err = mock(Response.Error.class);
    when(web3j.ethSendRawTransaction(any())).thenReturn(req);
    when(req.send()).thenReturn(res);
    when(res.hasError()).thenReturn(true);
    when(res.getError()).thenReturn(err);
    when(err.getMessage()).thenReturn("rpc error");

    assertThat(client.sendTransaction("0xsignedhex").isFail()).isTrue();
  }

  @Test
  void shouldReturnOkOnSendTransactionSuccess() throws IOException {
    Request req = mock(Request.class);
    EthSendTransaction res = mock(EthSendTransaction.class);
    when(web3j.ethSendRawTransaction(any())).thenReturn(req);
    when(req.send()).thenReturn(res);
    when(res.hasError()).thenReturn(false);
    when(res.getTransactionHash()).thenReturn("0xhash123");

    var result = client.sendTransaction("0xsignedhex");
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isEqualTo("0xhash123");
  }

  // ─── getNetworkId ─────────────────────────────────────────────────────────────

  @Test
  void shouldReturnFailOnGetNetworkIdIOException() throws IOException {
    Request req = mock(Request.class);
    when(web3j.netVersion()).thenReturn(req);
    when(req.send()).thenThrow(new IOException("connection refused"));

    assertThat(client.getNetworkId().isFail()).isTrue();
  }

  @Test
  void shouldReturnFailOnGetNetworkIdResponseError() throws IOException {
    Request req = mock(Request.class);
    NetVersion res = mock(NetVersion.class);
    Response.Error err = mock(Response.Error.class);
    when(web3j.netVersion()).thenReturn(req);
    when(req.send()).thenReturn(res);
    when(res.hasError()).thenReturn(true);
    when(res.getError()).thenReturn(err);
    when(err.getMessage()).thenReturn("rpc error");

    assertThat(client.getNetworkId().isFail()).isTrue();
  }

  // ─── isConnected ─────────────────────────────────────────────────────────────

  @Test
  void shouldReturnFalseOnIsConnectedIOException() throws IOException {
    Request req = mock(Request.class);
    when(web3j.web3ClientVersion()).thenReturn(req);
    when(req.send()).thenThrow(new IOException("connection refused"));

    var result = client.isConnected();
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isFalse();
  }

  // ─── getTransaction ──────────────────────────────────────────────────────────

  @Test
  void shouldReturnFailOnGetTransactionIOException() throws IOException {
    Request req = mock(Request.class);
    when(web3j.ethGetTransactionByHash(any())).thenReturn(req);
    when(req.send()).thenThrow(new IOException("connection refused"));

    assertThat(client.getTransaction("0xhash").isFail()).isTrue();
  }

  @Test
  void shouldReturnFailOnGetTransactionResponseError() throws IOException {
    Request req = mock(Request.class);
    EthTransaction res = mock(EthTransaction.class);
    Response.Error err = mock(Response.Error.class);
    when(web3j.ethGetTransactionByHash(any())).thenReturn(req);
    when(req.send()).thenReturn(res);
    when(res.hasError()).thenReturn(true);
    when(res.getError()).thenReturn(err);
    when(err.getMessage()).thenReturn("rpc error");

    assertThat(client.getTransaction("0xhash").isFail()).isTrue();
  }

  @Test
  void shouldReturnFailOnGetTransactionNotFound() throws IOException {
    Request req = mock(Request.class);
    EthTransaction res = mock(EthTransaction.class);
    when(web3j.ethGetTransactionByHash(any())).thenReturn(req);
    when(req.send()).thenReturn(res);
    when(res.hasError()).thenReturn(false);
    when(res.getTransaction()).thenReturn(Optional.empty());

    assertThat(client.getTransaction("0xhash").isFail()).isTrue();
  }

  @Test
  void shouldReturnOkOnGetPendingTransaction() throws IOException {
    // A pending transaction has null blockNumber — returns PENDING status with no receipt lookup
    Request req = mock(Request.class);
    EthTransaction res = mock(EthTransaction.class);
    org.web3j.protocol.core.methods.response.Transaction tx =
        mock(org.web3j.protocol.core.methods.response.Transaction.class);
    when(web3j.ethGetTransactionByHash(any())).thenReturn(req);
    when(req.send()).thenReturn(res);
    when(res.hasError()).thenReturn(false);
    when(res.getTransaction()).thenReturn(Optional.of(tx));
    when(tx.getBlockNumber()).thenReturn(null);
    when(tx.getHash()).thenReturn("0xhash");
    when(tx.getFrom()).thenReturn("0xfrom");
    when(tx.getTo()).thenReturn("0xto");
    when(tx.getValue()).thenReturn(BigInteger.ZERO);
    when(tx.getGasPrice()).thenReturn(BigInteger.valueOf(20_000_000_000L));
    when(tx.getGas()).thenReturn(BigInteger.valueOf(21000));
    when(tx.getNonce()).thenReturn(BigInteger.ZERO);
    when(tx.getInput()).thenReturn("0x");
    when(tx.getBlockHash()).thenReturn(null);

    var result = client.getTransaction("0xhash");
    assertThat(result.isOk()).isTrue();
  }

  // ─── getTransactionReceipt ───────────────────────────────────────────────────

  @Test
  void shouldReturnFailOnGetTransactionReceiptIOException() throws IOException {
    Request req = mock(Request.class);
    when(web3j.ethGetTransactionReceipt(any())).thenReturn(req);
    when(req.send()).thenThrow(new IOException("connection refused"));

    assertThat(client.getTransactionReceipt("0xhash").isFail()).isTrue();
  }

  @Test
  void shouldReturnFailOnGetTransactionReceiptResponseError() throws IOException {
    Request req = mock(Request.class);
    EthGetTransactionReceipt res = mock(EthGetTransactionReceipt.class);
    Response.Error err = mock(Response.Error.class);
    when(web3j.ethGetTransactionReceipt(any())).thenReturn(req);
    when(req.send()).thenReturn(res);
    when(res.hasError()).thenReturn(true);
    when(res.getError()).thenReturn(err);
    when(err.getMessage()).thenReturn("rpc error");

    assertThat(client.getTransactionReceipt("0xhash").isFail()).isTrue();
  }

  @Test
  void shouldReturnFailOnGetTransactionReceiptEmpty() throws IOException {
    Request req = mock(Request.class);
    EthGetTransactionReceipt res = mock(EthGetTransactionReceipt.class);
    when(web3j.ethGetTransactionReceipt(any())).thenReturn(req);
    when(req.send()).thenReturn(res);
    when(res.hasError()).thenReturn(false);
    when(res.getTransactionReceipt()).thenReturn(Optional.empty());

    assertThat(client.getTransactionReceipt("0xhash").isFail()).isTrue();
  }

  @Test
  void shouldReturnOkOnGetTransactionReceiptSuccess() throws IOException {
    Request req = mock(Request.class);
    EthGetTransactionReceipt res = mock(EthGetTransactionReceipt.class);
    TransactionReceipt receipt = mock(TransactionReceipt.class);
    when(web3j.ethGetTransactionReceipt(any())).thenReturn(req);
    when(req.send()).thenReturn(res);
    when(res.hasError()).thenReturn(false);
    when(res.getTransactionReceipt()).thenReturn(Optional.of(receipt));
    when(receipt.getTransactionHash()).thenReturn("0xhash");
    when(receipt.getBlockNumber()).thenReturn(BigInteger.ONE);
    when(receipt.getBlockHash()).thenReturn("0xblockhash");
    when(receipt.getFrom()).thenReturn("0xfrom");
    when(receipt.getTo()).thenReturn("0xto");
    when(receipt.getGasUsed()).thenReturn(BigInteger.valueOf(21000));
    when(receipt.getCumulativeGasUsed()).thenReturn(BigInteger.valueOf(21000));
    when(receipt.getContractAddress()).thenReturn(null);
    when(receipt.isStatusOK()).thenReturn(true);
    when(receipt.getLogs()).thenReturn(Collections.emptyList());

    var result = client.getTransactionReceipt("0xhash");
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().status()).isTrue();
  }
}
