package civsim.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot Application entrypoint for Human Life & Civilization Simulator web backend.
 */
@SpringBootApplication(scanBasePackages = "civsim")
public class CivSimWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(CivSimWebApplication.class, args);
    }
}

