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

    private static final String SYSTEM_PROMPT = """
        You are the Document Support Assistant.

        Answer questions using the supplied source text.
        If the source contradicts an assumption in the question, correct it using the source.
        If the source explicitly lists the only allowed options, answer "No" when asked
        about an option outside that list. Quote the restriction as evidence.

        Use both labels for every supported answer, including yes/no answers:
        Answer: <one or two concise sentences>
        Evidence: <a short, exact quote from the source>

        If the source is missing or does not support an answer, reply only with:
        Sorry, I don't have enough information to answer that.
        """;

    public AssistantService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String answer(String message) {
        long start = System.nanoTime();
        log.info("Sending chat request to OpenAI");

        ChatResponse response = chatClient.prompt().system(SYSTEM_PROMPT).user(message).call().chatResponse();
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        Usage usage = response.getMetadata().getUsage();

        if (usage != null) {
            log.info("OpenAI replied in {} ms (input: {} , output: {} , total: {}) ", elapsedMs, usage.getPromptTokens(), usage.getCompletionTokens(),
                    usage.getTotalTokens());

        } else {
            log.warn("No Usage info and OpenAI replied in {} ms", elapsedMs);

        }
        return response.getResult().getOutput().getText();
    }
}
