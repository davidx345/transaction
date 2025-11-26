package com.fintech.recon.service.ingestion;

import com.opencsv.CSVReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.Arrays;
import java.util.Optional;

/**
 * Detects bank format from CSV file content by analyzing headers and data patterns.
 */
@Component
@Slf4j
public class BankFormatDetector {
    
    /**
     * Detect the bank format from an input stream.
     * Note: This will consume part of the stream to read the header.
     * Use with mark/reset or create a fresh stream after detection.
     * 
     * @param inputStream The CSV input stream
     * @return Detected BankFormat or empty if unknown
     */
    public Optional<BankFormat> detect(InputStream inputStream) {
        try {
            // Try to mark the stream if supported
            if (inputStream.markSupported()) {
                inputStream.mark(8192);
            }
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            
            // Read first few lines for analysis
            String firstLine = reader.readLine();
            String secondLine = reader.readLine();
            String thirdLine = reader.readLine();
            
            // Reset stream if possible
            if (inputStream.markSupported()) {
                inputStream.reset();
            }
            
            return detectFromLines(firstLine, secondLine, thirdLine);
            
        } catch (IOException e) {
            log.error("Error detecting bank format", e);
            return Optional.empty();
        }
    }
    
    /**
     * Detect bank format from the first few lines of CSV
     */
    public Optional<BankFormat> detectFromLines(String headerLine, String secondLine, String thirdLine) {
        if (headerLine == null) {
            return Optional.empty();
        }
        
        // Sometimes bank statements have metadata rows before the actual header
        // Check if first line looks like a header (contains common keywords)
        String actualHeader = findHeaderLine(headerLine, secondLine, thirdLine);
        
        if (actualHeader == null) {
            log.warn("Could not find valid header row in CSV");
            return Optional.empty();
        }
        
        // Split header into columns
        String[] headers = parseHeaderColumns(actualHeader);
        
        // First, try to detect from header
        Optional<BankFormat> fromHeader = BankFormat.detectFromHeader(headers);
        if (fromHeader.isPresent()) {
            log.info("Detected bank format from header: {}", fromHeader.get().getDisplayName());
            return fromHeader;
        }
        
        // If header detection fails, try reference pattern detection from data rows
        String dataLine = (actualHeader.equals(headerLine)) ? secondLine : thirdLine;
        if (dataLine != null) {
            String[] dataCols = parseDataColumns(dataLine);
            if (dataCols.length > 0) {
                // First column is often the reference
                Optional<BankFormat> fromRef = BankFormat.detectFromReference(dataCols[0]);
                if (fromRef.isPresent()) {
                    log.info("Detected bank format from reference pattern: {}", fromRef.get().getDisplayName());
                    return fromRef;
                }
            }
        }
        
        log.info("Could not detect specific bank format, falling back to generic");
        return Optional.of(BankFormat.GENERIC);
    }
    
    /**
     * Find the actual header line (skipping metadata rows)
     */
    private String findHeaderLine(String... lines) {
        String[] headerKeywords = {
            "REFERENCE", "REF", "AMOUNT", "DEBIT", "CREDIT", "DATE", 
            "PAYMENT", "TRANSACTION", "TRANS", "VALUE", "STATUS"
        };
        
        for (String line : lines) {
            if (line == null) continue;
            
            String upperLine = line.toUpperCase();
            int matchCount = 0;
            
            for (String keyword : headerKeywords) {
                if (upperLine.contains(keyword)) {
                    matchCount++;
                }
            }
            
            // If line contains at least 2 header keywords, it's likely the header
            if (matchCount >= 2) {
                return line;
            }
        }
        
        // Default to first line if no clear header found
        return lines.length > 0 ? lines[0] : null;
    }
    
    /**
     * Parse header columns (handles both comma and tab delimited)
     */
    private String[] parseHeaderColumns(String headerLine) {
        // Detect delimiter
        char delimiter = detectDelimiter(headerLine);
        
        String[] columns = headerLine.split(String.valueOf(delimiter));
        
        // Clean up column names
        return Arrays.stream(columns)
            .map(String::trim)
            .map(s -> s.replaceAll("^\"|\"$", "")) // Remove quotes
            .toArray(String[]::new);
    }
    
    /**
     * Parse data row columns
     */
    private String[] parseDataColumns(String dataLine) {
        char delimiter = detectDelimiter(dataLine);
        return dataLine.split(String.valueOf(delimiter));
    }
    
    /**
     * Detect CSV delimiter (comma or tab)
     */
    private char detectDelimiter(String line) {
        int commaCount = line.length() - line.replace(",", "").length();
        int tabCount = line.length() - line.replace("\t", "").length();
        int semiCount = line.length() - line.replace(";", "").length();
        
        if (tabCount > commaCount && tabCount > semiCount) return '\t';
        if (semiCount > commaCount) return ';';
        return ',';
    }
    
    /**
     * Detect bank format from file bytes (creates a copy for detection)
     */
    public BankFormat detectFromBytes(byte[] fileBytes) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(fileBytes)) {
            return detect(bais).orElse(BankFormat.GENERIC);
        } catch (Exception e) {
            log.error("Error detecting bank format from bytes", e);
            return BankFormat.GENERIC;
        }
    }
    
    /**
     * Get number of rows to skip before actual data (for banks with metadata headers)
     */
    public int detectSkipRows(byte[] fileBytes) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(fileBytes)))) {
            
            String[] headerKeywords = {"REFERENCE", "REF", "AMOUNT", "DATE", "TRANS"};
            int rowIndex = 0;
            String line;
            
            while ((line = reader.readLine()) != null && rowIndex < 10) {
                String upperLine = line.toUpperCase();
                int matches = 0;
                
                for (String keyword : headerKeywords) {
                    if (upperLine.contains(keyword)) matches++;
                }
                
                if (matches >= 2) {
                    log.info("Header found at row {}", rowIndex);
                    return rowIndex;
                }
                rowIndex++;
            }
            
            return 0; // Default: no skip
            
        } catch (Exception e) {
            log.error("Error detecting skip rows", e);
            return 0;
        }
    }
}
