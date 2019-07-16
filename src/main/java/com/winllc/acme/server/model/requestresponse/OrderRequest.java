package com.winllc.acme.server.model.requestresponse;

import com.winllc.acme.server.model.acme.Identifier;

public class OrderRequest {
    //required
    private Identifier[] identifiers;
    //optional
    private String notBefore;
    //optional
    private String notAfter;

    public Identifier[] getIdentifiers() {
        return identifiers;
    }

    public void setIdentifiers(Identifier[] identifiers) {
        this.identifiers = identifiers;
    }

    public String getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(String notBefore) {
        this.notBefore = notBefore;
    }

    public String getNotAfter() {
        return notAfter;
    }

    public void setNotAfter(String notAfter) {
        this.notAfter = notAfter;
    }
}
