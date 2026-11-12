package com.dsa.documentsupportassistant.dto;

public record ImportJob(
        String jobId,
        String owner,
        String filename,
        ImportJobStatus status,
        String errorDetails
) {}