package com.triptrace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;


@SpringBootApplication
@EnableJpaAuditing
public class TriptraceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TriptraceApplication.class, args);
    }

}
