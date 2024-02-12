package ecsimsw.ratelimit.standalone;

import ecsimsw.ratelimit.testUtils.ConcurrentTestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("버킷 동시성 문제 처리 학습 테스트")
class LeakyBucketStandAloneConcurrentTest {

    int numberOfThreads = 10;

    @DisplayName("동시 요청시 bucket 의 동시성 문제를 확인한다.")
    @Test
    public void testWithoutConcurrent() {
        int additionCount = 2000;
        var bucket = new LinkedList<>();
        ConcurrentTestUtils.concurrentRequestScenario(numberOfThreads, () -> {
            for (var j = 0; j < additionCount; j++) {
                bucket.offerLast(0);
            }
        });
        assertThat(bucket.size()).isNotEqualTo(additionCount * numberOfThreads);
    }

    @DisplayName("Blocking queue 로 동시성 문제 처리를 확인한다.")
    @Test
    public void testWithConcurrentLib() {
        int additionCount = 2000;
        var bucket = new ArrayBlockingQueue<>(additionCount * numberOfThreads);
        ConcurrentTestUtils.concurrentRequestScenario(numberOfThreads, () -> {
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
}