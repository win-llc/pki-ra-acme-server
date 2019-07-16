package com.winllc.acme.server.external;

import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import java.security.cert.X509Certificate;
import java.util.List;

public interface CertificateAuthority {

    String getName();
    boolean revokeCertificate(X509Certificate certificate, int reason);
    X509Certificate issueCertificate(PKCS10CertificationRequest certificationRequest);
    boolean isCertificateRevoked(X509Certificate certificate);
    X509Certificate[] getTrustChain();
    List<CAValidationRule> getValidationRules();
}
