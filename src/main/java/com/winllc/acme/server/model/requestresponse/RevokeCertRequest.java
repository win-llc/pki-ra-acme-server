package com.winllc.acme.server.model.requestresponse;

import com.winllc.acme.common.util.CertUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class RevokeCertRequest implements RequestValidator {

    private String resource;
    //Required
    private String certificate;
    //Optional
    private Integer reason;

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    public Integer getReason() {
        return reason;
    }

    public void setReason(Integer reason) {
        this.reason = reason;
    }

    public X509Certificate buildX509Cert() throws CertificateException, IOException {
        return CertUtil.base64ToCert(certificate);
    }

    @Override
    public boolean isValid() {
        return StringUtils.isNotBlank(certificate);
    }
}
