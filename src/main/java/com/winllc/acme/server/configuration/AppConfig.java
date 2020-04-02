package com.winllc.acme.server.configuration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@SpringBootApplication
@EnableMongoRepositories
@ComponentScan("com.winllc.acme.server")
public class AppConfig {

    public static void main(String[] args){
        SpringApplication.run(AppConfig.class, args);
    }

    @Bean(name = "appTaskExecutor")
    public TaskExecutor taskExecutor(){
        ThreadPoolTaskExecutor poolTaskExecutor = new ThreadPoolTaskExecutor();
        poolTaskExecutor.setCorePoolSize(10);
        poolTaskExecutor.setMaxPoolSize(50);
        poolTaskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        poolTaskExecutor.afterPropertiesSet();
        return poolTaskExecutor;
    }
}
