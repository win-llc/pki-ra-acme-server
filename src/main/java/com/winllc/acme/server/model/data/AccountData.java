package com.winllc.acme.server.model.data;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.winllc.acme.server.Application;
import com.winllc.acme.server.model.acme.Account;
import com.winllc.acme.server.model.acme.Directory;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Date;


public class AccountData extends DataObject<Account> {

    private String jwk;
    private String eabKeyIdentifier;
    private Date lastAgreedToTermsOfServiceOn;

    public AccountData(Account object, String directory) {
        super(object, directory);
    }

    @Override
    public String buildUrl() {
        return buildBaseUrl() + "acct/" + getId();
    }

    public String getEabKeyIdentifier() {
        return eabKeyIdentifier;
    }

    public void setEabKeyIdentifier(String eabKeyIdentifier) {
        this.eabKeyIdentifier = eabKeyIdentifier;
    }

    public String getJwk() {
        return jwk;
    }

    public void setJwk(String jwk) {
        this.jwk = jwk;
    }

    public JWK buildJwk(){
        try {
            return JWK.parse(jwk);
        } catch (ParseException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not parse", e);
        }
    }

    public Date getLastAgreedToTermsOfServiceOn() {
        return lastAgreedToTermsOfServiceOn;
    }

    public void setLastAgreedToTermsOfServiceOn(Date lastAgreedToTermsOfServiceOn) {
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
