package ecsimsw.ratelimit.distribute;

import java.util.concurrent.TimeoutException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.support.atomic.RedisAtomicInteger;

public class BucketLock {

    private static final String LOCK_KEY = "BUCKET_LOCK";
    private static final int LOCK_WAIT_TIME_OUT_MS = 10000;

    private final RedisAtomicInteger locks;

    public BucketLock(RedisTemplate redisTemplate) {
        this.locks = new RedisAtomicInteger(LOCK_KEY, redisTemplate);
    }

    public void acquire() throws TimeoutException {
        var startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < LOCK_WAIT_TIME_OUT_MS) {
            if (locks.compareAndSet(0, 1)) {
                return;
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        throw new TimeoutException("time out");
    }

    public void release() {
        locks.compareAndSet(1, 0);
    }
}
