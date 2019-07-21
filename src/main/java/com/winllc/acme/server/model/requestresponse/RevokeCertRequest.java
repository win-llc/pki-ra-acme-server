package com.winllc.acme.server.model.requestresponse;

import com.winllc.acme.server.util.CertUtil;
import org.apache.commons.lang3.StringUtils;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class RevokeCertRequest implements RequestValidator {

    //Required
    private String certificate;
    //Optional
    private Integer reason;

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

    public X509Certificate buildX509Cert() throws CertificateException {
        return CertUtil.base64ToCert(certificate);
    }

    @Override
    public boolean isValid() {
        return StringUtils.isNotBlank(certificate);
    }
}
