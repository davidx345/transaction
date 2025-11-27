package com.fintech.recon.dto.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Moniepoint (Monnify) webhook payload structure
 * Single event type for payment completion with PAID status
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoniepointWebhookPayload {
    
    @JsonProperty("transactionReference")
    private String transactionReference;
    
    @JsonProperty("paymentReference")
    private String paymentReference;
    
    @JsonProperty("amountPaid")
    private String amountPaid;
    
    @JsonProperty("totalPayable")
    private String totalPayable;
    
    @JsonProperty("settlementAmount")
    private String settlementAmount;
    
    @JsonProperty("paidOn")
    private String paidOn;
    
    @JsonProperty("paymentStatus")
    private String paymentStatus; // PAID
    
    @JsonProperty("paymentDescription")
    private String paymentDescription;
    
    @JsonProperty("transactionHash")
    private String transactionHash;
    
    private String currency;
    
    @JsonProperty("paymentMethod")
    private String paymentMethod; // ACCOUNT_TRANSFER, CARD
    
    private MoniepointProduct product;
    
    @JsonProperty("cardDetails")
    private MoniepointCardDetails cardDetails;
    
    @JsonProperty("accountDetails")
    private MoniepointAccountDetails accountDetails;
    
    @JsonProperty("accountPayments")
    private List<MoniepointAccountDetails> accountPayments;
    
    private MoniepointCustomer customer;
    
    @JsonProperty("metaData")
    private Map<String, Object> metaData;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MoniepointProduct {
        private String type; // RESERVED_ACCOUNT, WEB_SDK, Invoice
        private String reference;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MoniepointCardDetails {
        @JsonProperty("cardType")
        private String cardType; // VISA, MASTER, VERVE
        
        @JsonProperty("authorizationCode")
        private String authorizationCode;
        
        private String last4;
        
        @JsonProperty("expMonth")
        private String expMonth;
        
        @JsonProperty("expYear")
        private String expYear;
        
        private String bin;
        private Boolean reusable;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MoniepointAccountDetails {
        @JsonProperty("accountName")
        private String accountName;
        
        @JsonProperty("accountNumber")
        private String accountNumber;
        
        @JsonProperty("bankCode")
        private String bankCode;
        
        @JsonProperty("amountPaid")
        private String amountPaid;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MoniepointCustomer {
        private String email;
        private String name;
    }
}
