package com.winllc.acme.server;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.winllc.acme.server.configuration.AppConfig;
import com.winllc.acme.server.service.AbstractServiceTest;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.ldap.LdapAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        EmbeddedMongoAutoConfiguration.class,
        ElasticsearchDataAutoConfiguration.class,
        LdapAutoConfiguration.class
})
@ComponentScan(basePackages = {"com.winllc.acme.server"},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {AppConfig.class}
        ))
@EnableMongoRepositories(basePackages = "com.winllc.acme.server.persistence")
@EnableConfigurationProperties
@ConfigurationPropertiesScan("com.winllc.acme.server.properties")
//@PropertySource(value = "classpath:application-local.properties")
@EnableCaching
public class TestConfig {

    //@Value("${spring.data.mongodb.uri}")
    // String uri;

    public static void main(String[] args){
        SpringApplication application = new SpringApplication(TestConfig.class);
        application.run(args);
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


    @Bean
    public Caffeine caffeineConfig() {
        return Caffeine.newBuilder().expireAfterWrite(600, TimeUnit.MINUTES);
    }
    @Bean
    public CacheManager cacheManager(Caffeine caffeine) {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager("usedNonce", "unusedNonce");
        caffeineCacheManager.setCaffeine(caffeine);
        return caffeineCacheManager;
    }

}
