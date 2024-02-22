package ecsimsw.ratelimit.distribute;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

public class SchedulerLock {

    private static final String LOCK_KEY = "SCHEDULER_LOCK";
    private static final int LOCK_TTL_SEC = 10 * 60;

    private final RLock locks;

    public SchedulerLock(RedissonClient redissonClient) {
        this.locks = redissonClient.getLock(LOCK_KEY);
    }

    public boolean getLock() {
        try {
            return locks.tryLock(1, LOCK_TTL_SEC, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public void release() {
        locks.unlock();
    }
}
