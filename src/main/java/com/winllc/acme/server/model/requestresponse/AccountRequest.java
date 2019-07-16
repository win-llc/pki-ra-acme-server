package com.winllc.acme.server.model.requestresponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSObject;
import com.winllc.acme.server.external.ExternalAccount;
import com.winllc.acme.server.model.AcmeJWSObject;

import java.io.IOException;

public class AccountRequest {
    //optional
    private String status;
    //optional
    private String[] contact;
    //optional
    private Boolean termsOfServiceAgreed;
    //optional
    private Boolean onlyReturnExisting;
    //optional
    private AcmeJWSObject externalAccountBinding;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

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

    public Boolean getOnlyReturnExisting() {
        return onlyReturnExisting;
    }

    public void setOnlyReturnExisting(Boolean onlyReturnExisting) {
        this.onlyReturnExisting = onlyReturnExisting;
    }

    public AcmeJWSObject getExternalAccountBinding() {
        return externalAccountBinding;
    }

    public void setExternalAccountBinding(AcmeJWSObject externalAccountBinding) {
        this.externalAccountBinding = externalAccountBinding;
    }

    public ExternalAccount buildExternalAccount() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(externalAccountBinding.getPayload().toJSONObject().toJSONString(), ExternalAccount.class);
    }
}
