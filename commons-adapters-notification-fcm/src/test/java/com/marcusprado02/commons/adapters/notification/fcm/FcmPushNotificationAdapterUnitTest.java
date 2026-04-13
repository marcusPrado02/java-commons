package com.marcusprado02.commons.adapters.notification.fcm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import com.google.firebase.messaging.TopicManagementResponse;
import com.marcusprado02.commons.ports.notification.NotificationTarget;
import com.marcusprado02.commons.ports.notification.PushNotification;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for FcmPushNotificationAdapter covering exception and branch paths. */
@ExtendWith(MockitoExtension.class)
class FcmPushNotificationAdapterUnitTest {

  @Mock private FirebaseMessaging messaging;

  private FcmPushNotificationAdapter adapter;
  private FcmPushNotificationAdapter adapterWithValidation;

  private static final PushNotification SIMPLE_NOTIFICATION =
      PushNotification.builder().title("Hello").body("World").build();

  @BeforeEach
  void setUp() {
    adapter = new FcmPushNotificationAdapter(messaging, false);
    adapterWithValidation = new FcmPushNotificationAdapter(messaging, true);
  }

  // ── send() routing ────────────────────────────────────────────────────────

  @Test
  void sendWithUnknownTargetShouldReturnFail() {
    NotificationTarget mockTarget = mock(NotificationTarget.class);
    when(mockTarget.isDevice()).thenReturn(false);
    when(mockTarget.isTopic()).thenReturn(false);
    when(mockTarget.isMultiDevice()).thenReturn(false);
    when(mockTarget.getType()).thenReturn(NotificationTarget.TargetType.DEVICE);

    var result = adapter.send(mockTarget, SIMPLE_NOTIFICATION);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Unknown target type");
  }

  @Test
  void sendShouldReturnFailOnOuterException() throws Exception {
    when(messaging.send(any(Message.class))).thenThrow(new RuntimeException("network error"));

    var result = adapter.send(NotificationTarget.device("token"), SIMPLE_NOTIFICATION);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Failed to send notification");
  }

  // ── sendToDevice ──────────────────────────────────────────────────────────

  @Test
  void sendToDeviceShouldSucceed() throws Exception {
    when(messaging.send(any(Message.class))).thenReturn("projects/test/messages/123");

    var result = adapter.send(NotificationTarget.device("token"), SIMPLE_NOTIFICATION);

    assertThat(result.isOk()).isTrue();
  }

  @Test
  void sendToDeviceShouldReturnFailOnFirebaseException() throws Exception {
    FirebaseMessagingException ex = mock(FirebaseMessagingException.class);
    when(ex.getMessage()).thenReturn("INVALID_ARGUMENT");
    when(ex.getMessagingErrorCode()).thenReturn(MessagingErrorCode.INVALID_ARGUMENT);
    when(messaging.send(any(Message.class))).thenThrow(ex);

    var result = adapter.send(NotificationTarget.device("token"), SIMPLE_NOTIFICATION);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("FCM send failed");
  }

  // ── sendToTopic ───────────────────────────────────────────────────────────

  @Test
  void sendToTopicShouldSucceed() throws Exception {
    when(messaging.send(any(Message.class))).thenReturn("projects/test/messages/456");

    var result = adapter.send(NotificationTarget.topic("news"), SIMPLE_NOTIFICATION);

    assertThat(result.isOk()).isTrue();
  }

  @Test
  void sendToTopicShouldReturnFailOnFirebaseException() throws Exception {
    FirebaseMessagingException ex = mock(FirebaseMessagingException.class);
    when(ex.getMessage()).thenReturn("QUOTA_EXCEEDED");
    when(messaging.send(any(Message.class))).thenThrow(ex);

    var result = adapter.send(NotificationTarget.topic("news"), SIMPLE_NOTIFICATION);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("FCM topic send failed");
  }

  // ── sendToMultipleDevices ─────────────────────────────────────────────────

