package com.dsa.documentsupportassistant.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ChatExceptionHandler {

    @ExceptionHandler(ModelProviderException.class)
    public ResponseEntity<ChatError> handleModelProviderFailure() {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ChatError(
                        "MODEL_PROVIDER_ERROR",
                        "The assistant could not complete a request to the model provider."
                ));
    }

    @ExceptionHandler(ChatLimitException.class)
    public ResponseEntity<ChatError> handleChatLimit(ChatLimitException ex) {
        HttpStatus status = switch (ex.code()) {
            case "CHAT_TIMEOUT" -> HttpStatus.GATEWAY_TIMEOUT;
            case "CHAT_INTERRUPTED" -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.BAD_GATEWAY;
        };
        return ResponseEntity.status(status).body(new ChatError(ex.code(), ex.getMessage()));
    }

    public record ChatError(String code, String message) {}
}
