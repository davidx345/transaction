package com.fintech.recon.security;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

/**
 * Webhook signature verification for payment provider callbacks
 */
@Component
@Slf4j
public class WebhookSignatureVerifier {

    private final WebhookSecretConfig config;

    public WebhookSignatureVerifier(WebhookSecretConfig config) {
        this.config = config;
    }

    /**
     * Verify Paystack webhook signature
     * Paystack uses HMAC-SHA512
     */
    public boolean verifyPaystackSignature(String payload, String signature) {
        if (signature == null || signature.isEmpty()) {
            log.warn("Missing Paystack webhook signature");
            return false;
        }

        try {
            String expectedSignature = computeHmacSha512(payload, config.getPaystackSecretKey());
            boolean valid = MessageDigest.isEqual(
                    signature.getBytes(StandardCharsets.UTF_8),
                    expectedSignature.getBytes(StandardCharsets.UTF_8)
            );
            
            if (!valid) {
                log.warn("Invalid Paystack webhook signature");
            }
            
            return valid;
        } catch (Exception e) {
            log.error("Error verifying Paystack signature", e);
            return false;
        }
    }

    /**
     * Verify Flutterwave webhook signature
     * Flutterwave uses a secret hash header
     */
    public boolean verifyFlutterwaveSignature(String secretHash) {
        if (secretHash == null || secretHash.isEmpty()) {
            log.warn("Missing Flutterwave webhook signature");
            return false;
        }

        boolean valid = MessageDigest.isEqual(
                secretHash.getBytes(StandardCharsets.UTF_8),
                config.getFlutterwaveSecretHash().getBytes(StandardCharsets.UTF_8)
        );
        
        if (!valid) {
            log.warn("Invalid Flutterwave webhook signature");
        }
        
        return valid;
    }

    /**
     * Generic HMAC-SHA256 verification
     */
    public boolean verifyHmacSha256(String payload, String signature, String secretKey) {
        try {
            String expectedSignature = computeHmacSha256(payload, secretKey);
            return MessageDigest.isEqual(
                    signature.getBytes(StandardCharsets.UTF_8),
                    expectedSignature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.error("Error verifying HMAC-SHA256 signature", e);
            return false;
        }
    }

    /**
     * Compute HMAC-SHA512
     */
    private String computeHmacSha512(String payload, String secretKey) 
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac hmac = Mac.getInstance("HmacSHA512");
        SecretKeySpec secretKeySpec = new SecretKeySpec(
                secretKey.getBytes(StandardCharsets.UTF_8), 
                "HmacSHA512"
        );
        hmac.init(secretKeySpec);
        byte[] hash = hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    /**
     * Compute HMAC-SHA256
     */
    private String computeHmacSha256(String payload, String secretKey) 
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(
                secretKey.getBytes(StandardCharsets.UTF_8), 
                "HmacSHA256"
        );
        hmac.init(secretKeySpec);
        byte[] hash = hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    /**
     * Convert bytes to hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    @Component
    @ConfigurationProperties(prefix = "webhook")
    @Data
    public static class WebhookSecretConfig {
        /**
         * Paystack secret key for webhook verification
         */
        private String paystackSecretKey = "sk_test_xxxxxxx";

        /**
         * Flutterwave secret hash for webhook verification
         */
        private String flutterwaveSecretHash = "FLWSECK_TEST-xxxxxxx";

        /**
         * Custom webhook secrets for other providers
         */
        private Map<String, String> customSecrets;
    }
}
