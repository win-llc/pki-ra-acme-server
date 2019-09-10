package com.winllc.acme.server.model.data;

import com.winllc.acme.server.Application;
import com.winllc.acme.server.model.acme.Directory;

import java.util.Arrays;

public class CertData extends DataObject<String[]> {

    //First index is cert
    private String[] certChain;
    private String issuerDn;
    private Long serialNumber;

    public CertData(String[] object, String directory) {
        super(object, directory);
    }

    @Override
    public String buildUrl() {
        return buildBaseUrl() + "cert/" + getId();
    }

    public String[] getCertChain() {
        if(certChain == null) certChain = new String[0];
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
        String[] chain = getObject();
        for(int i = 0; i < chain.length; i++){
            builder.append(chain[i]);
            if(i < chain.length) builder.append(System.lineSeparator());
        }
        return builder.toString();
    }

    @Override
    public String toString() {
        return "CertData{" +
                "certChain=" + Arrays.toString(certChain) +
                ", issuerDn='" + issuerDn + '\'' +
                ", serialNumber=" + serialNumber +
                "} " + super.toString();
    }
}
