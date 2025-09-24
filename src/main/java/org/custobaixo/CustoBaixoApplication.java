package org.custobaixo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling

public class CustoBaixoApplication {
    public static void main(String[] args) {
        SpringApplication.run(CustoBaixoApplication.class, args);
    }
}