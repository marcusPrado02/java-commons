package com.marcusprado02.commons.adapters.sms.sns;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.marcusprado02.commons.ports.sms.PhoneNumber;
import com.marcusprado02.commons.ports.sms.SMS;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.SnsClientBuilder;
import software.amazon.awssdk.services.sns.model.InternalErrorException;
import software.amazon.awssdk.services.sns.model.InvalidParameterValueException;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.ThrottledException;

class SnsBranchTest {

  private SnsClientBuilder stubBuilder(SnsClient client) {
    SnsClientBuilder builder = mock(SnsClientBuilder.class);
    when(builder.region(any())).thenReturn(builder);
    when(builder.credentialsProvider(any())).thenReturn(builder);
    when(builder.overrideConfiguration(
            any(software.amazon.awssdk.core.client.config.ClientOverrideConfiguration.class)))
        .thenReturn(builder);
    when(builder.build()).thenReturn(client);
    return builder;
  }

  // --- mapSnsException: ThrottledException ---

  @Test
  void send_throttledException_returnsRateLimitExceeded() {
    SnsClient snsClient = mock(SnsClient.class);
    try (var staticMock = mockStatic(SnsClient.class)) {
      staticMock.when(SnsClient::builder).thenReturn(stubBuilder(snsClient));

      var config =
          SnsConfiguration.builder()
              .region(Region.US_EAST_1)
              .accessKeyId("KEY")
              .secretAccessKey("SECRET")
              .requestTimeout(Duration.ofSeconds(5))
              .maxPriceUsd(1.0)
              .build();
      var adapter = new SnsSmsAdapter(config);

      when(snsClient.publish(any(PublishRequest.class)))
          .thenThrow(ThrottledException.builder().message("throttled").build());

      var result =
          adapter.send(
              SMS.of(PhoneNumber.of("+10000000000"), PhoneNumber.of("+11111111111"), "hi"));
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("RATE_LIMIT_EXCEEDED");
    }
  }

  // --- mapSnsException: InternalErrorException ---

  @Test
  void send_internalErrorException_returnsInternalServerError() {
    SnsClient snsClient = mock(SnsClient.class);
    try (var staticMock = mockStatic(SnsClient.class)) {
      staticMock.when(SnsClient::builder).thenReturn(stubBuilder(snsClient));

      var config =
          SnsConfiguration.builder()
              .region(Region.US_EAST_1)
              .accessKeyId("KEY")
              .secretAccessKey("SECRET")
              .requestTimeout(Duration.ofSeconds(5))
              .maxPriceUsd(1.0)
              .build();
      var adapter = new SnsSmsAdapter(config);

      when(snsClient.publish(any(PublishRequest.class)))
          .thenThrow(InternalErrorException.builder().message("internal").build());

      var result =
          adapter.send(
              SMS.of(PhoneNumber.of("+10000000000"), PhoneNumber.of("+11111111111"), "hi"));
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("INTERNAL_SERVER_ERROR");
    }
  }

  // --- mapSnsException: InvalidParameterValueException ---

  @Test
  void send_invalidParameterValueException_returnsInvalidParamValue() {
    SnsClient snsClient = mock(SnsClient.class);
    try (var staticMock = mockStatic(SnsClient.class)) {
      staticMock.when(SnsClient::builder).thenReturn(stubBuilder(snsClient));

      var config =
          SnsConfiguration.builder()
              .region(Region.US_EAST_1)
              .accessKeyId("KEY")
              .secretAccessKey("SECRET")
              .requestTimeout(Duration.ofSeconds(5))
              .maxPriceUsd(1.0)
              .build();
      var adapter = new SnsSmsAdapter(config);

      when(snsClient.publish(any(PublishRequest.class)))
          .thenThrow(InvalidParameterValueException.builder().message("bad value").build());

      var result =
          adapter.send(
              SMS.of(PhoneNumber.of("+10000000000"), PhoneNumber.of("+11111111111"), "hi"));
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("INVALID_PARAMETER_VALUE");
    }
  }

  // --- createSnsClient: session token credential branch ---

  @Test
  void constructor_withSessionToken_usesSessionCredentials() {
    SnsClient snsClient = mock(SnsClient.class);
    try (var staticMock = mockStatic(SnsClient.class)) {
      staticMock.when(SnsClient::builder).thenReturn(stubBuilder(snsClient));

      var config =
          SnsConfiguration.builder()
              .region(Region.US_EAST_1)
              .sessionToken("SESSION_TOKEN")
              .requestTimeout(Duration.ofSeconds(5))
              .maxPriceUsd(1.0)
              .build();
      var adapter = new SnsSmsAdapter(config);
      assertThat(adapter).isNotNull();
    }
  }

  // --- createSnsClient: default credentials (IAM role) branch ---

  @Test
  void constructor_withIamRole_usesDefaultCredentials() {
    SnsClient snsClient = mock(SnsClient.class);
    try (var staticMock = mockStatic(SnsClient.class)) {
      staticMock.when(SnsClient::builder).thenReturn(stubBuilder(snsClient));

      var config = SnsConfiguration.withIamRole(Region.US_EAST_1);
      var adapter = new SnsSmsAdapter(config);
      assertThat(adapter).isNotNull();
    }
  }

  // --- buildPublishRequest: defaultSenderId and deliveryStatusLogging branches ---

  @Test
  void send_withSenderIdAndDeliveryLogging_includesAttributes() {
    SnsClient snsClient = mock(SnsClient.class);
    try (var staticMock = mockStatic(SnsClient.class)) {
      staticMock.when(SnsClient::builder).thenReturn(stubBuilder(snsClient));

      var config =
          SnsConfiguration.builder()
              .region(Region.US_EAST_1)
              .accessKeyId("KEY")
              .secretAccessKey("SECRET")
              .requestTimeout(Duration.ofSeconds(5))
              .maxPriceUsd(1.0)
              .defaultSenderId("MySender")
              .deliveryStatusLogging(true)
              .build();
      var adapter = new SnsSmsAdapter(config);

      when(snsClient.publish(any(PublishRequest.class)))
          .thenReturn(PublishResponse.builder().messageId("msg-1").build());

      var result =
          adapter.send(
              SMS.of(PhoneNumber.of("+10000000000"), PhoneNumber.of("+11111111111"), "hi"));
      assertThat(result.isOk()).isTrue();
    }
  }
}
