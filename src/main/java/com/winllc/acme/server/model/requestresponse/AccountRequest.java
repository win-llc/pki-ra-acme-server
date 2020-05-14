package com.winllc.acme.server.model.requestresponse;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.util.Base64URL;
import com.winllc.acme.server.model.AcmeJWSObject;

import java.io.IOException;
import java.text.ParseException;
import java.util.Base64;

@JsonInclude(JsonInclude.Include.NON_NULL)
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
    @JsonProperty("externalAccountBinding")
    private ExternalAccountBinding externalAccountBinding;

    private String resource;

    public AccountRequest() {
    }

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

    public ExternalAccountBinding getExternalAccountBinding() {
        return externalAccountBinding;
    }

    public void setExternalAccountBinding(ExternalAccountBinding externalAccountBinding) {
        this.externalAccountBinding = externalAccountBinding;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    @JsonIgnore
    public JWSObject buildExternalAccountJWSObject() throws ParseException {
        return JWSObject.parse(externalAccountBinding.toString());
    }


    /*
    public JWSObject buildExternalAccount() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(externalAccountBinding.getPayload().toJSONObject().toJSONString(), JWSObject.class);
    }

     */
}
