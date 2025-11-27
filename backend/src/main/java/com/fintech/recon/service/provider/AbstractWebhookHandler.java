package com.fintech.recon.service.provider;

import com.fintech.recon.domain.Transaction;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Abstract base class with common functionality for webhook handlers
 */
@Slf4j
public abstract class AbstractWebhookHandler implements WebhookHandler {
    
    protected String computeHmacSha256(String payload, String secretKey) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            hmac.init(secretKeySpec);
            byte[] hash = hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            log.error("Error computing HMAC-SHA256", e);
            return "";
        }
    }
    
    protected String computeHmacSha512(String payload, String secretKey) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA512"
            );
            hmac.init(secretKeySpec);
            byte[] hash = hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            log.error("Error computing HMAC-SHA512", e);
            return "";
        }
    }
    
    protected String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    protected boolean secureCompare(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8)
        );
    }
    
    @SuppressWarnings("unchecked")
    protected Map<String, Object> getDataObject(Map<String, Object> payload) {
        Object data = payload.get("data");
        if (data instanceof Map) {
            return (Map<String, Object>) data;
        }
        return Map.of();
    }
    
    protected String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
    
    protected BigDecimal getBigDecimal(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return BigDecimal.ZERO;
        }
        try {
            if (value instanceof Number) {
                return new BigDecimal(value.toString());
            }
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            log.warn("Could not parse BigDecimal from {}: {}", key, value);
            return BigDecimal.ZERO;
        }
    }
    
    protected LocalDateTime parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return LocalDateTime.now();
        }
        try {
            // Try ISO format first
            return LocalDateTime.parse(dateStr.replace("Z", ""));
        } catch (Exception e) {
            try {
                // Try common date format
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm:ss a");
                return LocalDateTime.parse(dateStr, formatter);
            } catch (Exception e2) {
                log.warn("Could not parse date: {}", dateStr);
                return LocalDateTime.now();
            }
        }
    }
    
    protected Transaction.TransactionStatus mapStatus(String status, boolean isSuccess) {
        if (isSuccess) {
            return Transaction.TransactionStatus.SUCCESS;
        }
        if (status == null) {
            return Transaction.TransactionStatus.PENDING;
        }
        switch (status.toLowerCase()) {
            case "success":
            case "successful":
            case "paid":
            case "accepted":
            case "done":
            case "confirmed":
            case "completed":
                return Transaction.TransactionStatus.SUCCESS;
            case "failed":
            case "rejected":
            case "declined":
                return Transaction.TransactionStatus.FAILED;
            case "pending":
            case "processing":
            case "submitted":
                return Transaction.TransactionStatus.PENDING;
            default:
                return Transaction.TransactionStatus.PENDING;
        }
    }
}
