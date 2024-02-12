package ecsimsw.usage;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MainController {

    @RequestMapping(value = "/foo")
    ResponseEntity<String> handleFoo() {
        return ResponseEntity.ok("hi");
    }
}
