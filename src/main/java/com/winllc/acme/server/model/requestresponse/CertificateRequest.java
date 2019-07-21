package com.winllc.acme.server.model.requestresponse;

import org.apache.commons.lang3.StringUtils;

public class CertificateRequest implements RequestValidator {
    //required
    private String csr;

    public String getCsr() {
        return csr;
    }

    public void setCsr(String csr) {
        this.csr = csr;
    }

    @Override
    public boolean isValid() {
        return StringUtils.isNotBlank(csr);
    }
}
