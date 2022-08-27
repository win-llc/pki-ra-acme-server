package com.winllc.acme.server.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "acme")
public class AcmeProperties {
    private AcmeDefaultProperties defaultDirectory;

    public AcmeDefaultProperties getDefaultDirectory() {
        return defaultDirectory;
    }

    public void setDefaultDirectory(AcmeDefaultProperties defaultDirectory) {
        this.defaultDirectory = defaultDirectory;
    }
}
