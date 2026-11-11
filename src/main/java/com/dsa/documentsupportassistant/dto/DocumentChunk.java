package com.dsa.documentsupportassistant.dto;

public record DocumentChunk(
    String documentId,
    String filename,
    int pageNumber,
    int chunkIndex,
    String text
) {}