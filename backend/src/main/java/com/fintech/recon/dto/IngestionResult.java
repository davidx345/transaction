package com.fintech.recon.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO representing the result of a CSV ingestion operation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionResult {
    
    private boolean success;
    private String fileName;
    private String bankName;
    private boolean autoDetected;
    private String message;
    
    @Builder.Default
    private int totalRecords = 0;
    
    @Builder.Default
    private int successfulRecords = 0;
    
    @Builder.Default
    private int failedRecords = 0;
    
    @Builder.Default
    private int skippedRecords = 0;
    
    @Builder.Default
    private List<String> warnings = new ArrayList<>();
    
    @Builder.Default
    private List<String> parseErrors = new ArrayList<>();
    
    @Builder.Default
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp = LocalDateTime.now();
    
    /**
     * Get success rate as percentage
     */
    public double getSuccessRate() {
        if (totalRecords == 0) return 0;
        return (double) successfulRecords / totalRecords * 100;
    }
    
    /**
     * Check if there were any parsing errors
     */
    public boolean hasErrors() {
        return failedRecords > 0 || !parseErrors.isEmpty();
    }
    
    /**
     * Check if there were any warnings
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
}
