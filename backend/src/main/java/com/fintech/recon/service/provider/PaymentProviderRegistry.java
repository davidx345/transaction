package com.fintech.recon.service.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;

/**
 * Registry for payment provider webhook handlers
 * Provides routing and discovery of available providers
 */
@Component
@Slf4j
public class PaymentProviderRegistry {
    
    private final Map<String, WebhookHandler> handlers = new HashMap<>();
    private final List<WebhookHandler> allHandlers;
    
    public PaymentProviderRegistry(List<WebhookHandler> webhookHandlers) {
        this.allHandlers = webhookHandlers;
    }
    
    @PostConstruct
    public void init() {
        for (WebhookHandler handler : allHandlers) {
            String providerName = handler.getProviderName().toLowerCase();
            handlers.put(providerName, handler);
            log.info("Registered webhook handler for provider: {} ({})", 
                    providerName, handler.getDisplayName());
        }
        log.info("Total registered providers: {}", handlers.size());
    }
    
    /**
     * Get handler for a specific provider
     */
    public Optional<WebhookHandler> getHandler(String providerName) {
        if (providerName == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(handlers.get(providerName.toLowerCase()));
    }
    
    /**
     * Check if a provider is supported
     */
    public boolean isSupported(String providerName) {
        return providerName != null && handlers.containsKey(providerName.toLowerCase());
    }
    
    /**
     * Get all supported provider names
     */
    public Set<String> getSupportedProviders() {
        return Collections.unmodifiableSet(handlers.keySet());
    }
    
    /**
     * Get provider info for API responses
     */
    public List<ProviderInfo> getProviderInfoList() {
        List<ProviderInfo> infos = new ArrayList<>();
        for (WebhookHandler handler : handlers.values()) {
            infos.add(new ProviderInfo(
                    handler.getProviderName(),
                    handler.getDisplayName(),
                    handler.getSignatureHeaderName()
            ));
        }
        infos.sort(Comparator.comparing(ProviderInfo::displayName));
        return infos;
    }
    
    /**
     * Provider information record
     */
    public record ProviderInfo(String name, String displayName, String signatureHeader) {}
}
