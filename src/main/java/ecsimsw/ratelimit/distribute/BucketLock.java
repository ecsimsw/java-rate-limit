package ecsimsw.ratelimit.distribute;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class BucketLock {

    private static final String LOCK_KEY = "BUCKET_LOCK";
    private static final int LOCK_WAIT_TIME = 300;
    private static final int LOCK_TTL = 100;

    private final RLock locks;

    public BucketLock(RedissonClient redissonClient) {
        this.locks = redissonClient.getLock(LOCK_KEY);
    }

    public void acquire() throws TimeoutException {
        try {
            if (!locks.tryLock(LOCK_WAIT_TIME, LOCK_TTL, TimeUnit.MILLISECONDS)) {
                throw new TimeoutException();
            }
        } catch (InterruptedException e) {
            throw new IllegalArgumentException("Thread interrupted");
        }
    }

    public void release() {
        if(locks.isHeldByCurrentThread()) {
            locks.unlock();
        }
    }
}
