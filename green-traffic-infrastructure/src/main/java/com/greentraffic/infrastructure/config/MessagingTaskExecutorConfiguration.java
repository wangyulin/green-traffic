package com.greentraffic.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@Profile("dev")
public class MessagingTaskExecutorConfiguration {

    @Value("${messaging.executor.core-pool-size:2}")
    private int corePoolSize;

    @Value("${messaging.executor.max-pool-size:8}")
    private int maxPoolSize;

    @Value("${messaging.executor.queue-capacity:1000}")
    private int queueCapacity;

    @Value("${messaging.executor.thread-name-prefix:msg-exec-}")
    private String threadNamePrefix;

    @Bean(name = "messageTaskExecutor")
    public Executor messageTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
