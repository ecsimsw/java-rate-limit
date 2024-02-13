package ecsimsw.ratelimit.standalone;

import ecsimsw.ratelimit.BucketFullException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

public class LeakyBucket<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LeakyBucket.class);

    private final int flowRate;
    private final int capacity;
    private final BlockingQueue<T> waitings;

    public LeakyBucket(int flowRate, int capacity) {
        this.flowRate = flowRate;
        this.capacity = capacity;
        this.waitings = new ArrayBlockingQueue<>(capacity);
        fixedFlow(flowRate);
    }

    public void put(T id) {
        try {
            waitings.add(id);
            LOGGER.info("put, waitings : " + waitings.size());
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
