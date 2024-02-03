package ecsimsw;

import ecsimsw.ratelimit.RateLimit;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MainController {

    @RateLimit(rate = 100, burst = 5, noDelay = true)
    @RequestMapping(value = "/foo")
    ResponseEntity<String> handleFoo() {
        return ResponseEntity.ok("hi");
    }
}
