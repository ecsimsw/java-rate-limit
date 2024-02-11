package ecsimsw.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

import java.util.concurrent.*;

public class LeakyBucketDistributed<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LeakyBucketDistributed.class);

    private final int flowRate;
    private final int capacity;
    private final BlockingQueue<T> waitings;
    private final ListOperations<String, String> listOperations;


    public LeakyBucketDistributed(int flowRate, int capacity, RedisTemplate<String, String> redisTemplate) {
        this.flowRate = flowRate;
        this.capacity = capacity;
        this.waitings = new ArrayBlockingQueue<>(capacity);
        this.listOperations = redisTemplate.opsForList();
        fixedFlow(flowRate);
    }

    public void put(T id) {
        try {
            waitings.add(id);
            LOGGER.info("block, waitings : " + waitings.size());
        } catch (IllegalStateException e) {
            throw new BucketFullException("bucket full");
        }
    }

    public void putAndWait(T id) throws TimeoutException {
        put(id);
        var startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < (long) flowRate * capacity) {
            if (!waitings.contains(id)) {
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
            if (!waitings.isEmpty()) {
                waitings.poll();
                LOGGER.info("release, waitings : " + waitings.size());
            }
        }, 0, flowRate, TimeUnit.MILLISECONDS);
    }
}
