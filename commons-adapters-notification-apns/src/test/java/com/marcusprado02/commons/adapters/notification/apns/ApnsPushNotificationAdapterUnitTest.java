package com.marcusprado02.commons.adapters.notification.apns;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.concurrent.PushNotificationFuture;
import com.marcusprado02.commons.ports.notification.NotificationPriority;
import com.marcusprado02.commons.ports.notification.NotificationTarget;
import com.marcusprado02.commons.ports.notification.PushNotification;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for ApnsPushNotificationAdapter covering exception and branch paths. */
@ExtendWith(MockitoExtension.class)
class ApnsPushNotificationAdapterUnitTest {

  @Mock private ApnsClient apnsClient;

  private ApnsPushNotificationAdapter adapter;

  private static final String TOPIC = "com.example.app";
  private static final PushNotification SIMPLE_NOTIFICATION =
      PushNotification.builder().title("Hello").body("World").build();

  @BeforeEach
  void setUp() {
    adapter = new ApnsPushNotificationAdapter(apnsClient, TOPIC);
  }

  @SuppressWarnings("unchecked")
  private static PushNotificationFuture<
          SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>>
      completedFuture(PushNotificationResponse<SimpleApnsPushNotification> response) {
    PushNotificationFuture<
            SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>>
        f = new PushNotificationFuture<>(new SimpleApnsPushNotification("t", TOPIC, "{}"));
    f.complete(response);
    return f;
  }

  @SuppressWarnings("unchecked")
  private static PushNotificationFuture<
          SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>>
      failingFuture(Exception cause) throws Exception {
    PushNotificationFuture<
            SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>>
        f = mock(PushNotificationFuture.class);
    when(f.get()).thenThrow(cause);
    return f;
  }

  // ── send() routing ────────────────────────────────────────────────────────

  @Test
  void sendToTopicShouldReturnUnsupported() {
    var result = adapter.send(NotificationTarget.topic("news"), SIMPLE_NOTIFICATION);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("APNS does not support topic");
  }

  @Test
  void sendWithUnknownTargetShouldReturnFail() {
    NotificationTarget mockTarget = mock(NotificationTarget.class);
    when(mockTarget.isDevice()).thenReturn(false);
    when(mockTarget.isTopic()).thenReturn(false);
    when(mockTarget.isMultiDevice()).thenReturn(false);

    var result = adapter.send(mockTarget, SIMPLE_NOTIFICATION);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Unknown target type");
  }

  @Test
  void sendShouldReturnFailOnOuterException() {
    when(apnsClient.sendNotification(any(SimpleApnsPushNotification.class)))
        .thenThrow(new RuntimeException("network error"));

    var result = adapter.send(NotificationTarget.device("token"), SIMPLE_NOTIFICATION);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Failed to send notification");
  }

  // ── sendToDevice ──────────────────────────────────────────────────────────

  @Test
  @SuppressWarnings("unchecked")
  void sendToDeviceShouldSucceedWithApnsId() {
    PushNotificationResponse<SimpleApnsPushNotification> response =
        mock(PushNotificationResponse.class);
    when(response.isAccepted()).thenReturn(true);
    when(response.getApnsId()).thenReturn(UUID.randomUUID());
    when(apnsClient.sendNotification(any(SimpleApnsPushNotification.class)))
        .thenReturn(completedFuture(response));

    var result = adapter.send(NotificationTarget.device("device-token"), SIMPLE_NOTIFICATION);

    assertThat(result.isOk()).isTrue();
  }

  @Test
  @SuppressWarnings("unchecked")
  void sendToDeviceShouldSucceedWithNullApnsId() {
    PushNotificationResponse<SimpleApnsPushNotification> response =
        mock(PushNotificationResponse.class);
    when(response.isAccepted()).thenReturn(true);
    when(response.getApnsId()).thenReturn(null);
    when(apnsClient.sendNotification(any(SimpleApnsPushNotification.class)))
        .thenReturn(completedFuture(response));

    var result = adapter.send(NotificationTarget.device("device-token"), SIMPLE_NOTIFICATION);

    assertThat(result.isOk()).isTrue();
  }

  @Test
  @SuppressWarnings("unchecked")
  void sendToDeviceShouldReturnFailWhenRejected() {
    PushNotificationResponse<SimpleApnsPushNotification> response =
        mock(PushNotificationResponse.class);
    when(response.isAccepted()).thenReturn(false);
    when(response.getRejectionReason()).thenReturn(Optional.of("BadDeviceToken"));
    when(apnsClient.sendNotification(any(SimpleApnsPushNotification.class)))
        .thenReturn(completedFuture(response));

    var result = adapter.send(NotificationTarget.device("bad-token"), SIMPLE_NOTIFICATION);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("APNS rejected notification");
  }

  @Test
  void sendToDeviceShouldReturnFailOnInterruptedException() throws Exception {
    var future = failingFuture(new InterruptedException("interrupted"));
    when(apnsClient.sendNotification(any(SimpleApnsPushNotification.class))).thenReturn(future);

    var result = adapter.send(NotificationTarget.device("token"), SIMPLE_NOTIFICATION);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("interrupted");
  }

  @Test
  void sendToDeviceShouldReturnFailOnExecutionException() throws Exception {
    var future =
        failingFuture(new ExecutionException("send failed", new RuntimeException("network")));
    when(apnsClient.sendNotification(any(SimpleApnsPushNotification.class))).thenReturn(future);

    var result = adapter.send(NotificationTarget.device("token"), SIMPLE_NOTIFICATION);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("network");
  }

