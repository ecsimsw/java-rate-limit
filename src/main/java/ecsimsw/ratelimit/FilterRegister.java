package ecsimsw.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import org.reflections.Reflections;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.concurrent.TimeUnit;

@Component
public class FilterRegister implements BeanFactoryPostProcessor {

    final AtomicLong filterIds = new AtomicLong(0);

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        var rateLimitHandlers = AutoConfigurationPackages.get(beanFactory).stream()
            .flatMap(it -> new Reflections(it).getSubTypesOf(Object.class).stream())
            .flatMap(it -> Arrays.stream(it.getDeclaredMethods()))
            .filter(it -> it.isAnnotationPresent(RateLimit.class))
            .toList();

        System.out.println(rateLimitHandlers.size());

        for (var handler : rateLimitHandlers) {
            RateLimit rateLimitConfig = handler.getDeclaredAnnotation(RateLimit.class);
            String[] urlPaths = handler.getDeclaredAnnotation(RequestMapping.class).path();

            int rate = rateLimitConfig.rate();
            int burst = rateLimitConfig.burst();
            boolean noDelay = rateLimitConfig.noDelay();

            var registrationBean = new FilterRegistrationBean<>();
            registrationBean.setFilter(limitFilter(rate, burst, noDelay));
            registrationBean.setOrder(0);
            registrationBean.setEnabled(true);
            registrationBean.addUrlPatterns(urlPaths);
            beanFactory.registerSingleton("rate_limit_filter_" + filterIds.getAndIncrement(), registrationBean);
        }
    }

    private static OncePerRequestFilter limitFilter(int rate, int burst, boolean noDelay) {
        return new OncePerRequestFilter() {

            final AtomicLong requestIds = new AtomicLong(0);
            final BlockingQueue<Long> waitings = new LinkedBlockingQueue<>();
            final ScheduledExecutorService scheduleService = Executors.newScheduledThreadPool(1);

            {
                scheduleService.scheduleAtFixedRate(
                    () -> {
                        waitings.poll();
                        System.out.println("POLL");
                    }, 0, rate, TimeUnit.MILLISECONDS
                );
            }

            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
                if (waitings.size() > burst) {
                    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("Too many requests");
                    return;
                }

                var id = requestIds.getAndIncrement();
                System.out.println("REQUEST : " + System.currentTimeMillis());
                waitings.add(id);

                if (noDelay) {
                    filterChain.doFilter(request, response);
                    return;
                }

                var executor = Executors.newSingleThreadExecutor();
                var future = executor.submit(() -> {
                    while (waitings.contains(id)) {
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });

                try {
                    future.get((long) burst * rate, TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    future.cancel(true);
                } catch (Exception e) {
                    throw new IllegalArgumentException();
                } finally {
                    executor.shutdownNow();
                }

                try {
                    filterChain.doFilter(request, response);
                } catch (ServletException | IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
}
