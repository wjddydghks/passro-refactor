package com.passro.passrobackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class PassroBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(PassroBackendApplication.class, args);
    }

}
