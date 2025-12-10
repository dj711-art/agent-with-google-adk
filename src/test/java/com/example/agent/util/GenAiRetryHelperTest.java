package com.example.agent.util;

import com.google.genai.errors.ClientException;
import com.google.genai.errors.ServerException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class GenAiRetryHelperTest {

    @Test
    void callWithRetry_successOnFirstAttempt() throws Exception {
        Callable<String> task = () -> "success";
        String result = GenAiRetryHelper.callWithRetry(task, 3, 100L);
        assertEquals("success", result);
    }

    @Test
    void callWithRetry_retries429AndSucceeds() throws Exception {
        AtomicInteger attempts = new AtomicInteger(0);
        Callable<String> task = () -> {
            int attempt = attempts.incrementAndGet();
            if (attempt < 3) {
                throw new ClientException(429, "Too Many Requests", "Rate limit exceeded");
            }
            return "success";
        };

        String result = GenAiRetryHelper.callWithRetry(task, 5, 100L);
        assertEquals("success", result);
        assertEquals(3, attempts.get());
    }

    @Test
    void callWithRetry_retries503AndSucceeds() throws Exception {
        AtomicInteger attempts = new AtomicInteger(0);
        Callable<String> task = () -> {
            int attempt = attempts.incrementAndGet();
            if (attempt < 3) {
                throw new ServerException(503, "Service Unavailable", "The model is overloaded. Please try again later.");
            }
            return "success";
        };

        String result = GenAiRetryHelper.callWithRetry(task, 5, 100L);
        assertEquals("success", result);
        assertEquals(3, attempts.get());
    }

    @Test
    void callWithRetry_exhaustsRetriesOn429() {
        AtomicInteger attempts = new AtomicInteger(0);
        Callable<String> task = () -> {
            attempts.incrementAndGet();
            throw new ClientException(429, "Too Many Requests", "Rate limit exceeded");
        };

        assertThrows(ClientException.class, () -> 
            GenAiRetryHelper.callWithRetry(task, 3, 100L)
        );
        assertEquals(4, attempts.get()); // 1 initial + 3 retries
    }

    @Test
    void callWithRetry_exhaustsRetriesOn503() {
        AtomicInteger attempts = new AtomicInteger(0);
        Callable<String> task = () -> {
            attempts.incrementAndGet();
            throw new ServerException(503, "Service Unavailable", "The model is overloaded. Please try again later.");
        };

        assertThrows(ServerException.class, () -> 
            GenAiRetryHelper.callWithRetry(task, 3, 100L)
        );
        assertEquals(4, attempts.get()); // 1 initial + 3 retries
    }

    @Test
    void callWithRetry_doesNotRetryNon429ClientException() {
        AtomicInteger attempts = new AtomicInteger(0);
        Callable<String> task = () -> {
            attempts.incrementAndGet();
            throw new ClientException(400, "Bad Request", "Invalid input");
        };

        assertThrows(ClientException.class, () -> 
            GenAiRetryHelper.callWithRetry(task, 3, 100L)
        );
        assertEquals(1, attempts.get()); // Only 1 attempt, no retries
    }

    @Test
    void callWithRetry_doesNotRetryNon503ServerException() {
        AtomicInteger attempts = new AtomicInteger(0);
        Callable<String> task = () -> {
            attempts.incrementAndGet();
            throw new ServerException(500, "Internal Server Error", "Server error");
        };

        assertThrows(ServerException.class, () -> 
            GenAiRetryHelper.callWithRetry(task, 3, 100L)
        );
        assertEquals(1, attempts.get()); // Only 1 attempt, no retries
    }

    @Test
    void callWithRetry_handlesOverloadedInMessage() throws Exception {
        AtomicInteger attempts = new AtomicInteger(0);
        Callable<String> task = () -> {
            int attempt = attempts.incrementAndGet();
            if (attempt < 2) {
                throw new ServerException(503, "Service Unavailable", "503 . The model is overloaded. Please try again later.");
            }
            return "success";
        };

        String result = GenAiRetryHelper.callWithRetry(task, 5, 100L);
        assertEquals("success", result);
        assertEquals(2, attempts.get());
    }

    @Test
    void callWithRetry_handles429InMessage() throws Exception {
        AtomicInteger attempts = new AtomicInteger(0);
        Callable<String> task = () -> {
            int attempt = attempts.incrementAndGet();
            if (attempt < 2) {
                throw new ClientException(429, "Too Many Requests", "429 rate limit");
            }
            return "success";
        };

        String result = GenAiRetryHelper.callWithRetry(task, 5, 100L);
        assertEquals("success", result);
        assertEquals(2, attempts.get());
    }
}
