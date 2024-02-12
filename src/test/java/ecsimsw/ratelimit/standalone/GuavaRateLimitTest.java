package ecsimsw.ratelimit.standalone;

import com.google.common.util.concurrent.RateLimiter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Guava rateLimiter 테스트")
public class GuavaRateLimitTest {

    @Test
    public void testGuavaRateLimiter() {
        // Limit request per 0.1 sec
        var ratePerSec = 0.1;
        var queueingCount = 5;

        var rateLimiter = RateLimiter.create(1/ratePerSec);
        var startTime = System.currentTimeMillis();
        for(int i =0; i<queueingCount; i++) {
            rateLimiter.acquire(1);
        }
        var endTime = System.currentTimeMillis();
        assertThat(endTime - startTime).isGreaterThanOrEqualTo((long) (ratePerSec * (queueingCount-1) * 1000));
    }
}
