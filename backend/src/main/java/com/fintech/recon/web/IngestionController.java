package com.fintech.recon.web;

import com.fintech.recon.dto.IngestionResult;
import com.fintech.recon.service.TransactionIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/ingest")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class IngestionController {

    private final TransactionIngestionService ingestionService;

    /**
     * Upload CSV with specified bank format
     */
    @PostMapping("/csv")
    public ResponseEntity<IngestionResult> uploadCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "bank", required = false, defaultValue = "auto") String bankName) {
        
        IngestionResult result;
        if ("auto".equalsIgnoreCase(bankName) || bankName == null || bankName.isEmpty()) {
            result = ingestionService.ingestCsvWithAutoDetection(file);
        } else {
            result = ingestionService.ingestCsv(file, bankName);
        }
        
        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }
    
    /**
     * Upload CSV with auto-detection of bank format
     */
    @PostMapping("/csv/auto")
    public ResponseEntity<IngestionResult> uploadCsvAutoDetect(@RequestParam("file") MultipartFile file) {
        IngestionResult result = ingestionService.ingestCsvWithAutoDetection(file);
        
        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }
    
    /**
     * Get list of supported bank formats
     */
    @GetMapping("/banks")
    public ResponseEntity<List<String>> getSupportedBanks() {
        return ResponseEntity.ok(ingestionService.getSupportedBanks());
    }
}
