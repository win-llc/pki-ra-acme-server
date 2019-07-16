package com.winllc.acme.server.model;

import javax.servlet.http.HttpServletRequest;
import java.util.Objects;
import java.util.Optional;

public class AcmeURL {

    //e.g. https://example.com/acme/order/TOlocE8rfgo/finalize
    private String url;

    private String serverUrl;
    private String directoryIdentifier;
    private String acmeAction;
    private String objectId;
    private String extraAction ;

    public AcmeURL(String url){
        this.url = url;

        String temp = url.replace("https://", "");

        String[] split = temp.split("/");
        this.serverUrl = "https://"+split[0];
        this.directoryIdentifier = split[1];
        this.acmeAction = split[2];

        if(split.length > 3) this.objectId = split[3];
        if(split.length > 4) this.extraAction = split[4];
    }

    public AcmeURL(HttpServletRequest request){
        this(request.getRequestURL().toString());
    }

    public String getUrl() {
        return url;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public String getDirectoryIdentifier() {
        return directoryIdentifier;
    }

    public String getAcmeAction() {
        return acmeAction;
    }

    public Optional<String> getObjectId() {
        return Optional.of(this.objectId);
    }

    public Optional<String> getExtraAction() {
        return Optional.of(this.extraAction);
    }

    @Override
    public String toString() {
        return this.url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AcmeURL acmeURL = (AcmeURL) o;
        return url.equals(acmeURL.url) &&
                serverUrl.equals(acmeURL.serverUrl) &&
                directoryIdentifier.equals(acmeURL.directoryIdentifier) &&
                acmeAction.equals(acmeURL.acmeAction) &&
                Objects.equals(objectId, acmeURL.objectId) &&
                Objects.equals(extraAction, acmeURL.extraAction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, serverUrl, directoryIdentifier, acmeAction, objectId, extraAction);
    }
}
