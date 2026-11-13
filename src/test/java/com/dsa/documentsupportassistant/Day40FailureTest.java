package com.dsa.documentsupportassistant;

import com.dsa.documentsupportassistant.controller.ChatController;
import com.dsa.documentsupportassistant.controller.DocumentQuestionController;
import com.dsa.documentsupportassistant.dto.RetrievalRequest;
import com.dsa.documentsupportassistant.exception.ChatExceptionHandler;
import com.dsa.documentsupportassistant.exception.ModelProviderException;
import com.dsa.documentsupportassistant.repository.DocumentChunkRepository;
import com.dsa.documentsupportassistant.service.AssistantService;
import com.dsa.documentsupportassistant.service.DocumentQuestionService;
import com.dsa.documentsupportassistant.service.DocumentRetrievalService;
import com.dsa.documentsupportassistant.service.EmbeddingService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class Day40FailureTest {
    @Test
    void deniedDocumentReturns404BeforeEmbedding() throws Exception {
        var repo = mock(DocumentChunkRepository.class);
        var embeddings = mock(EmbeddingService.class);
        var retrieval = new DocumentRetrievalService(repo, embeddings, "");
        var builder = mock(ChatClient.Builder.class);
        var questions = new DocumentQuestionService(builder, retrieval);
        MockMvcBuilders.standaloneSetup(new DocumentQuestionController(questions)).build()
                .perform(post("/api/documents/ask").principal(() -> "user2")
                        .contentType("application/json")
                        .content("""
                                {"documentId":"private-doc","question":"What is the upload limit?"}
                                """))
                .andExpect(status().isNotFound());
        verifyNoInteractions(embeddings);
    }

    @Test
    void providerFailureReturnsStructured502() throws Exception {
        var assistant = mock(AssistantService.class);
        when(assistant.answer(anyString(), anyString(), eq("user")))
                .thenThrow(new ModelProviderException(new IllegalStateException("simulated failure")));
        MockMvcBuilders.standaloneSetup(new ChatController(assistant))
                .setControllerAdvice(new ChatExceptionHandler()).build()
                .perform(post("/api/chat").principal(() -> "user")
                        .contentType("application/json")
                        .content("""
                                {"conversationId":"day40","message":"Hello"}
                                """))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("MODEL_PROVIDER_ERROR"));
    }

    @Test
    void databaseFailureStopsRetrievalBeforeEmbedding() {
        var repo = mock(DocumentChunkRepository.class);
        var embeddings = mock(EmbeddingService.class);
        when(repo.hasAccess(eq("private-doc"), anyInt(), eq("user")))
                .thenThrow(new DataAccessResourceFailureException("simulated outage"));
        var retrieval = new DocumentRetrievalService(repo, embeddings, "");
        assertThrows(DataAccessResourceFailureException.class,
                () -> retrieval.search(new RetrievalRequest("private-doc", "What is the limit?"), "user"));
        verifyNoInteractions(embeddings);
    }
}
