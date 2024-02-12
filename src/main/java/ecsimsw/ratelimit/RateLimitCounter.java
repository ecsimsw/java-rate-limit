package ecsimsw.ratelimit;

import ecsimsw.ratelimit.standalone.LeakyBucketStandAlone;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

public class RateLimitCounter {

    private final AtomicLong requestIds = new AtomicLong(0);

    private final int burst;
    private final LeakyBucketStandAlone<Long> bucket;
    private final boolean noDelay;

    public RateLimitCounter(int burst, int rate, boolean noDelay) {
        this.noDelay = noDelay;
        this.burst = burst;
        this.bucket = new LeakyBucketStandAlone<>(rate, burst);
    }

    public void count() {
        var id = requestIds.getAndIncrement() % (burst+1);
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
