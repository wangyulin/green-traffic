package com.greentraffic.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;

@EnableScheduling
@ConfigurationPropertiesScan(basePackages = "com.greentraffic")
@SpringBootApplication(scanBasePackages = "com.greentraffic"
//        ,
//        exclude = {
//            DataSourceAutoConfiguration.class
//        }
)
public class GreenTrafficApplication implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static void main(String[] args) {
        SpringApplication.run(GreenTrafficApplication.class, args);
    }

    @Override
    public void run(String... args) {
        String result = jdbcTemplate.queryForObject("SELECT 1", String.class);
        logger.info("数据库连接成功: {}", result);
    }
}
