package ratelimiter;

import ratelimiter.enums.RateLimitType;
import ratelimiter.enums.UserTier;
import ratelimiter.factory.RateLimiterFactory;
import ratelimiter.limiter.RateLimiter;
import ratelimiter.model.RateLimitConfig;
import ratelimiter.model.User;
import ratelimiter.service.RateLimiterService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    // call allowRequest 20 times simultaneously (the video's check)
    static void checkConcurrency(RateLimiterService rateLimiterService) throws InterruptedException {
        User freeUser1 = new User("user1", UserTier.FREE);

        int threads = 20; // simulate 20 concurrent requests
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        CyclicBarrier barrier = new CyclicBarrier(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger allowedCount = new AtomicInteger();

        for (int i = 1; i <= threads; i++) {
            executor.submit(() -> {
                try {
                    // all threads wait here until barrier is full
                    barrier.await();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (rateLimiterService.allowRequest(freeUser1)) {
                    allowedCount.incrementAndGet();
                }

                latch.countDown();
            });
        }

        latch.await(); // wait for all threads to finish
        executor.shutdown();

        System.out.println(threads + " simultaneous requests for user1 (FREE, 10 req / 60 s): "
                + allowedCount.get() + " ALLOWED, " + (threads - allowedCount.get()) + " BLOCKED");
    }

    // Runs the same race many times on a fresh limiter. A thread-safe limiter allows exactly 10 every time.
    static void raceManyTimes(RateLimitType algo, int trials) throws Exception {
        int threads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        int exact = 0;

        for (int t = 0; t < trials; t++) {
            RateLimiter limiter = RateLimiterFactory.createRateLimiter(algo, new RateLimitConfig(10, 60));
            CyclicBarrier barrier = new CyclicBarrier(threads);
            List<Future<Boolean>> answers = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                answers.add(executor.submit(() -> {
                    barrier.await();
                    return limiter.allowRequest("user1");
                }));
            }

            int allowed = 0;
            for (Future<Boolean> answer : answers) {
                if (answer.get()) allowed++;
            }
            if (allowed == 10) exact++;
        }

        executor.shutdown();
        System.out.printf("%-19s %d/%d races allowed exactly 10 of 20%n", algo, exact, trials);
    }

    public static void main(String[] args) throws Exception {
        RateLimiterService rateLimiterService = new RateLimiterService();

        System.out.println("=== The video's check: 20 requests at the same instant ===");
        checkConcurrency(rateLimiterService);

        System.out.println("\n=== 500 races per algorithm (20 threads, limit 10) ===");
        raceManyTimes(RateLimitType.TOKEN_BUCKET, 500);
        raceManyTimes(RateLimitType.FIXED_WINDOW, 500);
        raceManyTimes(RateLimitType.SLIDING_WINDOW_LOG, 500);
    }
}
