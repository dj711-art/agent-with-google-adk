package com.example.agent.util;// java

import com.google.genai.errors.ClientException;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GenAiRetryHelper {
    private static final Logger LOGGER = Logger.getLogger(GenAiRetryHelper.class.getName());

    private GenAiRetryHelper() {}

    /**
     * Call a task and retry on 429. Honors a numeric "Retry-After" if found in the exception message
     * or headers (attempts reflective inspection). Uses exponential backoff with jitter otherwise.
     *
     * @param task the callable that performs the GenAI request
     * @param maxRetries maximum retry attempts (excluding the first try)
     * @param baseDelayMillis base delay used for exponential backoff (ms)
     * @param <T> return type
     * @return task result
     * @throws Exception if non-retriable or max retries exhausted
     */
    public static <T> T callWithRetry(Callable<T> task, int maxRetries, long baseDelayMillis) throws Exception {
        int attempt = 0;
        while (true) {
            try {
                return task.call();
            } catch (ClientException e) {
                if (!is429(e)) throw e;
                attempt++;
                if (attempt > maxRetries) throw e;

                Integer retryAfterSec = extractRetryAfterSeconds(e);
                long waitMillis;
                if (retryAfterSec != null && retryAfterSec > 0) {
                    waitMillis = TimeUnit.SECONDS.toMillis(retryAfterSec);
                } else {
                    // exponential backoff with jitter
                    long expo = baseDelayMillis * (1L << (attempt - 1));
                    long jitter = ThreadLocalRandom.current().nextLong(0, Math.min(3000, expo / 4) + 1);
                    waitMillis = expo + jitter;
                }

                LOGGER.info(String.format("Received 429 - retrying in %d ms (attempt %d/%d)", waitMillis, attempt, maxRetries));
                Thread.sleep(waitMillis);
            }
        }
    }

    // Basic detection: check message text contains 429 or Too Many Requests
    private static boolean is429(ClientException e) {
        String m = e.getMessage();
        if (m != null && (m.contains("429") || m.toLowerCase().contains("too many requests"))) return true;
        // try reflective access if exception type provides status or code
        try {
            Method codeMethod = e.getClass().getMethod("getStatusCode");
            Object code = codeMethod.invoke(e);
            if (code instanceof Number && ((Number) code).intValue() == 429) return true;
        } catch (Exception ignore) {}
        return false;
    }

    // Try parsing Retry-After (seconds) from message, or inspect response headers via reflection.
    private static Integer extractRetryAfterSeconds(ClientException e) {
        String msg = e.getMessage();
        if (msg != null) {
            // common forms: "Retry-After: 120" or "retry after 120 seconds"
            Matcher m = Pattern.compile("(?i)Retry-After\\s*[:=]\\s*(\\d+)").matcher(msg);
            if (m.find()) return Integer.parseInt(m.group(1));
            m = Pattern.compile("(?i)retry after\\s*(\\d+)\\s*seconds?").matcher(msg);
            if (m.find()) return Integer.parseInt(m.group(1));
        }

        // Attempt reflective access to response headers (best-effort; API may differ)
        try {
            Method getResponse = e.getClass().getMethod("getResponse");
            Object response = getResponse.invoke(e);
            if (response != null) {
                // common shape: response.getHeaders() -> Map<String, List<String>>
                Method getHeaders = response.getClass().getMethod("getHeaders");
                Object headersObj = getHeaders.invoke(response);
                if (headersObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, List<String>> headers = (Map<String, List<String>>) headersObj;
                    for (String key : headers.keySet()) {
                        if ("retry-after".equalsIgnoreCase(key) || "Retry-After".equalsIgnoreCase(key)) {
                            List<String> vals = headers.get(key);
                            if (vals != null && !vals.isEmpty()) {
                                String v = vals.getFirst().trim();
                                try {
                                    return Integer.parseInt(v);
                                } catch (NumberFormatException ignore) {}
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        return null;
    }
}