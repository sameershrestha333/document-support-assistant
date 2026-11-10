package com.dsa.documentsupportassistant.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

@Service
public class AssistantService {
    private static final Logger log = LoggerFactory.getLogger(AssistantService.class);

    private final ChatClient chatClient;

    public AssistantService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String answer(String message) {
        long start = System.nanoTime();
        log.info("Sending chat request to OpenAI");

        ChatResponse response = chatClient.prompt().user(message).call().chatResponse();
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        Usage usage = response.getMetadata().getUsage();

        if (usage != null) {
            log.info("OpenAI replied in {} ms (input: {} , output: {} , total: {}) ", elapsedMs, usage.getPromptTokens(), usage.getCompletionTokens(),
                    usage.getTotalTokens());

        } else {
            log.info("No Usage info and OpenAI replied in {} ms", elapsedMs);

        }
        return response.getResult().getOutput().getText();
    }
}
