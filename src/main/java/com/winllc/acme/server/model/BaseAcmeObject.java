package com.winllc.acme.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class BaseAcmeObject<T extends BaseAcmeObject> {

    @JsonProperty
    protected String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
