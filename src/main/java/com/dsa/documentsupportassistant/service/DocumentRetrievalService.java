package com.dsa.documentsupportassistant.service;

import com.dsa.documentsupportassistant.dto.RetrievalRequest;
import com.dsa.documentsupportassistant.dto.RetrievedChunk;
import com.dsa.documentsupportassistant.repository.DocumentChunkRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class DocumentRetrievalService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DocumentRetrievalService.class);


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
        requireAccess(request, owner);
        return searchChunks(request, owner);
    }

    public List<RetrievedChunk> searchHybrid(RetrievalRequest request, String owner) {
        requireAccess(request, owner);
        return searchChunks(request, owner, true);
    }

    private void requireAccess(RetrievalRequest request, String owner) {
        if (owner == null || owner.isBlank()
                || !documentChunkRepository.hasAccess(request.documentId(), DocumentIndexingService.INDEX_VERSION, owner)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
        }
    }

    public List<RetrievedChunk> searchSharedImportGuide(String question) {
        return guideId.isBlank() ? List.of() : searchChunks(new RetrievalRequest(guideId, question), null);
    }

    private List<RetrievedChunk> searchChunks(RetrievalRequest request, String owner) {
        return searchChunks(request, owner, false);
    }

    private List<RetrievedChunk> searchChunks(RetrievalRequest request, String owner, boolean hybrid) {
        long start = System.nanoTime();
        float[] embedding = embeddingService.embed(request.question());
        long searchStart = System.nanoTime();
        int version = DocumentIndexingService.INDEX_VERSION;
        List<RetrievedChunk> chunks = hybrid
                ? documentChunkRepository.findHybrid(request.documentId(), version, embedding, request.question(), owner)
                : documentChunkRepository.findSimilar(request.documentId(), version, embedding, 5, owner);
        log.info("retrievalMode={} embeddingMs={} searchMs={} chunks={}", hybrid ? "hybrid" : "vector",
                (searchStart - start) / 1_000_000, (System.nanoTime() - searchStart) / 1_000_000, chunks.size());
        return chunks;
    }
}
