package com.winllc.acme.server.model.requestresponse;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface RequestValidator {

    @JsonIgnore
    boolean isValid();
}
