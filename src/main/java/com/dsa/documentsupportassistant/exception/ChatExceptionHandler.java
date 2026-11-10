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

    public record ChatError(String code, String message) {}
}
