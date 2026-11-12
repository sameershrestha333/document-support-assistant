package com.dsa.documentsupportassistant.service;

import com.dsa.documentsupportassistant.dto.DocumentAnswer;
import com.dsa.documentsupportassistant.exception.ModelProviderException;
import com.dsa.documentsupportassistant.tool.ImportJobTools;
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
import com.dsa.documentsupportassistant.exception.ChatLimitException;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.model.tool.ToolCallLimitExceededException;

import java.util.Map;

@Service
public class AssistantService {
    private static final Logger log = LoggerFactory.getLogger(AssistantService.class);

    private final ChatClient chatClient;
    private final ImportJobTools importJobTools;

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
            
            For questions about an import job’s status or failure, use getImportStatus. 
            Base the answer on the tool result. If no job ID is provided, ask for it. For document questions,
            follow the existing source and evidence rules.

            For import jobs, distinguish recorded job facts from suggested next steps.
            Set supported to true when the tool returns job facts, even if guidance is unavailable.
            Give a next step only if the guide excerpt directly supports it, and quote that
            excerpt in evidence. Otherwise leave evidence empty and say guidance is unavailable
            if the user asked for it. If the tool finds no job, set supported to false,
            leave evidence empty, and report that no job was found.
            
            If the user asks what to check and the guide provides a next step, include that step in answer. 
            In evidence, quote only the supporting guide passage.
            """;

    public AssistantService(ChatClient.Builder builder, ImportJobTools importJobTools) {
        ChatMemory memory = MessageWindowChatMemory.builder()
                .maxMessages(10)
                .build();

        this.chatClient = builder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
                .build();
        this.importJobTools = importJobTools;
    }

    public DocumentAnswer answer(String conversationId, String message, String username) {
        return TimeLimits.run(() -> answerInternal(conversationId, message, username),
                15, "CHAT_TIMEOUT");
    }

    private DocumentAnswer answerInternal(String conversationId, String message, String username) {
        long start = System.nanoTime();
        log.info("Sending chat request to OpenAI");
        try {

            var converter = new BeanOutputConverter<>(DocumentAnswer.class);
            ChatResponse response = chatClient.prompt()
                    .system(SYSTEM_PROMPT + "\n\n" + converter.getFormat())
                    .user(message)
                    .advisors(advisor -> advisor.param(
                            ChatMemory.CONVERSATION_ID, username + ":" + conversationId))
                    .tools(importJobTools)
                    .toolContext(Map.of("username", username))
                    .call()
                    .chatResponse();

            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            var generation = response.getResult();
            if (ToolCallLimitExceededException.FINISH_REASON.equals(
                    generation.getMetadata().getFinishReason())) {
                throw new ChatLimitException("TOOL_CALL_LIMIT",
                        "The assistant reached the tool call limit.");
            }

            Usage usage = response.getMetadata().getUsage();

            if (usage != null) {
                log.info("OpenAI replied in {} ms (input: {} , output: {} , total: {}) ", elapsedMs, usage.getPromptTokens(), usage.getCompletionTokens(),
                        usage.getTotalTokens());

            } else {
                log.warn("No Usage info and OpenAI replied in {} ms", elapsedMs);

            }
            return converter.convert(generation.getOutput().getText());
        } catch (OpenAIServiceException | OpenAIIoException ex) {
            log.warn("Model provider request failed ({})", ex.getClass().getSimpleName());
            throw new ModelProviderException(ex);
        }
    }
}
