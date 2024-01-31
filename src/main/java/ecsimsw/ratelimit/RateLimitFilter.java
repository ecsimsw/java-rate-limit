package ecsimsw.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

public class RateLimitFilter extends OncePerRequestFilter {

    private final AtomicLong requestIds = new AtomicLong(0);

    private final LeakyBucket<Long> bucket;
    private final boolean noDelay;

    public RateLimitFilter(int rate, int burst, boolean noDelay) {
        this.noDelay = noDelay;
        this.bucket = new LeakyBucket<>(rate, burst);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException {
        try {
            var id = requestIds.getAndIncrement();
            if (noDelay) {
                bucket.offer(id);
                filterChain.doFilter(request, response);
                return;
            }
            try {
                bucket.offerAndWait(id);
                filterChain.doFilter(request, response);
            } catch (Exception timeoutException) {
                responseTooManyRequest(response);
            }
        } catch (Exception bucketFull) {
            responseTooManyRequest(response);
        }
    }

    private static void responseTooManyRequest(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("Too many requests");
    }
}
