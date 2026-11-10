package com.dsa.documentsupportassistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(@NotBlank @Size(max = 100) String conversationId,
                          @NotBlank @Size(max = 2000) String message) {
}