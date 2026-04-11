package com.marcusprado02.commons.adapters.grpc.client.interceptors;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import java.time.Instant;
import java.util.logging.Logger;

/**
 * Client interceptor for logging gRPC calls.
 *
 * <p>Logs:
 *
 * <ul>
 *   <li>Method invocation with start time
 *   <li>Method completion with duration and status
 *   <li>Error details on failure
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9090)
 *     .intercept(new LoggingInterceptor())
 *     .build();
 * }</pre>
 */
public class LoggingInterceptor implements ClientInterceptor {

  private static final Logger logger = Logger.getLogger(LoggingInterceptor.class.getName());

  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
      MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {

    String methodName = method.getFullMethodName();
    Instant startTime = Instant.now();

    logger.info("gRPC client call started: " + methodName);

    return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
        next.newCall(method, callOptions)) {

      @Override
      public void start(Listener<RespT> responseListener, Metadata headers) {
        super.start(
            new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(
                responseListener) {

              @Override
              public void onClose(Status status, Metadata trailers) {
                long durationMs = java.time.Duration.between(startTime, Instant.now()).toMillis();

                if (status.isOk()) {
                  logger.info(
                      "gRPC client call completed: "
                          + methodName
                          + " - duration: "
                          + durationMs
                          + "ms");
                } else {
                  logger.warning(
                      "gRPC client call failed: "
                          + methodName
                          + " - status: "
                          + status.getCode()
                          + " - duration: "
                          + durationMs
                          + "ms - description: "
                          + status.getDescription());
                }

                super.onClose(status, trailers);
              }
            },
            headers);
      }
    };
  }
}
