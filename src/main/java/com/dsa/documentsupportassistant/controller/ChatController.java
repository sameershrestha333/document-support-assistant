package com.dsa.documentsupportassistant.controller;

import com.dsa.documentsupportassistant.dto.ChatRequest;
import com.dsa.documentsupportassistant.dto.DocumentAnswer;
import com.dsa.documentsupportassistant.service.AssistantService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
public class ChatController {

    private final AssistantService assistantService;

    public ChatController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping("/api/chat")
    public DocumentAnswer chat(@Valid @RequestBody ChatRequest request, Principal principal) {
        return assistantService.answer(request.conversationId(), request.message(), principal.getName());
    }


}
