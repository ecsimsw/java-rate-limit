package ecsimsw.ratelimit.standalone;

import ecsimsw.ratelimit.BucketFullException;
import ecsimsw.ratelimit.LeakyBucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

public class LeakyBucketS implements LeakyBucket {

    private static final Logger LOGGER = LoggerFactory.getLogger(LeakyBucketS.class);

    private final int flowRate;
    private final int capacity;
    private final BlockingQueue<Integer> waitings;

    public LeakyBucketS(int flowRate, int capacity) {
        this.flowRate = flowRate;
        this.capacity = capacity;
        this.waitings = new ArrayBlockingQueue<>(capacity);
    }

    public void put(int id) {
        try {
            waitings.add(id);
            LOGGER.info("put, waitings : " + waitings.size());
        } catch (IllegalStateException e) {
            throw new BucketFullException("bucket full");
        }
    }

    public void putAndWait(int id) throws TimeoutException {
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

    public void fixedFlow() {
        var scheduleService = Executors.newScheduledThreadPool(1);
        scheduleService.scheduleAtFixedRate(() -> {
            if (!waitings.isEmpty()) {
                waitings.poll();
                LOGGER.info("release, waitings : " + waitings.size());
            }
        }, 0, flowRate, TimeUnit.MILLISECONDS);
    }
}
