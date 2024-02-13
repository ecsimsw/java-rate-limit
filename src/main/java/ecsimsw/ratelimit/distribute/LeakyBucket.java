package ecsimsw.ratelimit.distribute;

import ecsimsw.ratelimit.BucketFullException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class LeakyBucket<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LeakyBucket.class);
    private static final String BUCKET_KEY = "BUCKET_KEY";

    private final int flowRate;
    private final int capacity;
    private final ListOperations<String, Long> waitings;
    private final BucketLock bucketLock;

    public LeakyBucket(int flowRate, int capacity, RedisTemplate redisTemplate) {
        this.flowRate = flowRate;
        this.capacity = capacity;
        this.waitings = redisTemplate.opsForList();
        this.bucketLock = new BucketLock(redisTemplate);
        fixedFlow(flowRate);
    }

    public void put(Long id) {
        try {
            bucketLock.acquire();
            if (waitings.size(BUCKET_KEY) >= capacity) {
                throw new BucketFullException("bucket full");
            }
            waitings.rightPush(BUCKET_KEY, id);
            LOGGER.info("distributed, waitings : " + waitings.size(BUCKET_KEY));
        } catch (TimeoutException e) {
            throw new BucketFullException("bucket full");
        } finally {
            bucketLock.release();
        }
    }

    public void putAndWait(Long id) throws TimeoutException {
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

    private void fixedFlow(int flowRate) {
        var scheduleService = Executors.newScheduledThreadPool(1);
        scheduleService.scheduleAtFixedRate(() -> {
            if (waitings.size(BUCKET_KEY) > 0) {
                waitings.leftPop(BUCKET_KEY);
                LOGGER.info("release, waitings : " + waitings.size(BUCKET_KEY));
            }
        }, 0, flowRate, TimeUnit.MILLISECONDS);
    }
}
