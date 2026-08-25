/*
package com.greentraffic.infrastructure.config;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class HealthCheckConfig {

    @Bean
    public HealthIndicator dbHealthIndicator(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        return new HealthIndicator() {
            @Override
            public Health health() {
                try {
                    Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
                    return Health.up()
                            .withDetail("database", "MySQL")
                            .withDetail("validationQuery", "SELECT 1")
                            .withDetail("result", result)
                            .build();
                } catch (Exception e) {
                    return Health.down()
                            .withDetail("error", e.getMessage())
                            .build();
                }
            }
        };
    }
}
*/
