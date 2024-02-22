package ecsimsw.ratelimit.distribute;

import ecsimsw.ratelimit.BucketFullException;
import ecsimsw.ratelimit.LeakyBucket;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class LeakyBucketD implements LeakyBucket {

    public static final String BUCKET_KEY = "BUCKET_KEY";
    private static final Logger LOGGER = LoggerFactory.getLogger(LeakyBucketD.class);

    private final int flowRate;
    private final int capacity;
    private final ListOperations<String, Integer> waitings;
    private final BucketLock bucketLock;
    private final SchedulerLock schedulerLock;

    public LeakyBucketD(
        int flowRate,
        int capacity,
        RedisTemplate redisTemplate,
        RedissonClient redissonClient
    ) {
        this.flowRate = flowRate;
        this.capacity = capacity;
        this.waitings = redisTemplate.opsForList();
        this.bucketLock = new BucketLock(redissonClient);
        this.schedulerLock = new SchedulerLock(redissonClient);
    }

    public void put(int id) {
        try {
            bucketLock.acquire();
            var size = waitings.size(BUCKET_KEY);
            if (size >= capacity) {
                throw new BucketFullException("bucket full");
            }
            waitings.rightPush(BUCKET_KEY, id);
            LOGGER.info("put, waitings : " + (size + 1));
            bucketLock.release();
        } catch (TimeoutException e) {
            throw new BucketFullException("bucket full");
        }
    }

    public void putAndWait(int id) throws TimeoutException {
        put(id);
        var startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < (long) flowRate * capacity) {
            if (waitings.indexOf(BUCKET_KEY, id) == null) {
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

    @Async
    public void fixedFlow(int flowRate) {
//        while (true) {
            LOGGER.info("Try get lock");
            if (schedulerLock.getLock()) {
                var scheduleService = Executors.newScheduledThreadPool(1);
                scheduleService.scheduleAtFixedRate(() -> {
                    waitings.leftPop(BUCKET_KEY);
                    LOGGER.info("release, waitings : " + waitings.size(BUCKET_KEY));
                }, 0, flowRate, TimeUnit.MILLISECONDS);
            }
//        }
    }
}
