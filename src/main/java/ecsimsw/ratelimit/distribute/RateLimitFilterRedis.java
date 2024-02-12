package ecsimsw.ratelimit.distribute;

import ecsimsw.ratelimit.TooManyRequestException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

public class RateLimitFilterRedis extends OncePerRequestFilter {

    private final RateLimitCounter rateLimitCounter;

    public RateLimitFilterRedis(int rate, int burst, boolean noDelay, RedisTemplate redisTemplate) {
        this.rateLimitCounter = new RateLimitCounter(burst, rate, noDelay, redisTemplate);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            rateLimitCounter.count();
            filterChain.doFilter(request, response);
        } catch (TooManyRequestException e) {
            responseTooManyRequest(response);
        }
    }

    private static void responseTooManyRequest(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("Too many requests");
    }
}
