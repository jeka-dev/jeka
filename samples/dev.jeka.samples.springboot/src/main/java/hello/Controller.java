package hello;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class Controller {

    @Autowired
    private GreetingProvider greetingProvider;

    @GetMapping("/")
    public Greeting helloWorld() {
        return greetingProvider.get();
    }

    @Value("${my.value}")
    private String myvalue;

    @PostConstruct
    public void post() {
        System.out.println("----------------" + myvalue);
    }
}
