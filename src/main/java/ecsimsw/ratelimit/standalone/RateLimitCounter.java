package ecsimsw.ratelimit.standalone;

import ecsimsw.ratelimit.BucketFullException;
import ecsimsw.ratelimit.TooManyRequestException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

public class RateLimitCounter {

    private final AtomicLong requestIds = new AtomicLong(0);

    private final LeakyBucket<Long> bucket;
    private final boolean noDelay;

    public RateLimitCounter(int burst, int rate, boolean noDelay) {
        this.noDelay = noDelay;
        this.bucket = new LeakyBucket<>(rate, burst);
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
