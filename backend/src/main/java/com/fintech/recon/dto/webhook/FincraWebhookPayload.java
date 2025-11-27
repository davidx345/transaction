package com.fintech.recon.dto.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

/**
 * Fincra webhook payload structure
 * Events: collection.successful, collection.failed, payout.successful, payout.failed, 
 *         conversion.completed, virtualaccount.created
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FincraWebhookPayload {
    
    private String event;
    private FincraData data;
    private String business;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FincraData {
        private Long id;
        private String business;
        
        @JsonProperty("virtualAccount")
        private String virtualAccount;
        
        @JsonProperty("sessionId")
        private String sessionId;
        
        @JsonProperty("senderBankName")
        private String senderBankName;
        
        @JsonProperty("senderAccountName")
        private String senderAccountName;
        
        @JsonProperty("senderAccountNumber")
        private String senderAccountNumber;
        
        @JsonProperty("sourceCurrency")
        private String sourceCurrency;
        
        @JsonProperty("destinationCurrency")
        private String destinationCurrency;
        
        @JsonProperty("sourceAmount")
        private BigDecimal sourceAmount;
        
        @JsonProperty("destinationAmount")
        private BigDecimal destinationAmount;
        
        @JsonProperty("amountReceived")
        private BigDecimal amountReceived;
        
        @JsonProperty("amountCharged")
        private BigDecimal amountCharged;
        
        private BigDecimal fee;
        private BigDecimal rate;
        
        @JsonProperty("customerName")
        private String customerName;
        
        @JsonProperty("settlementDestination")
        private String settlementDestination;
        
        private String status;
        private String reason;
        
        @JsonProperty("paymentScheme")
        private String paymentScheme;
        
        @JsonProperty("paymentDestination")
        private String paymentDestination;
        
        private String description;
        
        @JsonProperty("customerReference")
        private String customerReference;
        
        private String reference;
        
        @JsonProperty("traceId")
        private String traceId;
        
        @JsonProperty("initiatedAt")
        private String initiatedAt;
        
        @JsonProperty("createdAt")
        private String createdAt;
        
        @JsonProperty("updatedAt")
        private String updatedAt;
        
        @JsonProperty("valuedAt")
        private String valuedAt;
        
        private FincraRecipient recipient;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FincraRecipient {
        private String name;
        
        @JsonProperty("accountNumber")
        private String accountNumber;
        
        private String type; // individual, business
        private String email;
    }
}
