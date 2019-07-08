package com.winllc.acme.server.model.data;

import com.winllc.acme.server.Application;

public class CertData extends DataObject<String[]> {

    //First index is cert
    private String[] certChain;
    private String issuerDn;
    private Long serialNumber;

    public CertData(String[] obj) {
        super(obj);
    }

    @Override
    public String buildUrl() {
        return Application.baseURL + "cert/" + getId();
    }

    @Override
    public String[] getObject() {
        return certChain;
    }

    public String[] getCertChain() {
        return certChain;
    }

    public void setCertChain(String[] certChain) {
        this.certChain = certChain;
    }

    public String getIssuerDn() {
        return issuerDn;
    }

    public void setIssuerDn(String issuerDn) {
        this.issuerDn = issuerDn;
    }

    public Long getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(Long serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String buildReturnString(){
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < certChain.length; i++){
            builder.append(certChain[i]);
            if(i < certChain.length) builder.append(System.lineSeparator());
        }
        return builder.toString();
    }
}
