package com.winllc.acme.server.external;

import java.util.Map;

public class ExternalAccountProviderSettings {

    private String name;
    private String linkedDirectoryName;
    private String accountVerificationUrl;
    private Map<String, String> additionalSettings;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLinkedDirectoryName() {
        return linkedDirectoryName;
    }

    public void setLinkedDirectoryName(String linkedDirectoryName) {
        this.linkedDirectoryName = linkedDirectoryName;
    }

    public String getAccountVerificationUrl() {
        return accountVerificationUrl;
    }

    public void setAccountVerificationUrl(String accountVerificationUrl) {
        this.accountVerificationUrl = accountVerificationUrl;
    }

    public Map<String, String> getAdditionalSettings() {
        return additionalSettings;
    }

    public void setAdditionalSettings(Map<String, String> additionalSettings) {
        this.additionalSettings = additionalSettings;
    }
}
