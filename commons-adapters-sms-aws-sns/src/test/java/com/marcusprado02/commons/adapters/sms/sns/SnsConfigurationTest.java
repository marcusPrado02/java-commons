package com.marcusprado02.commons.adapters.sms.sns;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class SnsConfigurationTest {

  @Test
  void shouldCreateMinimalConfiguration() {
    SnsConfiguration config = SnsConfiguration.builder()
        .region(Region.US_EAST_1)
        .accessKeyId("AKIAIOSFODNN7EXAMPLE")
        .secretAccessKey("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")
        .build();

    assertEquals(Region.US_EAST_1, config.region());
    assertEquals("AKIAIOSFODNN7EXAMPLE", config.accessKeyId());
    assertEquals("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY", config.secretAccessKey());
    assertNull(config.sessionToken());
    assertEquals(Duration.ofSeconds(15), config.requestTimeout());
    assertNull(config.defaultSenderId());
    assertEquals(0.5, config.maxPriceUSD());
    assertEquals(SnsConfiguration.SmsType.TRANSACTIONAL, config.smsType());
    assertFalse(config.deliveryStatusLogging());
  }

  @Test
  void shouldCreateFullConfiguration() {
    SnsConfiguration config = SnsConfiguration.builder()
        .region(Region.EU_WEST_1)
        .accessKeyId("AKIAIOSFODNN7EXAMPLE")
        .secretAccessKey("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")
        .sessionToken("session123")
        .requestTimeout(Duration.ofSeconds(30))
        .defaultSenderId("MyApp")
        .maxPriceUSD(1.0)
        .smsType(SnsConfiguration.SmsType.PROMOTIONAL)
        .deliveryStatusLogging(true)
        .build();

    assertEquals(Region.EU_WEST_1, config.region());
    assertEquals("AKIAIOSFODNN7EXAMPLE", config.accessKeyId());
    assertEquals("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY", config.secretAccessKey());
    assertEquals("session123", config.sessionToken());
    assertEquals(Duration.ofSeconds(30), config.requestTimeout());
    assertEquals("MyApp", config.defaultSenderId());
    assertEquals(1.0, config.maxPriceUSD());
    assertEquals(SnsConfiguration.SmsType.PROMOTIONAL, config.smsType());
    assertTrue(config.deliveryStatusLogging());
  }

  @Test
  void shouldCreateDevelopmentConfiguration() {
    SnsConfiguration config = SnsConfiguration.forDevelopment(
        Region.US_WEST_2,
        "AKIAIOSFODNN7EXAMPLE",
        "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"
    );

    assertEquals(Region.US_WEST_2, config.region());
    assertEquals("AKIAIOSFODNN7EXAMPLE", config.accessKeyId());
    assertEquals("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY", config.secretAccessKey());
    assertEquals(Duration.ofSeconds(30), config.requestTimeout());
    assertEquals(0.1, config.maxPriceUSD());
    assertEquals(SnsConfiguration.SmsType.TRANSACTIONAL, config.smsType());
    assertTrue(config.deliveryStatusLogging());
  }

  @Test
  void shouldCreateProductionConfiguration() {
    SnsConfiguration config = SnsConfiguration.forProduction(
        Region.US_EAST_1,
        "AKIAIOSFODNN7EXAMPLE",
        "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
        "MyCompany"
    );

    assertEquals(Region.US_EAST_1, config.region());
    assertEquals("AKIAIOSFODNN7EXAMPLE", config.accessKeyId());
    assertEquals("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY", config.secretAccessKey());
    assertEquals("MyCompany", config.defaultSenderId());
    assertEquals(Duration.ofSeconds(15), config.requestTimeout());
    assertEquals(1.0, config.maxPriceUSD());
    assertEquals(SnsConfiguration.SmsType.TRANSACTIONAL, config.smsType());
    assertTrue(config.deliveryStatusLogging());
  }

  @Test
  void shouldCreateIamRoleConfiguration() {
    SnsConfiguration config = SnsConfiguration.withIamRole(Region.US_EAST_1);

    assertEquals(Region.US_EAST_1, config.region());
    assertNull(config.accessKeyId());
    assertNull(config.secretAccessKey());
    assertNull(config.sessionToken());
    assertEquals(Duration.ofSeconds(20), config.requestTimeout());
    assertEquals(0.5, config.maxPriceUSD());
    assertEquals(SnsConfiguration.SmsType.TRANSACTIONAL, config.smsType());
    assertTrue(config.deliveryStatusLogging());
  }

  @Test
  void shouldFailWhenRegionIsNull() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> SnsConfiguration.builder()
            .accessKeyId("AKIAIOSFODNN7EXAMPLE")
            .secretAccessKey("secret")
            .build()
    );
    assertEquals("Region cannot be null", exception.getMessage());
  }

  @Test
  void shouldFailWhenNoCredentialsProvided() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> SnsConfiguration.builder()
            .region(Region.US_EAST_1)
            .build()
    );
    assertEquals("Either access key ID or session token must be provided", exception.getMessage());
  }

  @Test
  void shouldFailWhenAccessKeyWithoutSecret() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> SnsConfiguration.builder()
            .region(Region.US_EAST_1)
            .accessKeyId("AKIAIOSFODNN7EXAMPLE")
            .build()
    );
    assertEquals("Secret access key is required when access key ID is provided", exception.getMessage());
  }

  @Test
  void shouldFailWhenAccessKeyWithBlankSecret() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> SnsConfiguration.builder()
            .region(Region.US_EAST_1)
            .accessKeyId("AKIAIOSFODNN7EXAMPLE")
            .secretAccessKey("   ")
            .build()
    );
    assertEquals("Secret access key is required when access key ID is provided", exception.getMessage());
  }

  @Test
  void shouldAllowBlankAccessKeyWithSessionToken() {
    assertDoesNotThrow(() ->
        SnsConfiguration.builder()
            .region(Region.US_EAST_1)
            .sessionToken("session123")
            .build()
    );
  }

  @Test
  void shouldFailWhenTimeoutIsZero() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> SnsConfiguration.builder()
            .region(Region.US_EAST_1)
            .accessKeyId("AKIAIOSFODNN7EXAMPLE")
            .secretAccessKey("secret")
            .requestTimeout(Duration.ZERO)
            .build()
    );
    assertEquals("Request timeout must be positive", exception.getMessage());
  }

  @Test
  void shouldFailWhenTimeoutIsNegative() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> SnsConfiguration.builder()
            .region(Region.US_EAST_1)
            .accessKeyId("AKIAIOSFODNN7EXAMPLE")
            .secretAccessKey("secret")
            .requestTimeout(Duration.ofSeconds(-1))
            .build()
    );
    assertEquals("Request timeout must be positive", exception.getMessage());
  }

  @Test
  void shouldFailWhenMaxPriceIsZero() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> SnsConfiguration.builder()
            .region(Region.US_EAST_1)
            .accessKeyId("AKIAIOSFODNN7EXAMPLE")
            .secretAccessKey("secret")
            .maxPriceUSD(0)
            .build()
    );
    assertEquals("Max price must be positive", exception.getMessage());
  }

  @Test
  void shouldFailWhenMaxPriceIsNegative() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> SnsConfiguration.builder()
            .region(Region.US_EAST_1)
            .accessKeyId("AKIAIOSFODNN7EXAMPLE")
            .secretAccessKey("secret")
            .maxPriceUSD(-0.1)
            .build()
    );
    assertEquals("Max price must be positive", exception.getMessage());
  }

  @Test
  void shouldFailWhenMaxPriceExceedsLimit() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> SnsConfiguration.builder()
            .region(Region.US_EAST_1)
            .accessKeyId("AKIAIOSFODNN7EXAMPLE")
            .secretAccessKey("secret")
            .maxPriceUSD(15.0)
            .build()
    );
    assertEquals("Max price cannot exceed $10.00 USD for safety", exception.getMessage());
  }

  @Test
  void shouldAllowMaxPriceAtLimit() {
    assertDoesNotThrow(() ->
        SnsConfiguration.builder()
            .region(Region.US_EAST_1)
            .accessKeyId("AKIAIOSFODNN7EXAMPLE")
            .secretAccessKey("secret")
            .maxPriceUSD(10.0)
            .build()
    );
  }

  @Test
  void shouldHaveCorrectSmsTypes() {
    assertEquals(2, SnsConfiguration.SmsType.values().length);
    assertNotNull(SnsConfiguration.SmsType.valueOf("PROMOTIONAL"));
    assertNotNull(SnsConfiguration.SmsType.valueOf("TRANSACTIONAL"));
  }
}
