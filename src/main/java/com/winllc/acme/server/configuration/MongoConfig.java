package com.winllc.acme.server.configuration;

import com.mongodb.MongoClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "com.winllc.acme.server.persistence")
public class MongoConfig extends AbstractMongoConfiguration {

    @Override
    protected String getDatabaseName() {
        return "acme";
    }

    @Override
    public MongoClient mongoClient() {
        return new MongoClient("database.winllc-dev.com", 27017);
    }

    @Override
    protected String getMappingBasePackage() {
        return "com.winllc";
    }
}