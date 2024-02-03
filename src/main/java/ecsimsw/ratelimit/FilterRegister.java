package ecsimsw.ratelimit;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class FilterRegister implements BeanFactoryPostProcessor {

    private static final AtomicLong FILTER_IDS = new AtomicLong(0);

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        var controllers = AutoConfigurationPackages.get(beanFactory).stream()
            .flatMap(it -> new Reflections(it, new SubTypesScanner(false)).getSubTypesOf(Object.class).stream())
            .filter(it -> it.isAnnotationPresent(Controller.class) || it.isAnnotationPresent(RestController.class))
            .toList();
        var handlers = controllers.stream()
            .flatMap(it -> Arrays.stream(it.getDeclaredMethods()))
            .filter(it -> it.isAnnotationPresent(RequestMapping.class))
            .toList();
        var rateLimitHandlers = handlers.stream()
            .filter(it -> it.isAnnotationPresent(RateLimit.class))
            .toList();

        for (var handler : rateLimitHandlers) {
            registerRateLimitFilter(
                beanFactory,
                handler.getDeclaredAnnotation(RateLimit.class),
                handler.getDeclaredAnnotation(RequestMapping.class).value()
            );
        }
    }

    private static void registerRateLimitFilter(ConfigurableListableBeanFactory beanFactory, RateLimit rateLimitConfig, String[] urlPaths) {
        var registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new RateLimitFilter(
            rateLimitConfig.rate(),
            rateLimitConfig.burst(),
            rateLimitConfig.noDelay()
        ));
        registrationBean.setOrder(0);
        registrationBean.setEnabled(true);
        registrationBean.addUrlPatterns(urlPaths);

        var filterName = "rate_limit_filter_" + FILTER_IDS.getAndIncrement();
        beanFactory.registerSingleton(filterName, registrationBean);
    }
}
