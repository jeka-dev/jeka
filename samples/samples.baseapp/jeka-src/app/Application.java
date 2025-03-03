package app;

import dev.jeka.core.tool.JkDep;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@JkDep("org.springframework.boot:spring-boot-dependencies::pom:3.3.5")
@JkDep("org.springframework.boot:spring-boot-starter-web")

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
