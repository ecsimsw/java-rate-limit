package ecsimsw.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("동시성 문제 처리 학습 테스트")
class RateLimitCounterTest {

    int numberOfThreads = 10;
    int additionCount = 2000;

    @DisplayName("동시 요청시 bucket 의 동시성 문제를 확인한다.")
    @Test
    public void testWithoutConcurrent() {
        var bucket = new LinkedList<>();
        concurrentRequestScenario(() -> {
            for (var j = 0; j < additionCount; j++) {
                bucket.offerLast(0);
            }
        });
        assertThat(bucket.size()).isNotEqualTo(additionCount * numberOfThreads);
    }

    @DisplayName("Blocking queue 로 동시성 문제 처리를 확인한다.")
    @Test
    public void testWithConcurrentLib() {
        var bucket = new ArrayBlockingQueue<>(additionCount * numberOfThreads);
        concurrentRequestScenario(() -> {
            try {
                for (var j = 0; j < additionCount; j++) {
                    bucket.put(0);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        assertThat(bucket.size()).isEqualTo(additionCount * numberOfThreads);
    }

    private void concurrentRequestScenario(Runnable concurrentRequestScene) {
        try (
            var service = Executors.newFixedThreadPool(numberOfThreads);
        ) {
            var latch = new CountDownLatch(numberOfThreads);
            for (var i = 0; i < numberOfThreads; i++) {
                service.execute(concurrentRequestScene);
                latch.countDown();
            }
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}