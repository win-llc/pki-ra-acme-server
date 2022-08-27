package com.winllc.acme.server.properties;

import java.util.Date;
import java.util.List;

public class AcmeDefaultProperties {
    private String name;

    //Directory fields
    private boolean allowPreAuthorization;
    private String mapsToCertificateAuthorityName;
    private String externalAccountProviderName;

    //META
    private String metaTermsOfService;
    private String metaWebsite;
    private boolean metaExternalAccountRequired;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isAllowPreAuthorization() {
        return allowPreAuthorization;
    }

    public void setAllowPreAuthorization(boolean allowPreAuthorization) {
        this.allowPreAuthorization = allowPreAuthorization;
    }

    public String getMapsToCertificateAuthorityName() {
        return mapsToCertificateAuthorityName;
    }

    public void setMapsToCertificateAuthorityName(String mapsToCertificateAuthorityName) {
        this.mapsToCertificateAuthorityName = mapsToCertificateAuthorityName;
    }

    public String getExternalAccountProviderName() {
        return externalAccountProviderName;
    }

    public void setExternalAccountProviderName(String externalAccountProviderName) {
        this.externalAccountProviderName = externalAccountProviderName;
    }


    public String getMetaTermsOfService() {
        return metaTermsOfService;
    }

    public void setMetaTermsOfService(String metaTermsOfService) {
        this.metaTermsOfService = metaTermsOfService;
    }

    public String getMetaWebsite() {
        return metaWebsite;
    }

    public void setMetaWebsite(String metaWebsite) {
        this.metaWebsite = metaWebsite;
    }

    public boolean isMetaExternalAccountRequired() {
        return metaExternalAccountRequired;
    }

    public void setMetaExternalAccountRequired(boolean metaExternalAccountRequired) {
        this.metaExternalAccountRequired = metaExternalAccountRequired;
    }
}
