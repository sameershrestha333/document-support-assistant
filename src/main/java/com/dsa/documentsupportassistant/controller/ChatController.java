package com.dsa.documentsupportassistant.controller;

import com.dsa.documentsupportassistant.service.AssistantService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    private final AssistantService assistantService;

    public ChatController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping("/api/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        String answer = assistantService.answer(request.message());
        return new ChatResponse(answer);
    }

    public record ChatRequest(@NotBlank @Size(max = 2000) String message) {
    }

    public record ChatResponse(String answer) {
    }
}
