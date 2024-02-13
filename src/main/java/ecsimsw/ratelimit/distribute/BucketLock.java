package ecsimsw.ratelimit.distribute;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

public class BucketLock {

    private static final String LOCK_KEY = "BUCKET_LOCK";
    private static final int LOCK_TTL = 100;

    private final ValueOperations locks;

    public BucketLock(RedisTemplate redisTemplate) {
        this.locks = redisTemplate.opsForValue();
    }

    public void acquire() throws TimeoutException {
        var startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < LOCK_TTL + 1) {
            if (locks.setIfAbsent(LOCK_KEY, 0, LOCK_TTL, TimeUnit.MILLISECONDS)) {
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
        locks.getAndDelete(LOCK_KEY);
    }
}
