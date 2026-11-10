package com.dsa.documentsupportassistant.service;

import com.dsa.documentsupportassistant.dto.DocumentAnswer;
import com.dsa.documentsupportassistant.exception.ModelProviderException;
import com.openai.errors.OpenAIIoException;
import com.openai.errors.OpenAIServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

@Service
public class AssistantService {
    private static final Logger log = LoggerFactory.getLogger(AssistantService.class);

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = """
            You are the Document Support Assistant.
            
            Answer using source text supplied in the current message or an earlier
            user message in this conversation. Use previous assistant replies to
            understand follow-up questions. Quote evidence from the supplied source.
            
            If the source contradicts an assumption in the question, correct it.
            If the source explicitly lists the only allowed options and the question
            asks about an option outside that list, answer "No" and quote the restriction.
            
            When the source supports an answer, including a "No":
            - Set supported to true.
            - Put a concise answer in answer.
            - Copy a short, exact excerpt from the source into evidence.
            
            When the source is missing or does not support an answer:
            - Set supported to false.
            - Set answer to "Sorry, I don't have enough information to answer that."
            - Set evidence to an empty string.
            """;

    public AssistantService(ChatClient.Builder builder) {
        ChatMemory memory = MessageWindowChatMemory.builder()
                .maxMessages(10)
                .build();

        this.chatClient = builder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
                .build();
    }

    public DocumentAnswer answer(String conversationId, String message) {
        long start = System.nanoTime();
        log.info("Sending chat request to OpenAI");
        try {

            var result = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(message)
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .call()
                    .responseEntity(DocumentAnswer.class);

            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            ChatResponse response = result.getResponse();

            Usage usage = response.getMetadata().getUsage();

            if (usage != null) {
                log.info("OpenAI replied in {} ms (input: {} , output: {} , total: {}) ", elapsedMs, usage.getPromptTokens(), usage.getCompletionTokens(),
                        usage.getTotalTokens());

            } else {
                log.warn("No Usage info and OpenAI replied in {} ms", elapsedMs);

            }
            return result.entity();
        } catch (OpenAIServiceException | OpenAIIoException ex) {
            log.warn("Model provider request failed ({})", ex.getClass().getSimpleName());
            throw new ModelProviderException(ex);
        }
    }
}
