package com.spendwise;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@Slf4j
public class SpendwiseApplication {

    public static void main(String[] args) {
        log.info("Starting Spendwise backend");
        SpringApplication.run(SpendwiseApplication.class, args);
    }
}
