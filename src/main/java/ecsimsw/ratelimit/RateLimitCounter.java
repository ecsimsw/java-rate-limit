package ecsimsw.ratelimit;

import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimitCounter {

    private final AtomicInteger requestIds = new AtomicInteger(0);

    private final LeakyBucket bucket;
    private final boolean noDelay;

    public RateLimitCounter(LeakyBucket leakyBucket, boolean noDelay) {
        this.noDelay = noDelay;
        this.bucket = leakyBucket;
        this.bucket.fixedFlow();
    }

    public void count() {
        var id = requestIds.getAndIncrement();
        try {
            if (noDelay) {
                bucket.put(id);
                return;
            }
            bucket.putAndWait(id);
        } catch (BucketFullException | TimeoutException e) {
            throw new TooManyRequestException();
        }
    }
}
