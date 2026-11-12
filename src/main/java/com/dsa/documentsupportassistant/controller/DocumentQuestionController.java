package com.dsa.documentsupportassistant.controller;

import com.dsa.documentsupportassistant.dto.DocumentQuestionResponse;
import com.dsa.documentsupportassistant.dto.GroundedAnswer;
import com.dsa.documentsupportassistant.dto.RetrievalRequest;
import com.dsa.documentsupportassistant.dto.RetrievedChunk;
import com.dsa.documentsupportassistant.service.DocumentQuestionService;
import com.dsa.documentsupportassistant.service.DocumentRetrievalService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.security.Principal;

@RestController
public class DocumentQuestionController {

    private final DocumentQuestionService documentQuestionService;

    public DocumentQuestionController(DocumentQuestionService documentQuestionService) {
        this.documentQuestionService = documentQuestionService;
    }


    @PostMapping("/api/documents/ask")
    public DocumentQuestionResponse ask(@Valid @RequestBody RetrievalRequest request, Principal principal) {

        return documentQuestionService.ask(request, principal.getName());
    }


}
