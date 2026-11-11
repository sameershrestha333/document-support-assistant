package com.dsa.documentsupportassistant.dto;

public record RetrievedChunk(String filename,
                             int pageNumber,
                             int chunkIndex,
                             String text,
                             double distance) {
}
