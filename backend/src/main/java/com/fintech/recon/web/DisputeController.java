package com.fintech.recon.web;

import com.fintech.recon.domain.Reconciliation;
import com.fintech.recon.service.DisputeService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/disputes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Allow any frontend (Vercel, localhost, etc.)
public class DisputeController {

    private final DisputeService disputeService;

    @GetMapping
    public ResponseEntity<List<Reconciliation>> listDisputes(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(disputeService.getDisputes(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Reconciliation> getDispute(@PathVariable UUID id) {
        return ResponseEntity.ok(disputeService.getDispute(id));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Reconciliation> approveDispute(@PathVariable UUID id, @RequestBody ActionRequest request) {
        return ResponseEntity.ok(disputeService.approveDispute(id, request.getReason()));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Reconciliation> rejectDispute(@PathVariable UUID id, @RequestBody ActionRequest request) {
        return ResponseEntity.ok(disputeService.rejectDispute(id, request.getReason()));
    }

    @Data
    public static class ActionRequest {
        private String reason;
    }
}
