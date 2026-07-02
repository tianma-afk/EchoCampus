package com.echocampus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
public class EchoCampusApplication {

    public static void main(String[] args) {
        SpringApplication.run(EchoCampusApplication.class, args);
    }
}
