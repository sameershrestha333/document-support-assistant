package com.dsa.documentsupportassistant.config;

import org.slf4j.MDC;

import java.util.concurrent.Callable;

public final class LogContext {
    private LogContext() {}

    public static <T> Callable<T> capture(Callable<T> work) {
        var captured = MDC.getCopyOfContextMap();
        return () -> {
            var previous = MDC.getCopyOfContextMap();
            try {
                MDC.setContextMap(captured);
                return work.call();
            } finally {
                MDC.setContextMap(previous);
            }
        };
    }
}
