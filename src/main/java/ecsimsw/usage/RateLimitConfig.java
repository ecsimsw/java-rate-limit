package ecsimsw.usage;

import ecsimsw.ratelimit.distribute.RateLimitCounter;
import ecsimsw.ratelimit.distribute.RateLimitFilterRedis;
import ecsimsw.ratelimit.standalone.RateLimitFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
public class RateLimitConfig {

    @ConditionalOnProperty(value = "spring.data.redis.host", havingValue = " ", matchIfMissing = true)
    @Bean
    public RateLimitFilter rateLimitFilterStandAlone() {
        return new RateLimitFilter(1000, 10, false);
    }

    @ConditionalOnProperty(value = "spring.data.redis.host")
    @Bean
    public RateLimitFilterRedis rateLimitFilterDistributed(
        @Autowired RedisTemplate redisTemplate,
        @Autowired RedissonClient redissonClient
    ) {
        var burst = 10;
        var rate =  1000;
        var noDelay = false;
        return new RateLimitFilterRedis(
            new RateLimitCounter(burst, rate, noDelay, redisTemplate, redissonClient)
        );
    }
}
