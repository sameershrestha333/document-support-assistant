package com.dsa.documentsupportassistant.service;

import com.dsa.documentsupportassistant.dto.ImportJob;
import com.dsa.documentsupportassistant.dto.ImportJobStatus;

import java.util.Map;
import java.util.Optional;

public class ImportJobService {
    private final Map<String, ImportJob> jobs = Map.of(
            "JOB-101", new ImportJob(
                    "JOB-101", "user", "products.csv",
                    ImportJobStatus.SUCCEEDED, null),
            "JOB-102", new ImportJob(
                    "JOB-102", "user", "inventory.csv",
                    ImportJobStatus.FAILED, "Missing required column: sku"),
            "JOB-103", new ImportJob(
                    "JOB-103", "user", "prices.csv",
                    ImportJobStatus.RUNNING, null)
    );

    public Optional<ImportJob> findById(String jobId) {
        return Optional.ofNullable(jobs.get(jobId));
    }
}
