package com.dsa.documentsupportassistant.dto;

public record DocumentQuestionResponse(
        String answer,
        String evidence,
        boolean supported,
        Citation citation,
        AnswerStatus status
) {}