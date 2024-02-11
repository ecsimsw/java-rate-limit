package ecsimsw.usage;

import ecsimsw.ratelimit.RateLimit;
import lombok.val;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;

@RestController
public class MainController {

    @RateLimit(rate = 1000, burst = 5, noDelay = false)
    @RequestMapping(value = "/foo")
    ResponseEntity<String> handleFoo() {
        return ResponseEntity.ok("hi");
    }
}