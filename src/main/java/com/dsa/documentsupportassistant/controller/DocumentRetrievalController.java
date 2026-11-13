package com.dsa.documentsupportassistant.controller;

import com.dsa.documentsupportassistant.dto.RetrievalRequest;
import com.dsa.documentsupportassistant.dto.RetrievedChunk;
import com.dsa.documentsupportassistant.service.DocumentRetrievalService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.security.Principal;

@RestController
public class DocumentRetrievalController {

    private final DocumentRetrievalService documentRetrievalService;

    public DocumentRetrievalController(DocumentRetrievalService documentRetrievalService) {
        this.documentRetrievalService = documentRetrievalService;
    }

    @PostMapping("/api/retrieval/search")
    public List<RetrievedChunk> search(@Valid @RequestBody RetrievalRequest request, Principal principal,
                                       @RequestParam(defaultValue = "vector") String mode) {

        if (!"vector".equals(mode) && !"hybrid".equals(mode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mode must be vector or hybrid");
        }
        return "hybrid".equals(mode)
                ? documentRetrievalService.searchHybrid(request, principal.getName())
                : documentRetrievalService.search(request, principal.getName());
    }

}
