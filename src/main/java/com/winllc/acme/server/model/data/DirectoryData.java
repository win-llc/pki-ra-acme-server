package com.winllc.acme.server.model.data;

import com.winllc.acme.server.Application;
import com.winllc.acme.server.model.acme.Directory;

import java.util.Date;

public class DirectoryData extends DataObject<Directory> {
    private String name;
    private boolean allowPreAuthorization;
    private String mapsToCertificateAuthorityName;
    private String externalAccountProviderName;
    private Date termsOfServiceLastUpdatedOn;

    public DirectoryData(Directory obj) {
        super(obj);
    }

    @Override
    public String buildUrl() {
        return Application.baseURL + name + "/";
    }

    public String buildLinkUrl(){
        return "<" +
                buildUrl() +
                "directory" +
                ">;rel=\"index\"";
    }

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

    public Date getTermsOfServiceLastUpdatedOn() {
        return termsOfServiceLastUpdatedOn;
    }

    public void setTermsOfServiceLastUpdatedOn(Date termsOfServiceLastUpdatedOn) {
        this.termsOfServiceLastUpdatedOn = termsOfServiceLastUpdatedOn;
    }
}
