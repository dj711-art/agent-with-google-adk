// java
package com.example.agent.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class SimpleRateLimiterTest {

    @Test
    void allowsUpToMaxRequests_thenBlocks() {
        SimpleRateLimiter limiter = new SimpleRateLimiter(3, 1_000L);

        assertTrue(limiter.tryAcquire());
        assertTrue(limiter.tryAcquire());
        assertTrue(limiter.tryAcquire());
        assertFalse(limiter.tryAcquire(), "Fourth request should be blocked when max=3");
    }

    @Test
    void windowExpires_allowsRequestsAgain() throws InterruptedException {
        SimpleRateLimiter limiter = new SimpleRateLimiter(2, 200L);

        assertTrue(limiter.tryAcquire());
        assertTrue(limiter.tryAcquire());
        assertFalse(limiter.tryAcquire());

        // wait for window to pass
        Thread.sleep(250);

        // Should allow again after window expiry
        assertTrue(limiter.tryAcquire());
    }

    @Test
    void concurrentAccess_respectsLimit() throws Exception {
        final int maxRequests = 5;
        final SimpleRateLimiter limiter = new SimpleRateLimiter(maxRequests, 1_000L);

        int threads = 20;
        try (ExecutorService es = Executors.newFixedThreadPool(threads)) {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Boolean>> futures = new ArrayList<>();

            for (int i = 0; i < threads; i++) {
                futures.add(es.submit(() -> {
                    start.await();
                    return limiter.tryAcquire();
                }));
            }

            // start all tasks
            start.countDown();

            int successCount = 0;
            for (Future<Boolean> f : futures) {
                if (f.get()) successCount++;
            }

            // Exactly maxRequests should succeed
            assertEquals(maxRequests, successCount, "Number of successful acquisitions should equal the configured maxRequests");

            es.shutdownNow();
            assertTrue(es.awaitTermination(1, TimeUnit.SECONDS));
        }
    }

    @AfterAll
    static void cleanupSleep() throws InterruptedException {
        // Small pause to avoid timing flakiness for other tests that might run immediately after.
        Thread.sleep(50);
    }
}
