package com.winllc.acme.server.model.requestresponse;

import org.apache.commons.lang3.StringUtils;

public class CertificateRequest implements RequestValidator {
    //required
    private String csr;
    private String resource;

    public String getCsr() {
        return csr;
    }

    public void setCsr(String csr) {
        this.csr = csr;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    @Override
    public boolean isValid() {
        return StringUtils.isNotBlank(csr);
    }
}
