package hello;

import java.util.concurrent.atomic.AtomicLong;

import com.google.common.base.Strings;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GreetingController {

    private static final String template = "Hello, %s!";
    private final AtomicLong counter = new AtomicLong();

    @RequestMapping("/greeting")
    public Greeting greeting(@RequestParam(value="name", defaultValue="World") String name) {
        String count = Strings.padStart("" + counter.incrementAndGet(), 3,'0');
        return new Greeting(count, String.format(template, name));
    }
}
