package com.winllc.acme.server.model.data;

import com.nimbusds.jose.jwk.JWK;
import com.winllc.acme.server.Application;
import com.winllc.acme.server.model.Account;
import com.winllc.acme.server.model.Order;
import com.winllc.acme.server.util.AppUtil;

import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;

public class AccountData extends DataObject<Account> {

    private String jwk;
    private Timestamp lastAgreedToTermsOfServiceOn;

    public AccountData(Account obj) {
        super(obj);
    }

    @Override
    public String buildUrl() {
        return Application.baseURL + "acct/" + getId();
    }

    public String getJwk() {
        return jwk;
    }

    public void setJwk(String jwk) {
        this.jwk = jwk;
    }

    public Timestamp getLastAgreedToTermsOfServiceOn() {
        return lastAgreedToTermsOfServiceOn;
    }

    public void setLastAgreedToTermsOfServiceOn(Timestamp lastAgreedToTermsOfServiceOn) {
        this.lastAgreedToTermsOfServiceOn = lastAgreedToTermsOfServiceOn;
    }

}
