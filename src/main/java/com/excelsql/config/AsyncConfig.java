package com.excelsql.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
/**
 * @Description: 异步处理配置
 * @Author: IT 派同学
 * @Date: 2025-06-30-22:39
 */
@Configuration
public class AsyncConfig {

    @Bean(name = "excelTaskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("Excel-");
        executor.initialize();
        return executor;
    }
}
