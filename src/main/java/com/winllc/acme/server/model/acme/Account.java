package com.winllc.acme.server.model.acme;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nimbusds.jose.JWSObject;

//RFC8555 Section 7.1.2
public class Account extends BaseAcmeObject<Account> {

    //optional
    @JsonProperty
    private String[] contact;
    //optional
    @JsonProperty
    private Boolean termsOfServiceAgreed;
    //optional
    @JsonProperty
    private JWSObject externalAccountBinding;
    //required
    @JsonProperty
    private String orders;

    public String[] getContact() {
        return contact;
    }

    public void setContact(String[] contact) {
        this.contact = contact;
    }

    public Boolean getTermsOfServiceAgreed() {
        return termsOfServiceAgreed;
    }

    public void setTermsOfServiceAgreed(Boolean termsOfServiceAgreed) {
        this.termsOfServiceAgreed = termsOfServiceAgreed;
    }

    public JWSObject getExternalAccountBinding() {
        return externalAccountBinding;
    }

    public void setExternalAccountBinding(JWSObject externalAccountBinding) {
        this.externalAccountBinding = externalAccountBinding;
    }

    public String getOrders() {
        return orders;
    }

    public void setOrders(String orders) {
        this.orders = orders;
    }

    @Override
    public boolean isValid() {
        return orders != null;
    }
}
