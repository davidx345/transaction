package com.fintech.recon.dto.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Quidax webhook payload structure for cryptocurrency transactions
 * Events: deposit.successful, deposit.failed, withdraw.successful, withdraw.rejected, 
 *         order.done, order.cancelled, swap.completed, swap.failed
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuidaxWebhookPayload {
    
    private String event;
    private QuidaxData data;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuidaxData {
        private String id;
        private String reference;
        private String type; // coin_address, internal
        private String currency;
        private String amount;
        private String fee;
        private String total;
        private String txid; // blockchain transaction ID
        
        @JsonProperty("transaction_note")
        private String transactionNote;
        
        private String narration;
        private String status; // accepted, Done, Rejected, submitted, confirmed
        private String reason;
        
        @JsonProperty("created_at")
        private String createdAt;
        
        @JsonProperty("done_at")
        private String doneAt;
        
        private QuidaxWallet wallet;
        private QuidaxUser user;
        
        @JsonProperty("payment_transaction")
        private QuidaxPaymentTransaction paymentTransaction;
        
        @JsonProperty("payment_address")
        private QuidaxPaymentAddress paymentAddress;
        
        private QuidaxRecipient recipient;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuidaxWallet {
        private String id;
        private String name;
        private String currency;
        private String balance;
        private String locked;
        private String staked;
        
        @JsonProperty("converted_balance")
        private String convertedBalance;
        
        @JsonProperty("reference_currency")
        private String referenceCurrency;
        
        @JsonProperty("is_crypto")
        private Boolean isCrypto;
        
        @JsonProperty("blockchain_enabled")
        private Boolean blockchainEnabled;
        
        @JsonProperty("default_network")
        private String defaultNetwork;
        
        @JsonProperty("deposit_address")
        private String depositAddress;
        
        @JsonProperty("destination_tag")
        private String destinationTag;
        
        @JsonProperty("created_at")
        private String createdAt;
        
        @JsonProperty("updated_at")
        private String updatedAt;
        
        private QuidaxUser user;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuidaxUser {
        private String id;
        private String sn;
        private String email;
        private String reference;
        
        @JsonProperty("first_name")
        private String firstName;
        
        @JsonProperty("last_name")
        private String lastName;
        
        @JsonProperty("display_name")
        private String displayName;
        
        @JsonProperty("created_at")
        private String createdAt;
        
        @JsonProperty("updated_at")
        private String updatedAt;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuidaxPaymentTransaction {
        private String status; // confirmed, unconfirmed
        private Integer confirmations;
        
        @JsonProperty("required_confirmations")
        private Integer requiredConfirmations;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuidaxPaymentAddress {
        private String id;
        private String reference;
        private String currency;
        private String address;
        private String network;
        
        @JsonProperty("destination_tag")
        private String destinationTag;
        
        @JsonProperty("total_payments")
        private String totalPayments;
        
        @JsonProperty("created_at")
        private String createdAt;
        
        @JsonProperty("updated_at")
        private String updatedAt;
        
        private QuidaxUser user;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuidaxRecipient {
        private String type; // internal, coin_address
        private QuidaxRecipientDetails details;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuidaxRecipientDetails {
        @JsonProperty("user_id")
        private String userId;
        
        private String address;
        private String network;
    }
}
