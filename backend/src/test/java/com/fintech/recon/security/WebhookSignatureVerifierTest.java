package com.fintech.recon.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for WebhookSignatureVerifier
 */
class WebhookSignatureVerifierTest {

    private WebhookSignatureVerifier verifier;
    private WebhookSignatureVerifier.WebhookSecretConfig config;

    @BeforeEach
    void setUp() {
        config = new WebhookSignatureVerifier.WebhookSecretConfig();
        config.setPaystackSecretKey("sk_test_secret_key_12345");
        config.setFlutterwaveSecretHash("FLWSECK_TEST-secret_hash_67890");
        
        verifier = new WebhookSignatureVerifier(config);
    }

    // ========== Paystack Tests ==========

    @Test
    @DisplayName("Should verify valid Paystack signature")
    void shouldVerifyValidPaystackSignature() {
        String payload = "{\"event\":\"charge.success\",\"data\":{\"id\":123}}";
        String signature = computeHmacSha512(payload, config.getPaystackSecretKey());

        boolean isValid = verifier.verifyPaystackSignature(payload, signature);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should reject invalid Paystack signature")
    void shouldRejectInvalidPaystackSignature() {
        String payload = "{\"event\":\"charge.success\",\"data\":{\"id\":123}}";
        String invalidSignature = "invalid_signature_12345";

        boolean isValid = verifier.verifyPaystackSignature(payload, invalidSignature);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject null Paystack signature")
    void shouldRejectNullPaystackSignature() {
        String payload = "{\"event\":\"charge.success\"}";

        boolean isValid = verifier.verifyPaystackSignature(payload, null);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject empty Paystack signature")
    void shouldRejectEmptyPaystackSignature() {
        String payload = "{\"event\":\"charge.success\"}";

        boolean isValid = verifier.verifyPaystackSignature(payload, "");

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject Paystack signature for tampered payload")
    void shouldRejectPaystackSignatureForTamperedPayload() {
        String originalPayload = "{\"event\":\"charge.success\",\"data\":{\"id\":123}}";
        String tamperedPayload = "{\"event\":\"charge.success\",\"data\":{\"id\":999}}";
        String signature = computeHmacSha512(originalPayload, config.getPaystackSecretKey());

        boolean isValid = verifier.verifyPaystackSignature(tamperedPayload, signature);

        assertThat(isValid).isFalse();
    }

    // ========== Flutterwave Tests ==========

    @Test
    @DisplayName("Should verify valid Flutterwave secret hash")
    void shouldVerifyValidFlutterwaveSecretHash() {
        String secretHash = config.getFlutterwaveSecretHash();

        boolean isValid = verifier.verifyFlutterwaveSignature(secretHash);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should reject invalid Flutterwave secret hash")
    void shouldRejectInvalidFlutterwaveSecretHash() {
        String invalidHash = "INVALID_SECRET_HASH";

        boolean isValid = verifier.verifyFlutterwaveSignature(invalidHash);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject null Flutterwave signature")
    void shouldRejectNullFlutterwaveSignature() {
        boolean isValid = verifier.verifyFlutterwaveSignature(null);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject empty Flutterwave signature")
    void shouldRejectEmptyFlutterwaveSignature() {
        boolean isValid = verifier.verifyFlutterwaveSignature("");

        assertThat(isValid).isFalse();
    }

    // ========== Generic HMAC-SHA256 Tests ==========

    @Test
    @DisplayName("Should verify valid HMAC-SHA256 signature")
    void shouldVerifyValidHmacSha256Signature() {
        String payload = "{\"test\":\"data\"}";
        String secretKey = "my_secret_key";
        String signature = computeHmacSha256(payload, secretKey);

        boolean isValid = verifier.verifyHmacSha256(payload, signature, secretKey);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should reject invalid HMAC-SHA256 signature")
    void shouldRejectInvalidHmacSha256Signature() {
        String payload = "{\"test\":\"data\"}";
        String secretKey = "my_secret_key";
        String invalidSignature = "invalid_signature";

        boolean isValid = verifier.verifyHmacSha256(payload, invalidSignature, secretKey);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject HMAC-SHA256 with wrong secret key")
    void shouldRejectHmacSha256WithWrongSecretKey() {
        String payload = "{\"test\":\"data\"}";
        String correctKey = "correct_key";
        String wrongKey = "wrong_key";
        String signature = computeHmacSha256(payload, correctKey);

        boolean isValid = verifier.verifyHmacSha256(payload, signature, wrongKey);

        assertThat(isValid).isFalse();
    }

    // ========== Helper Methods ==========

    private String computeHmacSha512(String payload, String secretKey) {
        try {
            javax.crypto.Mac hmac = javax.crypto.Mac.getInstance("HmacSHA512");
            javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(
                    secretKey.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    "HmacSHA512"
            );
            hmac.init(keySpec);
            byte[] hash = hmac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String computeHmacSha256(String payload, String secretKey) {
        try {
            javax.crypto.Mac hmac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(
                    secretKey.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            hmac.init(keySpec);
            byte[] hash = hmac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
