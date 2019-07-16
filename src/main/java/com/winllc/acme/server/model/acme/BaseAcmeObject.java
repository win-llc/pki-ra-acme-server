package com.winllc.acme.server.model.acme;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.winllc.acme.server.contants.StatusType;

public abstract class BaseAcmeObject<T extends BaseAcmeObject> {

    @JsonProperty
    protected String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @JsonIgnore
    public StatusType getStatusType(){
        return StatusType.valueOf(status);
    }
}
