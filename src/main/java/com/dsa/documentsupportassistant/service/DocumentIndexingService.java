package com.dsa.documentsupportassistant.service;

import com.dsa.documentsupportassistant.repository.DocumentChunkRepository;
import com.dsa.documentsupportassistant.util.PdfTextExtractor;
import com.dsa.documentsupportassistant.util.TextChunker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class DocumentIndexingService {
    static final int INDEX_VERSION = 1;

    private final DocumentChunkRepository repository;
    private final EmbeddingService embeddingService;

    public DocumentIndexingService(DocumentChunkRepository repository, EmbeddingService embeddingService) {
        this.repository = repository;
        this.embeddingService = embeddingService;
    }

    @Transactional(rollbackFor = IOException.class)
    public String index(Path pdfPath, String owner) throws IOException {
        return index(pdfPath, owner, pdfPath.getFileName().toString());
    }

    @Transactional(rollbackFor = IOException.class)
    public String index(Path pdfPath, String owner, String displayName) throws IOException {
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("An owner username is required.");
        }
        byte[] pdfBytes = Files.readAllBytes(pdfPath);
        String documentId = hash(pdfBytes);

        if (!repository.registerIndex(documentId, INDEX_VERSION)) {
            repository.grantAccess(documentId, INDEX_VERSION, owner);
            return documentId;
        }

        var pages = new PdfTextExtractor().extract(pdfBytes, displayName, documentId);
        var chunker = new TextChunker();
        int chunkCount = 0;
        for (var page : pages) {
            for (var chunk : chunker.split(page)) {
                repository.save(chunk, INDEX_VERSION, embeddingService.embed(chunk.text()));
                chunkCount++;
            }
        }

        if (chunkCount == 0) {
            throw new IllegalArgumentException("The PDF contains no indexable text.");
        }
        repository.grantAccess(documentId, INDEX_VERSION, owner);
        return documentId;
    }

    private static String hash(byte[] pdfBytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(pdfBytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