  @Test
  void sendToMultipleDevicesShouldSucceedWithNoFailures() throws Exception {
    BatchResponse batchResponse = mock(BatchResponse.class);
    when(batchResponse.getSuccessCount()).thenReturn(2);
    when(batchResponse.getFailureCount()).thenReturn(0);
    when(messaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(batchResponse);

    var result =
        adapter.send(NotificationTarget.devices(Set.of("token1", "token2")), SIMPLE_NOTIFICATION);

    assertThat(result.isOk()).isTrue();
  }

  @Test
  void sendToMultipleDevicesShouldCollectFailures() throws Exception {
    SendResponse failedResponse = mock(SendResponse.class);
    when(failedResponse.isSuccessful()).thenReturn(false);
    FirebaseMessagingException failEx = mock(FirebaseMessagingException.class);
    when(failEx.getMessage()).thenReturn("bad token");
    when(failedResponse.getException()).thenReturn(failEx);

    BatchResponse batchResponse = mock(BatchResponse.class);
    when(batchResponse.getSuccessCount()).thenReturn(0);
    when(batchResponse.getFailureCount()).thenReturn(1);
    when(batchResponse.getResponses()).thenReturn(List.of(failedResponse));
    when(messaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(batchResponse);

    var result = adapter.send(NotificationTarget.devices(Set.of("bad-token")), SIMPLE_NOTIFICATION);

    assertThat(result.isOk()).isTrue();
  }

  @Test
  void sendToMultipleDevicesShouldReturnFailOnFirebaseException() throws Exception {
    FirebaseMessagingException ex = mock(FirebaseMessagingException.class);
    when(ex.getMessage()).thenReturn("multicast failed");
    when(messaging.sendEachForMulticast(any(MulticastMessage.class))).thenThrow(ex);

    var result =
        adapter.send(NotificationTarget.devices(Set.of("token1", "token2")), SIMPLE_NOTIFICATION);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("FCM multicast failed");
  }

  @Test
  void sendInBatchesShouldHandleMoreThan500Tokens() throws Exception {
    Set<String> tokens =
        IntStream.range(0, 501).mapToObj(i -> "token" + i).collect(Collectors.toSet());

    BatchResponse batchResponse = mock(BatchResponse.class);
    when(batchResponse.getSuccessCount()).thenReturn(500);
    when(batchResponse.getFailureCount()).thenReturn(0);
    when(batchResponse.getResponses()).thenReturn(List.of());
    when(messaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(batchResponse);

    var result = adapter.send(NotificationTarget.devices(tokens), SIMPLE_NOTIFICATION);

    assertThat(result.isOk()).isTrue();
  }

  @Test
  void sendInBatchesShouldHandleFirebaseExceptionInBatch() throws Exception {
    Set<String> tokens =
        IntStream.range(0, 501).mapToObj(i -> "token" + i).collect(Collectors.toSet());

    FirebaseMessagingException ex = mock(FirebaseMessagingException.class);
    when(ex.getMessage()).thenReturn("batch failed");
    when(messaging.sendEachForMulticast(any(MulticastMessage.class))).thenThrow(ex);

    var result = adapter.send(NotificationTarget.devices(tokens), SIMPLE_NOTIFICATION);

    assertThat(result.isOk()).isTrue();
  }

  // ── subscribeToTopic ──────────────────────────────────────────────────────

  @Test
  void subscribeToTopicShouldSucceedForSingleToken() throws Exception {
    TopicManagementResponse response = mock(TopicManagementResponse.class);
    when(response.getFailureCount()).thenReturn(0);
    when(messaging.subscribeToTopic(anyList(), anyString())).thenReturn(response);

    var result = adapter.subscribeToTopic("device-token", "news");

    assertThat(result.isOk()).isTrue();
  }

  @Test
  void subscribeToTopicShouldSucceedForSet() throws Exception {
    TopicManagementResponse response = mock(TopicManagementResponse.class);
    when(response.getFailureCount()).thenReturn(0);
    when(messaging.subscribeToTopic(anyList(), anyString())).thenReturn(response);

    var result = adapter.subscribeToTopic(Set.of("token1", "token2"), "news");

    assertThat(result.isOk()).isTrue();
  }

  @Test
  void subscribeToTopicShouldReturnFailOnPartialFailure() throws Exception {
    TopicManagementResponse response = mock(TopicManagementResponse.class);
    when(response.getFailureCount()).thenReturn(1);
    when(messaging.subscribeToTopic(anyList(), anyString())).thenReturn(response);

    var result = adapter.subscribeToTopic(Set.of("token1"), "news");

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("failed to subscribe");
  }

  @Test
  void subscribeToTopicShouldReturnFailOnException() throws Exception {
    FirebaseMessagingException ex = mock(FirebaseMessagingException.class);
    when(ex.getMessage()).thenReturn("subscribe failed");
    when(messaging.subscribeToTopic(anyList(), anyString())).thenThrow(ex);

    var result = adapter.subscribeToTopic(Set.of("token1"), "news");

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Topic subscription failed");
  }

  // ── unsubscribeFromTopic ──────────────────────────────────────────────────

  @Test
  void unsubscribeFromTopicShouldSucceedForSingleToken() throws Exception {
    TopicManagementResponse response = mock(TopicManagementResponse.class);
    when(response.getFailureCount()).thenReturn(0);
    when(messaging.unsubscribeFromTopic(anyList(), anyString())).thenReturn(response);

    var result = adapter.unsubscribeFromTopic("device-token", "news");

    assertThat(result.isOk()).isTrue();
  }

  @Test
  void unsubscribeFromTopicShouldReturnFailOnPartialFailure() throws Exception {
    TopicManagementResponse response = mock(TopicManagementResponse.class);
    when(response.getFailureCount()).thenReturn(2);
    when(messaging.unsubscribeFromTopic(anyList(), anyString())).thenReturn(response);

    var result = adapter.unsubscribeFromTopic(Set.of("token1", "token2"), "news");

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("failed to unsubscribe");
  }

  @Test
  void unsubscribeFromTopicShouldReturnFailOnException() throws Exception {
    FirebaseMessagingException ex = mock(FirebaseMessagingException.class);
    when(ex.getMessage()).thenReturn("unsubscribe failed");
    when(messaging.unsubscribeFromTopic(anyList(), anyString())).thenThrow(ex);

    var result = adapter.unsubscribeFromTopic(Set.of("token1"), "news");

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Topic unsubscription failed");
  }

  // ── validateToken ─────────────────────────────────────────────────────────

  @Test
  void validateTokenShouldReturnTrueWhenValidationDisabled() {
    var result = adapter.validateToken("any-token");

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isTrue();
  }

  @Test
  void validateTokenShouldReturnTrueOnSuccess() throws Exception {
    when(messaging.send(any(Message.class), anyBoolean())).thenReturn("msg-id");

    var result = adapterWithValidation.validateToken("valid-token");

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isTrue();
  }

  @Test
  void validateTokenShouldReturnFalseOnInvalidArgument() throws Exception {
    FirebaseMessagingException ex = mock(FirebaseMessagingException.class);
    when(ex.getMessagingErrorCode()).thenReturn(MessagingErrorCode.INVALID_ARGUMENT);
    when(messaging.send(any(Message.class), anyBoolean())).thenThrow(ex);

    var result = adapterWithValidation.validateToken("invalid-token");

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isFalse();
  }

  @Test
  void validateTokenShouldReturnFalseOnUnregistered() throws Exception {
    FirebaseMessagingException ex = mock(FirebaseMessagingException.class);
    when(ex.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNREGISTERED);
    when(messaging.send(any(Message.class), anyBoolean())).thenThrow(ex);

    var result = adapterWithValidation.validateToken("unregistered-token");

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isFalse();
  }

  @Test
  void validateTokenShouldReturnFailOnOtherException() throws Exception {
    FirebaseMessagingException ex = mock(FirebaseMessagingException.class);
    when(ex.getMessage()).thenReturn("server error");
    when(ex.getMessagingErrorCode()).thenReturn(MessagingErrorCode.INTERNAL);
    when(messaging.send(any(Message.class), anyBoolean())).thenThrow(ex);

    var result = adapterWithValidation.validateToken("token");

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Token validation failed");
  }

  // ── collectFailures with null exception ───────────────────────────────────

  @Test
  void sendToMultipleDevicesShouldHandleNullExceptionInFailedResponse() throws Exception {
    SendResponse failedResponse = mock(SendResponse.class);
    when(failedResponse.isSuccessful()).thenReturn(false);
    when(failedResponse.getException()).thenReturn(null);

    BatchResponse batchResponse = mock(BatchResponse.class);
    when(batchResponse.getSuccessCount()).thenReturn(0);
    when(batchResponse.getFailureCount()).thenReturn(1);
    when(batchResponse.getResponses()).thenReturn(List.of(failedResponse));
    when(messaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(batchResponse);

    var result = adapter.send(NotificationTarget.devices(Set.of("bad-token")), SIMPLE_NOTIFICATION);

    assertThat(result.isOk()).isTrue();
  }
}
