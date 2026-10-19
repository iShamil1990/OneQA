package io.oneqa.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "io.oneqa")
public class OneQaApplication {
    public static void main(String[] args) {
        SpringApplication.run(OneQaApplication.class, args);
    }
}
