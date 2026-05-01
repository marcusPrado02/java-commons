package com.marcusprado02.commons.adapters.notification.fcm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.marcusprado02.commons.ports.notification.NotificationPriority;
import com.marcusprado02.commons.ports.notification.NotificationTarget;
import com.marcusprado02.commons.ports.notification.PushNotification;
import java.util.Map;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
class FcmBranchTest {

  // ── FcmConfiguration.Builder branches ────────────────────────────────────

  @Test
  void builder_credentials_setsField() {
    GoogleCredentials mockCreds = mock(GoogleCredentials.class);
    FcmConfiguration config =
        FcmConfiguration.builder()
            .credentials(mockCreds)
            .projectId("proj-1")
            .validateTokens(false)
            .build();

    assertThat(config.getCredentials()).isSameAs(mockCreds);
    assertThat(config.getProjectId()).isEqualTo("proj-1");
    assertThat(config.isValidateTokens()).isFalse();
  }

  @Test
  void builder_nullCredentials_throwsNpe() {
    assertThatThrownBy(
            () -> FcmConfiguration.builder().credentials((GoogleCredentials) null).build())
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void builder_buildWithNullCredentials_throwsNpe() {
    assertThatThrownBy(() -> FcmConfiguration.builder().build())
        .isInstanceOf(NullPointerException.class);
  }

  // ── FcmConfiguration.initializeApp() branches ────────────────────────────

  @Test
  void initializeApp_withProjectId_setsProjectId() {
    try (var staticMock = mockStatic(FirebaseApp.class)) {
      FirebaseApp mockApp = mock(FirebaseApp.class);
      staticMock
          .when(() -> FirebaseApp.initializeApp(any(FirebaseOptions.class), any(String.class)))
          .thenReturn(mockApp);
      staticMock
          .when(() -> FirebaseApp.initializeApp(any(FirebaseOptions.class)))
          .thenReturn(mockApp);

      GoogleCredentials mockCreds = mock(GoogleCredentials.class);
      FcmConfiguration config =
          FcmConfiguration.builder().credentials(mockCreds).projectId("my-project").build();

      FirebaseApp result = config.initializeApp();
      assertThat(result).isNotNull();
    }
  }

  @Test
  void initializeApp_withoutProjectId_skipsProjectIdBranch() {
    try (var staticMock = mockStatic(FirebaseApp.class)) {
      FirebaseApp mockApp = mock(FirebaseApp.class);
      staticMock
          .when(() -> FirebaseApp.initializeApp(any(FirebaseOptions.class), any(String.class)))
          .thenReturn(mockApp);
      staticMock
          .when(() -> FirebaseApp.initializeApp(any(FirebaseOptions.class)))
          .thenReturn(mockApp);

      GoogleCredentials mockCreds = mock(GoogleCredentials.class);
      FcmConfiguration config = FcmConfiguration.builder().credentials(mockCreds).build();

      FirebaseApp result = config.initializeApp();
      assertThat(result).isNotNull();
    }
  }

  // ── FcmPushNotificationAdapter payload building branches ─────────────────

  private FcmPushNotificationAdapter adapter() {
    FirebaseMessaging mockMessaging = mock(FirebaseMessaging.class);
    return new FcmPushNotificationAdapter(mockMessaging, false);
  }

  @Test
  void send_withImageUrl_coversImageBranch() throws Exception {
    FirebaseMessaging mockMessaging = mock(FirebaseMessaging.class);
    when(mockMessaging.send(any())).thenReturn("msg-1");

    FcmPushNotificationAdapter fcm = new FcmPushNotificationAdapter(mockMessaging, false);

    PushNotification notification =
        PushNotification.builder()
            .title("Title")
            .body("Body")
            .imageUrl("https://example.com/img.png")
            .build();

    var result = fcm.send(NotificationTarget.device("token"), notification);
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void send_withData_coversDataBranch() throws Exception {
    FirebaseMessaging mockMessaging = mock(FirebaseMessaging.class);
    when(mockMessaging.send(any())).thenReturn("msg-2");

    FcmPushNotificationAdapter fcm = new FcmPushNotificationAdapter(mockMessaging, false);

    PushNotification notification =
        PushNotification.builder().title("Title").body("Body").data(Map.of("key", "value")).build();

    var result = fcm.send(NotificationTarget.device("token"), notification);
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void send_withTimeToLive_coversTtlBranch() throws Exception {
    FirebaseMessaging mockMessaging = mock(FirebaseMessaging.class);
    when(mockMessaging.send(any())).thenReturn("msg-3");

    FcmPushNotificationAdapter fcm = new FcmPushNotificationAdapter(mockMessaging, false);

    PushNotification notification =
        PushNotification.builder().title("T").body("B").timeToLive(3600L).build();

    var result = fcm.send(NotificationTarget.device("token"), notification);
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void send_withSoundAndClickAction_coversSoundClickBranches() throws Exception {
    FirebaseMessaging mockMessaging = mock(FirebaseMessaging.class);
    when(mockMessaging.send(any())).thenReturn("msg-4");

    FcmPushNotificationAdapter fcm = new FcmPushNotificationAdapter(mockMessaging, false);

    PushNotification notification =
        PushNotification.builder()
            .title("T")
            .body("B")
            .sound("ding.aiff")
            .clickAction("OPEN_ACTIVITY_1")
            .build();

    var result = fcm.send(NotificationTarget.device("token"), notification);
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void send_withBadge_coversBadgeBranch() throws Exception {
    FirebaseMessaging mockMessaging = mock(FirebaseMessaging.class);
    when(mockMessaging.send(any())).thenReturn("msg-5");

    FcmPushNotificationAdapter fcm = new FcmPushNotificationAdapter(mockMessaging, false);

    PushNotification notification =
        PushNotification.builder().title("T").body("B").badge(3).build();

    var result = fcm.send(NotificationTarget.device("token"), notification);
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void send_dataOnlyWithNoVisualContent_coversContentAvailableBranch() throws Exception {
    FirebaseMessaging mockMessaging = mock(FirebaseMessaging.class);
    when(mockMessaging.send(any())).thenReturn("msg-6");

    FcmPushNotificationAdapter fcm = new FcmPushNotificationAdapter(mockMessaging, false);

    // No title/body → hasVisualContent=false; has data → hasData=true
    PushNotification notification =
        PushNotification.builder().data(Map.of("action", "refresh")).build();

    var result = fcm.send(NotificationTarget.device("token"), notification);
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void send_highPriority_coversHighPriorityBranch() throws Exception {
    FirebaseMessaging mockMessaging = mock(FirebaseMessaging.class);
    when(mockMessaging.send(any())).thenReturn("msg-7");

    FcmPushNotificationAdapter fcm = new FcmPushNotificationAdapter(mockMessaging, false);

    PushNotification notification =
        PushNotification.builder().title("T").body("B").priority(NotificationPriority.HIGH).build();

    var result = fcm.send(NotificationTarget.device("token"), notification);
    assertThat(result.isOk()).isTrue();
  }
}
