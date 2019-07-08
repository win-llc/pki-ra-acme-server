package com.winllc.acme.server.model.requestresponse;

public class CertificateRequest {
    //required
    private String csr;

    public String getCsr() {
        return csr;
    }

    public void setCsr(String csr) {
        this.csr = csr;
    }
}
