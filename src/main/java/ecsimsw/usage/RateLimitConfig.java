package ecsimsw.usage;

import ecsimsw.ratelimit.RateLimitCounter;
import ecsimsw.ratelimit.RateLimitFilter;
import ecsimsw.ratelimit.distribute.LeakyBucketD;
import ecsimsw.ratelimit.standalone.LeakyBucketS;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
public class RateLimitConfig {

    private static final int burst = 10;
    private static final int rate = 1000;
    private static final boolean noDelay = false;

    @ConditionalOnProperty(value = "spring.data.redis.host", havingValue = " ", matchIfMissing = true)
    @Bean
    public RateLimitCounter rateLimitCounterStandAlone() {
        var bucket = new LeakyBucketS(rate, burst);
        bucket.fixedFlow(rate);
        return new RateLimitCounter(bucket, noDelay);
    }

    @ConditionalOnProperty(value = "spring.data.redis.host")
    @Bean
    public RateLimitCounter rateLimitCounterDistributed(
        @Autowired RedisTemplate redisTemplate,
        @Autowired RedissonClient redissonClient
    ) {
        var bucket = new LeakyBucketD(rate, burst, redisTemplate, redissonClient);
        bucket.fixedFlow(rate);
        return new RateLimitCounter(bucket, noDelay);
    }

    @Bean
    public RateLimitFilter rateLimitFilter(RateLimitCounter counter) {
        return new RateLimitFilter(counter);
    }
}
