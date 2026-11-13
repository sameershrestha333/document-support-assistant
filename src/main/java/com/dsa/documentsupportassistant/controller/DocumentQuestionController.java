package com.dsa.documentsupportassistant.controller;

import com.dsa.documentsupportassistant.dto.DocumentQuestionResponse;
import com.dsa.documentsupportassistant.dto.RetrievalRequest;
import com.dsa.documentsupportassistant.service.DocumentQuestionService;
import jakarta.validation.Valid;
import com.dsa.documentsupportassistant.config.LogContext;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.Executors;
import jakarta.annotation.PreDestroy;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class DocumentQuestionController {

    private final java.util.concurrent.ExecutorService streams = Executors.newVirtualThreadPerTaskExecutor();

    private final DocumentQuestionService documentQuestionService;

    public DocumentQuestionController(DocumentQuestionService documentQuestionService) {
        this.documentQuestionService = documentQuestionService;
    }


    @PostMapping("/api/documents/ask")
    public DocumentQuestionResponse ask(@Valid @RequestBody RetrievalRequest request, Principal principal) {

        return documentQuestionService.ask(request, principal.getName());
    }


    @PostMapping(value = "/api/documents/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@Valid @RequestBody RetrievalRequest request, Principal principal) {
        var prepared = documentQuestionService.prepareQuestion(request, principal.getName());
        var emitter = new SseEmitter(60_000L);
        var task = streams.submit(LogContext.capture(() -> {
            try {
                var answer = documentQuestionService.streamAnswer(prepared,
                        text -> send(emitter, "delta", Map.of("text", text)));
                send(emitter, "complete", answer);
                emitter.complete();
            } catch (UncheckedIOException disconnected) {
                emitter.completeWithError(disconnected.getCause());
            } catch (RuntimeException failure) {
                try {
                    send(emitter, "error", Map.of("message", "Could not complete the answer."));
                    emitter.complete();
                } catch (UncheckedIOException disconnected) {
                    emitter.completeWithError(disconnected.getCause());
                }
            }
            return null;
        }));
        emitter.onTimeout(() -> task.cancel(true));
        emitter.onError(error -> task.cancel(true));
        emitter.onCompletion(() -> task.cancel(true));
        return emitter;
    }

    private static void send(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data, MediaType.APPLICATION_JSON));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @PreDestroy
    void stopStreams() {
        streams.shutdownNow();
    }
}
