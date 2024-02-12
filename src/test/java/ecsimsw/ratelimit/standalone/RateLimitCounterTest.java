package ecsimsw.ratelimit.standalone;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RateLimitCounter 테스트")
public class RateLimitCounterTest {

    @Test
    public void testRateLimitCounter() {
        // Limit request per 0.1 sec
        var rate = 100;
        var queueingCount = 5;

        var rateLimiter = new RateLimitCounter(100, rate, false);

        var startTime = System.currentTimeMillis();
        for(int i =0; i<queueingCount; i++) {
            rateLimiter.count();
        }
        var endTime = System.currentTimeMillis();

        assertThat(endTime - startTime).isGreaterThanOrEqualTo(rate * (queueingCount-1));
    }
}
