package ecsimsw.ratelimit.distribute;

import ecsimsw.ratelimit.BucketFullException;
import ecsimsw.ratelimit.TooManyRequestException;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimitCounter {

    private final AtomicInteger requestIds = new AtomicInteger(0);

    private final LeakyBucket bucket;
    private final boolean noDelay;

    public RateLimitCounter(int burst, int rate, boolean noDelay, RedisTemplate redisTemplate) {
        this.noDelay = noDelay;
        this.bucket = new LeakyBucket(rate, burst, redisTemplate);
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
