package com.winllc.acme.server.model.data;

import com.winllc.acme.server.Application;
import com.winllc.acme.server.model.acme.Account;
import com.winllc.acme.server.model.acme.Directory;

import java.sql.Timestamp;

public class AccountData extends DataObject<Account> {

    private String jwk;
    private Timestamp lastAgreedToTermsOfServiceOn;

    public AccountData(Account obj, DirectoryData directoryData) {
        super(obj, directoryData);
    }

    @Override
    public String buildUrl() {
        return buildBaseUrl() + "acct/" + getId();
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

    @Override
    public String toString() {
        return "AccountData{" +
                "jwk='" + jwk + '\'' +
                ", lastAgreedToTermsOfServiceOn=" + lastAgreedToTermsOfServiceOn +
                "} " + super.toString();
    }
}
