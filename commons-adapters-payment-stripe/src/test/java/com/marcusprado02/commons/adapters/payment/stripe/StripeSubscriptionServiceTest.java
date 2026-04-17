package com.marcusprado02.commons.adapters.payment.stripe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.stripe.exception.StripeException;
import com.stripe.param.SubscriptionCancelParams;
import com.stripe.param.SubscriptionCreateParams;
import com.stripe.param.SubscriptionListParams;
import com.stripe.param.SubscriptionUpdateParams;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StripeSubscriptionServiceTest {

  private StripeSubscriptionService service;

  @BeforeEach
  void setUp() {
    service = StripeSubscriptionService.create("sk_test_fake_key");
  }

  @Test
  void shouldCreateService() {
    assertThat(service).isNotNull();
  }

  @Test
  void shouldReturnFailureOnCreateSubscriptionException() {
    var ex = mock(StripeException.class);
    try (var mocked = mockStatic(com.stripe.model.Subscription.class)) {
      mocked
          .when(() -> com.stripe.model.Subscription.create(any(SubscriptionCreateParams.class)))
          .thenThrow(ex);
      var result = service.createSubscription("cus_test", "price_test", "pm_test", null);
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("SUBSCRIPTION.CREATE_FAILED");
    }
  }

  @Test
  void shouldReturnFailureOnCreateSubscriptionWithMetadataException() {
    var ex = mock(StripeException.class);
    try (var mocked = mockStatic(com.stripe.model.Subscription.class)) {
      mocked
          .when(() -> com.stripe.model.Subscription.create(any(SubscriptionCreateParams.class)))
          .thenThrow(ex);
      // non-null metadata covers the putAllMetadata branch
      var result =
          service.createSubscription("cus_test", "price_test", "pm_test", Map.of("key", "val"));
      assertThat(result.isFail()).isTrue();
    }
  }

  @Test
  void shouldReturnFailureOnGetSubscriptionException() {
    var ex = mock(StripeException.class);
    try (var mocked = mockStatic(com.stripe.model.Subscription.class)) {
      mocked.when(() -> com.stripe.model.Subscription.retrieve("sub_test")).thenThrow(ex);
      var result = service.getSubscription("sub_test");
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("SUBSCRIPTION.NOT_FOUND");
    }
  }

  @Test
  void shouldReturnFailureOnListSubscriptionsException() {
    var ex = mock(StripeException.class);
    try (var mocked = mockStatic(com.stripe.model.Subscription.class)) {
      mocked
          .when(() -> com.stripe.model.Subscription.list(any(SubscriptionListParams.class)))
          .thenThrow(ex);
      var result = service.listSubscriptions("cus_test");
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("SUBSCRIPTION.LIST_FAILED");
    }
  }

  @Test
  void shouldReturnOkOnListSubscriptionsSuccess() throws Exception {
    try (var mocked = mockStatic(com.stripe.model.Subscription.class)) {
      var collection = mock(com.stripe.model.SubscriptionCollection.class);
      when(collection.getData()).thenReturn(List.of());
      mocked
          .when(() -> com.stripe.model.Subscription.list(any(SubscriptionListParams.class)))
          .thenReturn(collection);

      var result = service.listSubscriptions("cus_test");
      assertThat(result.isOk()).isTrue();
      assertThat(result.getOrNull()).isEmpty();
    }
  }

  @Test
  void shouldReturnFailureOnUpdateSubscriptionException() throws Exception {
    var ex = mock(StripeException.class);
    try (var mocked = mockStatic(com.stripe.model.Subscription.class)) {
      var mockSub = mock(com.stripe.model.Subscription.class);
      mocked.when(() -> com.stripe.model.Subscription.retrieve("sub_test")).thenReturn(mockSub);
      when(mockSub.update(any(SubscriptionUpdateParams.class))).thenThrow(ex);
      var result = service.updateSubscription("sub_test", null, null, null);
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("SUBSCRIPTION.UPDATE_FAILED");
    }
  }

  @Test
  void shouldReturnFailureOnUpdateSubscriptionWithAllParamsException() throws Exception {
    // Covers true branches of priceId / paymentMethodId / metadata null-checks
    var ex = mock(StripeException.class);
    try (var mocked = mockStatic(com.stripe.model.Subscription.class)) {
      var mockItem = mock(com.stripe.model.SubscriptionItem.class);
      when(mockItem.getId()).thenReturn("si_test");
      var mockItems = mock(com.stripe.model.SubscriptionItemCollection.class);
      when(mockItems.getData()).thenReturn(List.of(mockItem));
      var mockSub = mock(com.stripe.model.Subscription.class);
      when(mockSub.getItems()).thenReturn(mockItems);
      mocked.when(() -> com.stripe.model.Subscription.retrieve("sub_test")).thenReturn(mockSub);
      when(mockSub.update(any(SubscriptionUpdateParams.class))).thenThrow(ex);
      var result =
          service.updateSubscription("sub_test", "price_test", "pm_test", Map.of("k", "v"));
      assertThat(result.isFail()).isTrue();
    }
  }

  @Test
  void shouldReturnFailureOnCancelSubscriptionException() throws Exception {
    var ex = mock(StripeException.class);
    try (var mocked = mockStatic(com.stripe.model.Subscription.class)) {
      var mockSub = mock(com.stripe.model.Subscription.class);
      mocked.when(() -> com.stripe.model.Subscription.retrieve("sub_test")).thenReturn(mockSub);
      when(mockSub.cancel(any(SubscriptionCancelParams.class))).thenThrow(ex);
      var result = service.cancelSubscription("sub_test", false);
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("SUBSCRIPTION.CANCEL_FAILED");
    }
  }

  @Test
  void shouldReturnFailureOnResumeSubscriptionException() throws Exception {
    var ex = mock(StripeException.class);
    try (var mocked = mockStatic(com.stripe.model.Subscription.class)) {
      var mockSub = mock(com.stripe.model.Subscription.class);
      mocked.when(() -> com.stripe.model.Subscription.retrieve("sub_test")).thenReturn(mockSub);
      when(mockSub.update(any(SubscriptionUpdateParams.class))).thenThrow(ex);
      var result = service.resumeSubscription("sub_test");
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("SUBSCRIPTION.RESUME_FAILED");
    }
  }
}
