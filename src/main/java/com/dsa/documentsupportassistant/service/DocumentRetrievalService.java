package com.dsa.documentsupportassistant.service;

import com.dsa.documentsupportassistant.dto.GroundedAnswer;
import com.dsa.documentsupportassistant.dto.RetrievalRequest;
import com.dsa.documentsupportassistant.dto.RetrievedChunk;
import com.dsa.documentsupportassistant.repository.DocumentChunkRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Service
public class DocumentRetrievalService {

    private final DocumentChunkRepository documentChunkRepository;

    private final EmbeddingService embeddingService;

    private final String guideId;

    public DocumentRetrievalService(DocumentChunkRepository documentChunkRepository,
                                    EmbeddingService embeddingService,
                                    @Value("${app.import-guide.document-id:}") String guideId) {
        this.guideId = guideId;
        this.documentChunkRepository = documentChunkRepository;
        this.embeddingService = embeddingService;
    }

    public List<RetrievedChunk> search(RetrievalRequest request, String owner) {
        if (owner == null || owner.isBlank()
                || !documentChunkRepository.hasAccess(request.documentId(), DocumentIndexingService.INDEX_VERSION, owner)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
        }
        return searchChunks(request, owner);
    }

    public List<RetrievedChunk> searchSharedImportGuide(String question) {
        return guideId.isBlank() ? List.of() : searchChunks(new RetrievalRequest(guideId, question), null);
    }

    private List<RetrievedChunk> searchChunks(RetrievalRequest request, String owner) {
      float [] questionEmbedding =   embeddingService.embed(request.question());
      return documentChunkRepository.findSimilar(request.documentId(), DocumentIndexingService.INDEX_VERSION, questionEmbedding, 5, owner);

    }
}
