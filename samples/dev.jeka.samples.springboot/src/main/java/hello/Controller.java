package hello;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class Controller {

    @GetMapping("/")
    public Greeting helloWorld() {
        return new Greeting("Hi", "World");
    }

    public static class Greeting {

        public String salutation;

        public String whom;

        public Greeting(String salutation, String whom) {
            this.salutation = salutation;
            this.whom = whom;
        }

    }
}
