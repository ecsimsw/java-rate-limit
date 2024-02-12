package ecsimsw.ratelimit.testUtils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

public class ConcurrentTestUtils {

    public static void concurrentRequestScenario(int numberOfThreads, Runnable concurrentRequestScene) {
        try (
            var service = Executors.newFixedThreadPool(numberOfThreads);
        ) {
            var latch = new CountDownLatch(numberOfThreads);
            for (var i = 0; i < numberOfThreads; i++) {
                service.execute(() -> {
                    concurrentRequestScene.run();
                    latch.countDown();
                });
            }
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
