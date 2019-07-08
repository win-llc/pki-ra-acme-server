package com.winllc.acme.server.model.requestresponse;

import com.nimbusds.jose.jwk.JWK;

public class KeyChangeRequest {
    //required
    private String account;
    //required
    private JWK oldKey;

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public JWK getOldKey() {
        return oldKey;
    }

    public void setOldKey(JWK oldKey) {
        this.oldKey = oldKey;
    }
}
