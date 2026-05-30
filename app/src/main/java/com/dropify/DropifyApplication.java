package com.dropify;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.dropify")
public class DropifyApplication {

    public static void main(String[] args) {
        SpringApplication.run(DropifyApplication.class, args);
    }
}
