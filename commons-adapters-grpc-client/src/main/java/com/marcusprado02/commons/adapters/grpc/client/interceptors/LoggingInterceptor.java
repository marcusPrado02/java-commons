package com.marcusprado02.commons.adapters.grpc.client.interceptors;

import io.grpc.*;

import java.time.Instant;
import java.util.logging.Level;
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

    logger.log(Level.INFO, "gRPC client call started: {0}", methodName);

    return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
        next.newCall(method, callOptions)) {

      @Override
      public void start(Listener<RespT> responseListener, Metadata headers) {
        super.start(
            new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(
                responseListener) {

              @Override
              public void onClose(Status status, Metadata trailers) {
                long durationMs =
                    java.time.Duration.between(startTime, Instant.now()).toMillis();

                if (status.isOk()) {
                  logger.log(
                      Level.INFO,
                      "gRPC client call completed: {0} - duration: {1}ms",
                      new Object[] {methodName, durationMs});
                } else {
                  logger.log(
                      Level.WARNING,
                      "gRPC client call failed: {0} - status: {1} - duration: {2}ms - description: {3}",
                      new Object[] {
                        methodName, status.getCode(), durationMs, status.getDescription()
                      });
                }

                super.onClose(status, trailers);
              }
            },
            headers);
      }
    };
  }
}
