package com.marcusprado02.commons.adapters.grpc.server.interceptors;

import io.grpc.ForwardingServerCall;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;

import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server interceptor for logging gRPC method calls.
 *
 * <p>Logs:
 * <ul>
 *   <li>Method invocation with start time</li>
 *   <li>Method completion with duration and status</li>
 *   <li>Error details on failure</li>
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ServerBuilder.forPort(9090)
 *     .addService(myService)
 *     .intercept(new LoggingInterceptor())
 *     .build();
 * }</pre>
 */
public class LoggingInterceptor implements ServerInterceptor {

  private static final Logger logger = Logger.getLogger(LoggingInterceptor.class.getName());

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call,
                                                                 Metadata headers,
                                                                 ServerCallHandler<ReqT, RespT> next) {
    String methodName = call.getMethodDescriptor().getFullMethodName();
    Instant startTime = Instant.now();

    logger.log(Level.INFO, "gRPC call started: {0}", methodName);

    ServerCall.Listener<ReqT> listener = next.startCall(
        new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
          @Override
          public void close(Status status, Metadata trailers) {
            long durationMs = java.time.Duration.between(startTime, Instant.now()).toMillis();

            if (status.isOk()) {
              logger.log(Level.INFO, "gRPC call completed: {0} - duration: {1}ms",
                  new Object[]{methodName, durationMs});
            } else {
              logger.log(Level.WARNING,
                  "gRPC call failed: {0} - status: {1} - duration: {2}ms - description: {3}",
                  new Object[]{methodName, status.getCode(), durationMs, status.getDescription()});
            }

            super.close(status, trailers);
          }
        }, headers);

    return listener;
  }
}
