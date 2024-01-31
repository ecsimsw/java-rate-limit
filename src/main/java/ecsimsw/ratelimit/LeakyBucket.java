package ecsimsw.ratelimit;

import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
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

        var scheduleService = Executors.newScheduledThreadPool(1);
        scheduleService.scheduleAtFixedRate(() -> {
            if (!waitings.isEmpty()) {
                waitings.poll();
                LOGGER.info("poll, waitings : " + waitings.size());
            }
        }, 0, flowRate, TimeUnit.MILLISECONDS);
    }

    public void offer(T id) {
        try {
            waitings.add(id);
            LOGGER.info("offer, waitings : " + waitings.size());
        } catch (IllegalStateException e) {
            throw new BucketFullException("bucket full");
        }
    }

    public void offerAndWait(T id) throws TimeoutException {
        offer(id);
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
}
