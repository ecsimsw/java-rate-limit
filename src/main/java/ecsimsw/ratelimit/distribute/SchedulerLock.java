package ecsimsw.ratelimit.distribute;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SchedulerLock {

    private static final String LOCK_KEY = "SCHEDULER_LOCK";
    private static final Logger logger = LoggerFactory.getLogger(SchedulerLock.class);

    private final RLock locks;

    public SchedulerLock(RedissonClient redissonClient) {
        this.locks = redissonClient.getLock(LOCK_KEY);
    }

    public void lockAndRun(long timeout, int flowRate, Runnable command) {
        try {
            while (true) {
                logger.info("try lock");
                if (locks.tryLock(timeout + 1, timeout, TimeUnit.MILLISECONDS)) {
                    break;
                }
            }
            var scheduleService = Executors.newScheduledThreadPool(1);
            var scheduledFuture = scheduleService.scheduleAtFixedRate(
                command, 0, flowRate, TimeUnit.MILLISECONDS
            );
            Thread.sleep(timeout);
            scheduledFuture.cancel(true);
            if (locks.isHeldByCurrentThread()) {
                locks.unlock();
            }
        } catch (InterruptedException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
