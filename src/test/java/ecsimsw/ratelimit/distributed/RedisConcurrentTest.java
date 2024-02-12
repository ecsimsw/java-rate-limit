package ecsimsw.ratelimit.distributed;

import static org.assertj.core.api.Assertions.assertThat;

import ecsimsw.ratelimit.testUtils.ConcurrentTestUtils;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.support.atomic.RedisAtomicInteger;

@DisplayName("Redis 의 원자성을 확인한다.")
@SpringBootTest
public class RedisConcurrentTest {

    private static final int NUMBER_OF_THREADS = 10;
    private static String COUNT_KEY;
    private static String LOCK_KEY;

    private final int countLimit = 1000;
    private final ValueOperations<String, Integer> ops;
    private final RedisAtomicInteger redisAtomicInteger;

    public RedisConcurrentTest(
        @Autowired RedisTemplate redisTemplate
    ) {
        this.ops = redisTemplate.opsForValue();
        this.redisAtomicInteger = new RedisAtomicInteger(COUNT_KEY, redisTemplate);
    }

    @BeforeAll
    public static void init() {
        COUNT_KEY = UUID.randomUUID().toString();
        LOCK_KEY = UUID.randomUUID().toString();
    }

    @AfterEach
    public void clear() {
        ops.getAndDelete(COUNT_KEY);
        ops.getAndDelete(LOCK_KEY);
    }

    @DisplayName("원자성이 보장되지 않는 상황을 테스트한다.")
    @Test
    public void testNotAtomicity() {
        ops.set(COUNT_KEY, 0);
        ConcurrentTestUtils.concurrentRequestScenario(NUMBER_OF_THREADS, () -> {
            for (int j = 0; j < 200; j++) {
                if (ops.get(COUNT_KEY) < countLimit) {
                    ops.increment(COUNT_KEY, 1);
                }
            }
        });
        assertThat(ops.get(COUNT_KEY)).isNotEqualTo(countLimit);
    }

    @DisplayName("RedisAtomicInteger 로 원자성을 보장한다.")
    @Test
    public void testRedisAtomicInteger() {
        redisAtomicInteger.set(0);
        ConcurrentTestUtils.concurrentRequestScenario(NUMBER_OF_THREADS, () -> {
            for (int j = 0; j < 200; j++) {
                int now = redisAtomicInteger.get();
                if (now < countLimit) {
                    redisAtomicInteger.compareAndSet(now, now + 1);
                }
            }
        });
        assertThat(ops.get(COUNT_KEY)).isEqualTo(countLimit);
    }

    @DisplayName("Set if not exists 으로 Lock 을 구현한다.")
    @Test
    public void test() {
        ops.set(COUNT_KEY, 0);
        ConcurrentTestUtils.concurrentRequestScenario(NUMBER_OF_THREADS, () -> {
            for (int j = 0; j < 5000; j++) {
                boolean isLocked = ops.setIfAbsent(LOCK_KEY, 0);
                if(isLocked) {
                    if (ops.get(COUNT_KEY) < countLimit) {
                        ops.increment(COUNT_KEY, 1);
                    }
                    ops.getAndDelete(LOCK_KEY);
                }
            }
        });
        assertThat(ops.get(COUNT_KEY)).isEqualTo(countLimit);
    }
}
