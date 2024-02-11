package ecsimsw;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@SpringBootApplication
public class MainApplication {

    public static void main(String[] args) {
        var app = new SpringApplication(MainApplication.class);
        ConfigurableApplicationContext ctx = app.run();

        TestComp bean = ctx.getBean(TestComp.class);
        bean.test();
    }
}

@Component
class TestComp {

    private final RedisTemplate<String, String> redisTemplate;

    public TestComp(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void test() {
        ListOperations<String, String> listOperations = redisTemplate.opsForList();
        String listKey = "listKey";

        List<Long> list = List.of(1L, 2L, 3L, 4L, 5L);

        for (Long l : list) {
            listOperations.leftPush(listKey, l.toString());
        }
    }
}
