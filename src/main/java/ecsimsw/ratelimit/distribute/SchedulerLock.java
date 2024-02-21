package ecsimsw.ratelimit.distribute;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

public class SchedulerLock {

    private static final String LOCK_KEY = "SCHEDULER_LOCK";
    private static final int LOCK_TTL_MIN = 10;

    private final RLock locks;

    public SchedulerLock(RedissonClient redissonClient) {
        this.locks = redissonClient.getLock(LOCK_KEY);
    }

    public boolean getLock() {
        try {
            return locks.tryLock(LOCK_TTL_MIN, LOCK_TTL_MIN, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public long getTTLAsMillis() {
        return LOCK_TTL_MIN * 1000 * 60;
    }

    public void release() {
        locks.unlock();
    }
}
