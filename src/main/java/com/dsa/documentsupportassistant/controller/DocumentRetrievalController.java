package com.dsa.documentsupportassistant.controller;

import com.dsa.documentsupportassistant.dto.RetrievalRequest;
import com.dsa.documentsupportassistant.dto.RetrievedChunk;
import com.dsa.documentsupportassistant.service.DocumentRetrievalService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class DocumentRetrievalController {

    private final DocumentRetrievalService documentRetrievalService;

    public DocumentRetrievalController(DocumentRetrievalService documentRetrievalService) {
        this.documentRetrievalService = documentRetrievalService;
    }

    @PostMapping("/api/retrieval/search")
    public List<RetrievedChunk> search(@Valid @RequestBody RetrievalRequest request) {

        return documentRetrievalService.search(request);
    }

}
