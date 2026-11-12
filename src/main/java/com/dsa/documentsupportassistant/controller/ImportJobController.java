package com.dsa.documentsupportassistant.controller;

import com.dsa.documentsupportassistant.dto.ImportJob;
import com.dsa.documentsupportassistant.dto.ImportJobStatus;
import com.dsa.documentsupportassistant.service.ImportJobService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
public class ImportJobController {

    private final ImportJobService importJobService;

    public ImportJobController(ImportJobService importJobService) {
        this.importJobService = importJobService;
    }

    @GetMapping("/api/import-jobs/{jobId}")
    public ResponseEntity<ImportJob> findJob(
            @PathVariable String jobId, Principal principal) {
        return ResponseEntity.of(
                importJobService.findByIdForOwner(jobId, principal.getName()));
    }
}
