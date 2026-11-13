package com.dsa.documentsupportassistant.service;

import com.dsa.documentsupportassistant.exception.ChatLimitException;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class TimeLimits {
    private TimeLimits() {}

    public static <T> T run(Callable<T> operation, long seconds, String timeoutCode) {
        FutureTask<T> task = new FutureTask<>(operation);
        Thread.startVirtualThread(task);
        try {
            return task.get(seconds, TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            task.cancel(true);
            throw new ChatLimitException(timeoutCode, "The operation timed out.");
        } catch (InterruptedException ex) {
            task.cancel(true);
            Thread.currentThread().interrupt();
            throw new ChatLimitException("CHAT_INTERRUPTED", "The request was interrupted.");
        } catch (ExecutionException ex) {
            if (ex.getCause() instanceof RuntimeException cause) {
                throw cause;
            }
            if (ex.getCause() instanceof Error cause) {
                throw cause;
            }
            throw new IllegalStateException("The operation failed.", ex.getCause());
        }
    }
}
