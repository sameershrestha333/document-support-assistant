package com.dsa.documentsupportassistant.service;

import com.dsa.documentsupportassistant.dto.GroundedAnswer;
import com.dsa.documentsupportassistant.dto.RetrievalRequest;
import com.dsa.documentsupportassistant.dto.RetrievedChunk;
import com.dsa.documentsupportassistant.repository.DocumentChunkRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Service
public class DocumentRetrievalService {

    private final DocumentChunkRepository documentChunkRepository;

    private final EmbeddingService embeddingService;

    public DocumentRetrievalService(DocumentChunkRepository documentChunkRepository, EmbeddingService embeddingService) {
        this.documentChunkRepository = documentChunkRepository;
        this.embeddingService = embeddingService;
    }

    public List<RetrievedChunk> search(RetrievalRequest request) {
      float [] questionEmbedding =   embeddingService.embed(request.question());
      return documentChunkRepository.findSimilar(request.documentId(), DocumentIndexingService.INDEX_VERSION, questionEmbedding, 5);

    }
}
