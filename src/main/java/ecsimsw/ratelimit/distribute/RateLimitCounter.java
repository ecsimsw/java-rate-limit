package ecsimsw.ratelimit.distribute;

import ecsimsw.ratelimit.BucketFullException;
import ecsimsw.ratelimit.TooManyRequestException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.data.redis.core.RedisTemplate;

public class RateLimitCounter {

    private final AtomicLong requestIds = new AtomicLong(0);

    private final int burst;
    private final LeakyBucket<Long> bucket;
    private final boolean noDelay;

    public RateLimitCounter(int burst, int rate, boolean noDelay, RedisTemplate redisTemplate) {
        this.noDelay = noDelay;
        this.burst = burst;
        this.bucket = new LeakyBucket<>(rate, burst, redisTemplate);
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
