package com.dsa.documentsupportassistant.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AssistantService {
    private static final Logger log = LoggerFactory.getLogger(AssistantService.class);

    private final ChatClient chatClient;

    public AssistantService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String answer(String message){
        long start = System.nanoTime();
        log.info("Sending chat request to OpenAI");

        String reply = chatClient.prompt().user(message).call().content();
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        log.info("Received chat response from OpenAI in {} ms", elapsedMs);
        return reply;
    }
}
