package hello;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class Controller {

    @GetMapping("/")
    public Greeting helloWorld() {
        return new Greeting("Hi", "World");
    }

    @Value("${my.value}")
    private String myvalue;

    public static class Greeting {

        public String salutation;

        public String whom;

        public Greeting(String salutation, String whom) {
            this.salutation = salutation;
            this.whom = whom;
        }

    }

    @PostConstruct
    public void post() {
        System.out.println("----------------" + myvalue);
    }
}
