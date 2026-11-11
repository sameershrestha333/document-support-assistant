package com.dsa.documentsupportassistant.dto;

import jakarta.validation.constraints.NotBlank;

public record RetrievalRequest(@NotBlank String documentId, @NotBlank String question) {
}
