package com.osx23.avi.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AviApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AviApiApplication.class, args);
    }
}
