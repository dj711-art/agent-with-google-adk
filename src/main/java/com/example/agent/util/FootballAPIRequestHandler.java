package com.example.agent.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;



public class FootballAPIRequestHandler {
    public static final String BASE_URL = "https://api.football-data.org/v4";
    private static final Logger LOGGER = Logger.getLogger(FootballAPIRequestHandler.class.getName());
    // This rate liimiter allows 10 requests per 60 seconds and it is specific to football API, this is a better place to put it
    private static final SimpleRateLimiter RATE_LIMITER = new SimpleRateLimiter(10, 60_000L);
    // Add this field near other statics in FootballAgent
    private static final ConcurrentHashMap<String, String> URL_CACHE = new ConcurrentHashMap<>();

    // Helper: simple HTTP GET with X-Auth-Token header. Returns response body or null on failure.
    public static String fetchUrlWithApiKey(String url, String apiKey) {
        LOGGER.info("Fetching URL: " + url);
        // Enforce rate limit (10 requests per 60 seconds)
        // Enforce rate limit (10 requests per 60 seconds)
        if (!RATE_LIMITER.tryAcquire()) {
            LOGGER.warning("Rate limit exceeded for fetchUrlWithApiKey — attempting to return cached response if available.");
            String cached = URL_CACHE.get(url);
            if (cached != null) {
                LOGGER.info("Returning cached response for URL: " + url);
                return cached;
            }
            LOGGER.warning("No cached response available for URL: " + url);
            return null;
        }
        try (HttpClient client = HttpClient.newHttpClient()){
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("X-Auth-Token", apiKey)
                    .GET()
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            LOGGER.info(String.format("Football Agent status code: %s response: %s" , resp.statusCode(),resp.body()));
            if (resp.statusCode() / 100 == 2) {
                String body = resp.body();
                // update URL cache on successful fetch
                URL_CACHE.put(url, body);
                return body;
            } else {
                // non-2xx: try to return cached if present
                String cached = URL_CACHE.get(url);
                if (cached != null) {
                    LOGGER.info("Non-2xx response — returning cached response for URL: " + url);
                    return cached;
                }
                return null;
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return handleException(url, "Interrupted — returning cached response for URL: ", "Football Agent fetchUrlWithApiKey interrupted: ", ie.getMessage(),ie);
        } catch (IOException ioe) {
            return handleException(url, "IO error — returning cached response for URL: ", "Football Agent fetchUrlWithApiKey IO error: ", ioe.getMessage(),ioe);
        }
    }

    @Nullable
    private static String handleException(String url, String message, String errorMsg, String excMsg,Exception exception) {
        String cached = URL_CACHE.get(url);
        String rca = getRootCause(exception);
        LOGGER.info(message + url);
        LOGGER.log(Level.SEVERE, errorMsg + excMsg + rca);
        return cached;
    }

    @NotNull
    private static String getRootCause(Throwable throwable) {
        StringBuilder rca = new StringBuilder();
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable current = throwable;
        while (current != null && !seen.contains(current)) {
            seen.add(current);
            Throwable cause = current.getCause();
            if (cause != null) {
                rca.append(" Caused by: ").append(cause.toString());
            }
            current = cause;
        }
        if (current != null) { // cycle detected
            rca.append(" [cycle detected]");
        }
        return rca.toString();
    }
}
