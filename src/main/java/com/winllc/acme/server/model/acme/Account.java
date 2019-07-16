package com.winllc.acme.server.model.acme;

import com.fasterxml.jackson.annotation.JsonProperty;

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
    private Object externalAccountBinding;
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

    public Object getExternalAccountBinding() {
        return externalAccountBinding;
    }

    public void setExternalAccountBinding(Object externalAccountBinding) {
        this.externalAccountBinding = externalAccountBinding;
    }

    public String getOrders() {
        return orders;
    }

    public void setOrders(String orders) {
        this.orders = orders;
    }
}
