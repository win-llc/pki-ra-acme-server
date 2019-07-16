package com.winllc.acme.server.external;

import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import java.security.cert.X509Certificate;
import java.util.List;

public class CertificateAuthorityImpl implements CertificateAuthority {

    private String name;

    public CertificateAuthorityImpl(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean revokeCertificate(X509Certificate certificate, int reason) {
        //TODO
        return false;
    }

    @Override
    public X509Certificate issueCertificate(PKCS10CertificationRequest certificationRequest) {
        return null;
    }

    @Override
    public X509Certificate[] getTrustChain() {
        return new X509Certificate[0];
    }

    @Override
    public boolean isCertificateRevoked(X509Certificate certificate) {
        //TODO
        return false;
    }

    @Override
    public List<CAValidationRule> getValidationRules() {
        return null;
    }


}
