package app;

import dev.jeka.core.tool.JkInjectClasspath;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@JkInjectClasspath("org.springframework.boot:spring-boot-dependencies::pom:3.2.0")
@JkInjectClasspath("org.springframework.boot:spring-boot-starter-web")

@SpringBootApplication
public class Application {



    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
