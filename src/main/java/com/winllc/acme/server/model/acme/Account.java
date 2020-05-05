package com.winllc.acme.server.model.acme;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nimbusds.jose.JWSObject;
import com.winllc.acme.server.contants.StatusType;

import java.util.Arrays;

//RFC8555 Section 7.1.2
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Account extends BaseAcmeObject<Account> {

    //optional
    @JsonProperty
    private String[] contact;
    //optional
    @JsonProperty
    private Boolean termsOfServiceAgreed;
    //optional
    @JsonProperty
    private String externalAccountBinding;
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

    public String getExternalAccountBinding() {
        return externalAccountBinding;
    }

    public void setExternalAccountBinding(String externalAccountBinding) {
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

    @JsonIgnore
    public void markValid(){
        setStatus(StatusType.VALID.toString());
    }

    @JsonIgnore
    public void markDeactivated(){
        setStatus(StatusType.DEACTIVATED.toString());
    }

    @JsonIgnore
    public void markRevoked(){
        setStatus(StatusType.REVOKED.toString());
    }

    @Override
    public String toString() {
        return "Account{" +
                "contact=" + Arrays.toString(contact) +
                ", termsOfServiceAgreed=" + termsOfServiceAgreed +
                ", externalAccountBinding=" + externalAccountBinding +
                ", orders='" + orders + '\'' +
                ", status='" + status + '\'' +
                ", resource='" + resource + '\'' +
                '}';
    }
}
