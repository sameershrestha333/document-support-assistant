package com.dsa.documentsupportassistant.exception;

public class ChatLimitException extends RuntimeException {
    private final String code;

    public ChatLimitException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
