package com.fintech.recon.service.ingestion;

import com.fintech.recon.domain.Transaction;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class GtBankStrategy implements BankCsvStrategy {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public List<Transaction> parse(InputStream inputStream) {
        List<Transaction> transactions = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream))) {
            String[] header = reader.readNext(); // Skip header
            // Validate header if necessary

            String[] line;
            while ((line = reader.readNext()) != null) {
                // Expected format: PAYMENT_REF,AMOUNT,SETTLEMENT_DATE,STATUS
                // Example: GTB-PSK_abc123,5000.00,22/11/2024,SUCCESS
                
                if (line.length < 4) continue;

                String rawRef = line[0];
                String amountStr = line[1];
                String dateStr = line[2];
                String status = line[3];

                Transaction transaction = Transaction.builder()
                        .source("bank")
                        .externalReference(rawRef)
                        .normalizedReference(normalizeReference(rawRef))
                        .amount(new BigDecimal(amountStr))
                        .timestamp(LocalDate.parse(dateStr, DATE_FORMATTER).atStartOfDay())
                        .status(status)
                        .rawData(createRawData(line))
                        .build();

                transactions.add(transaction);
            }
        } catch (IOException | CsvValidationException e) {
            log.error("Error parsing GTBank CSV", e);
            throw new RuntimeException("Error parsing GTBank CSV", e);
        }
        return transactions;
    }

    @Override
    public String getBankName() {
        return "GTBank";
    }

    private String normalizeReference(String rawRef) {
        // Remove "GTB-" prefix
        return rawRef.replace("GTB-", "").trim();
    }

    private Map<String, Object> createRawData(String[] line) {
        Map<String, Object> data = new HashMap<>();
        data.put("PAYMENT_REF", line[0]);
        data.put("AMOUNT", line[1]);
        data.put("SETTLEMENT_DATE", line[2]);
        data.put("STATUS", line[3]);
        return data;
    }
}
