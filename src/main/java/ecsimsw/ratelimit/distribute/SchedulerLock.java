package ecsimsw.ratelimit.distribute;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class SchedulerLock {

    private static final String LOCK_KEY = "SCHEDULER_LOCK";
    private static final Logger logger = LoggerFactory.getLogger(SchedulerLock.class);

    private final RLock locks;

    public SchedulerLock(RedissonClient redissonClient) {
        this.locks = redissonClient.getLock(LOCK_KEY);
    }

    public void fixedRate(long flowRate, Runnable command) {
        try {
            while (true) {
                logger.info("try lock");
                if (locks.tryLock(flowRate, flowRate, TimeUnit.MILLISECONDS)) {
                    break;
                }
            }
            var startCommandTime = System.currentTimeMillis();
            command.run();
            var jobDuration = System.currentTimeMillis() - startCommandTime;
            if (flowRate - jobDuration > 0) {
                Thread.sleep(flowRate - jobDuration);
            }
        } catch (InterruptedException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