  @Test
  void sendToDeviceShouldReturnFailOnExecutionExceptionWithNullCause() throws Exception {
    var future = failingFuture(new ExecutionException("send failed", null));
    when(apnsClient.sendNotification(any(SimpleApnsPushNotification.class))).thenReturn(future);

    var result = adapter.send(NotificationTarget.device("token"), SIMPLE_NOTIFICATION);

    assertThat(result.isFail()).isTrue();
  }

  // ── sendToMultipleDevices ─────────────────────────────────────────────────

  @Test
  @SuppressWarnings("unchecked")
  void sendToMultipleDevicesShouldReturnOkWithSuccessAndFailure() {
    PushNotificationResponse<SimpleApnsPushNotification> accepted =
        mock(PushNotificationResponse.class);
    when(accepted.isAccepted()).thenReturn(true);
    when(accepted.getApnsId()).thenReturn(UUID.randomUUID());

    PushNotificationResponse<SimpleApnsPushNotification> rejected =
        mock(PushNotificationResponse.class);
    when(rejected.isAccepted()).thenReturn(false);
    when(rejected.getRejectionReason()).thenReturn(Optional.of("BadToken"));

    when(apnsClient.sendNotification(any(SimpleApnsPushNotification.class)))
        .thenReturn(completedFuture(accepted))
        .thenReturn(completedFuture(rejected));

    var result =
        adapter.send(
            NotificationTarget.devices(Set.of("good-token", "bad-token")), SIMPLE_NOTIFICATION);

    assertThat(result.isOk()).isTrue();
  }

  @Test
  void sendToMultipleDevicesShouldHandleInterruptedExceptionInLoop() throws Exception {
    var future = failingFuture(new InterruptedException("interrupted"));
    when(apnsClient.sendNotification(any(SimpleApnsPushNotification.class))).thenReturn(future);

    var result = adapter.send(NotificationTarget.devices(Set.of("token1")), SIMPLE_NOTIFICATION);

    assertThat(result.isOk()).isTrue();
  }

  @Test
  void sendToMultipleDevicesShouldHandleExecutionExceptionInLoop() throws Exception {
    var future = failingFuture(new ExecutionException(new RuntimeException("error")));
    when(apnsClient.sendNotification(any(SimpleApnsPushNotification.class))).thenReturn(future);

    var result = adapter.send(NotificationTarget.devices(Set.of("token1")), SIMPLE_NOTIFICATION);

    assertThat(result.isOk()).isTrue();
  }

  // ── topic operations ──────────────────────────────────────────────────────

  @Test
  void subscribeToTopicWithSingleTokenShouldReturnUnsupported() {
    var result = adapter.subscribeToTopic("device-token", "news");

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("APNS does not support topic");
  }

  @Test
  void subscribeToTopicWithSetShouldReturnUnsupported() {
    var result = adapter.subscribeToTopic(Set.of("token1", "token2"), "news");

    assertThat(result.isFail()).isTrue();
  }

  @Test
  void unsubscribeFromTopicWithSingleTokenShouldReturnUnsupported() {
    var result = adapter.unsubscribeFromTopic("device-token", "news");

    assertThat(result.isFail()).isTrue();
  }

  @Test
  void unsubscribeFromTopicWithSetShouldReturnUnsupported() {
    var result = adapter.unsubscribeFromTopic(Set.of("token1"), "news");

    assertThat(result.isFail()).isTrue();
  }

  // ── validateToken ─────────────────────────────────────────────────────────

  @Test
  void validateTokenShouldAlwaysReturnTrue() {
    var result = adapter.validateToken("any-token");

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isTrue();
  }

  // ── close ─────────────────────────────────────────────────────────────────

  @Test
  void closeShouldSucceedNormally() {
    when(apnsClient.close()).thenReturn(CompletableFuture.completedFuture(null));

    adapter.close();
  }

  @Test
  @SuppressWarnings("unchecked")
  void closeShouldThrowRuntimeExceptionOnError() throws Exception {
    CompletableFuture<Void> failedFuture = mock(CompletableFuture.class);
    when(failedFuture.get()).thenThrow(new ExecutionException(new RuntimeException("close error")));
    when(apnsClient.close()).thenReturn(failedFuture);

    assertThatThrownBy(() -> adapter.close())
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Failed to close APNS client");
  }

  // ── payload branching ─────────────────────────────────────────────────────

  @Test
  @SuppressWarnings("unchecked")
  void sendShouldHandleNotificationWithCustomDataAndHighPriority() {
    PushNotificationResponse<SimpleApnsPushNotification> response =
        mock(PushNotificationResponse.class);
    when(response.isAccepted()).thenReturn(true);
    when(response.getApnsId()).thenReturn(null);
    when(apnsClient.sendNotification(any(SimpleApnsPushNotification.class)))
        .thenReturn(completedFuture(response));

    PushNotification notification =
        PushNotification.builder()
            .badge(3)
            .sound("ping.aiff")
            .priority(NotificationPriority.HIGH)
            .data(Map.of("key", "value"))
            .build();

    var result = adapter.send(NotificationTarget.device("token"), notification);

    assertThat(result.isOk()).isTrue();
  }

  @Test
  @SuppressWarnings("unchecked")
  void sendShouldHandleNotificationWithDefaultSound() {
    PushNotificationResponse<SimpleApnsPushNotification> response =
        mock(PushNotificationResponse.class);
    when(response.isAccepted()).thenReturn(true);
    when(response.getApnsId()).thenReturn(null);
    when(apnsClient.sendNotification(any(SimpleApnsPushNotification.class)))
        .thenReturn(completedFuture(response));

    PushNotification notification = PushNotification.builder().title("Test").build();

    var result = adapter.send(NotificationTarget.device("token"), notification);

    assertThat(result.isOk()).isTrue();
  }
}
