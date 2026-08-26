package com.greentraffic.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan(basePackages = "com.greentraffic")
@SpringBootApplication(scanBasePackages = "com.greentraffic")
public class GreenTrafficBootstrapApplication {

    public static void main(String[] args) {
        SpringApplication.run(GreenTrafficBootstrapApplication.class, args);
    }
}
