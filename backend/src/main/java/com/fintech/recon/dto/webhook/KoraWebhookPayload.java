package com.fintech.recon.dto.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

/**
 * Kora (Korapay) webhook payload structure
 * Events: transfer.success, transfer.failed, charge.success, charge.failed, refund.success, refund.failed
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KoraWebhookPayload {
    
    private String event;
    private KoraData data;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KoraData {
        private BigDecimal amount;
        private BigDecimal fee;
        private String currency;
        private String status;
        private String reference;
        
        @JsonProperty("payment_reference")
        private String paymentReference;
        
        @JsonProperty("payout_reference")
        private String payoutReference;
        
        private String description;
        
        @JsonProperty("narration")
        private String narration;
        
        @JsonProperty("customer_reference")
        private String customerReference;
        
        private KoraCustomer customer;
        
        @JsonProperty("bank_account")
        private KoraBankAccount bankAccount;
        
        @JsonProperty("virtual_bank_account")
        private KoraVirtualBankAccount virtualBankAccount;
        
        private KoraCard card;
        
        @JsonProperty("created_at")
        private String createdAt;
        
        @JsonProperty("completed_at")
        private String completedAt;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KoraCustomer {
        private String name;
        private String email;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KoraBankAccount {
        @JsonProperty("bank_name")
        private String bankName;
        
        @JsonProperty("bank_code")
        private String bankCode;
        
        @JsonProperty("account_number")
        private String accountNumber;
        
        @JsonProperty("account_name")
        private String accountName;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KoraVirtualBankAccount {
        @JsonProperty("account_number")
        private String accountNumber;
        
        @JsonProperty("account_name")
        private String accountName;
        
        @JsonProperty("bank_name")
        private String bankName;
        
        @JsonProperty("bank_code")
        private String bankCode;
        
        @JsonProperty("account_reference")
        private String accountReference;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KoraCard {
        @JsonProperty("card_type")
        private String cardType;
        
        private String last4;
        
        @JsonProperty("expiry_month")
        private String expiryMonth;
        
        @JsonProperty("expiry_year")
        private String expiryYear;
    }
}
