package com.dsa.documentsupportassistant.dto;

public record GroundedAnswer(
        String answer,
        String evidence,
        boolean supported,
        String passageId
) {}