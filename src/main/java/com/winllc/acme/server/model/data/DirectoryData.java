package com.winllc.acme.server.model.data;

import com.winllc.acme.common.DirectoryDataSettings;
import com.winllc.acme.server.Application;
import com.winllc.acme.server.model.acme.Directory;
import com.winllc.acme.server.model.acme.Meta;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDate;
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

    public static DirectoryData buildFromSettings(DirectoryDataSettings settings){

        String directoryBaseUrl = Application.baseURL+settings.getName()+"/";
        Directory directory = new Directory();
        directory.setNewNonce(directoryBaseUrl+"new-nonce");
        directory.setNewAccount(directoryBaseUrl+"new-account");
        directory.setNewOrder(directoryBaseUrl+"new-order");

        //Only include if allowed, 7.4.1
        if(settings.isAllowPreAuthorization()) {
            directory.setNewAuthz(directoryBaseUrl + "new-authz");
        }

        directory.setRevokeCert(directoryBaseUrl+"revoke-cert");
        directory.setKeyChange(directoryBaseUrl+"key-change");

        Meta meta = new Meta();
        meta.setTermsOfService(settings.getMetaTermsOfService());
        meta.setWebsite(Application.baseURL);
        meta.setCaaIdentities(settings.getMetaCaaIdentities());
        meta.setExternalAccountRequired(settings.isMetaExternalAccountRequired());

        directory.setMeta(meta);

        DirectoryData directoryData = new DirectoryData(directory);
        directoryData.setAllowPreAuthorization(settings.isAllowPreAuthorization());
        directoryData.setName(settings.getName());
        directoryData.setMapsToCertificateAuthorityName(settings.getMapsToCertificateAuthorityName());
        directoryData.setExternalAccountProviderName(settings.getExternalAccountProviderName());
        directoryData.setTermsOfServiceLastUpdatedOn(settings.getTermsOfServiceLastUpdatedOn());

        return directoryData;
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

    @Override
    public String toString() {
        return "DirectoryData{" +
                "name='" + name + '\'' +
                ", allowPreAuthorization=" + allowPreAuthorization +
                ", mapsToCertificateAuthorityName='" + mapsToCertificateAuthorityName + '\'' +
                ", externalAccountProviderName='" + externalAccountProviderName + '\'' +
                ", termsOfServiceLastUpdatedOn=" + termsOfServiceLastUpdatedOn +
                "} " + super.toString();
    }
}
