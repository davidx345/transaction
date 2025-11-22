package com.fintech.recon.web;

import com.fintech.recon.service.TransactionIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ingest")
@RequiredArgsConstructor
public class IngestionController {

    private final TransactionIngestionService ingestionService;

    @PostMapping("/csv")
    public ResponseEntity<String> uploadCsv(@RequestParam("file") MultipartFile file,
                                            @RequestParam("bank") String bankName) {
        ingestionService.ingestCsv(file, bankName);
        return ResponseEntity.ok("CSV ingested successfully");
    }
}
