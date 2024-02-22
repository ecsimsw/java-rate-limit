package ecsimsw.usage;

import ecsimsw.ratelimit.LeakyBucket;
import ecsimsw.ratelimit.RateLimitCounter;
import ecsimsw.ratelimit.RateLimitFilter;
import ecsimsw.ratelimit.distribute.LeakyBucketD;
import ecsimsw.ratelimit.standalone.LeakyBucketS;
import jakarta.annotation.PostConstruct;
import lombok.NoArgsConstructor;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

@NoArgsConstructor
@Configuration
public class RateLimitConfig {

    private static final int burst = 10;
    private static final int rate = 1000;
    private static final boolean noDelay = false;

    @ConditionalOnProperty(value = "spring.data.redis.host", havingValue = " ", matchIfMissing = true)
    @Bean
    public LeakyBucket leakyBucketStandAlone() {
        return new LeakyBucketS(rate, burst);
    }

    @ConditionalOnProperty(value = "spring.data.redis.host")
    @Bean
    public LeakyBucket leakyBucketDistributed(
        @Autowired RedisTemplate redisTemplate,
        @Autowired RedissonClient redissonClient
    ) {
        return new LeakyBucketD(rate, burst, redisTemplate, redissonClient);
    }

    @Bean
    public RateLimitFilter rateLimitFilter(LeakyBucket leakyBucket) {
        return new RateLimitFilter(
            new RateLimitCounter(leakyBucket, noDelay)
        );
    }
}
