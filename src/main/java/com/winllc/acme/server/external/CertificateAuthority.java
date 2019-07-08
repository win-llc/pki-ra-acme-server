package com.winllc.acme.server.external;

import com.winllc.acme.server.model.Authorization;
import com.winllc.acme.server.model.Identifier;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;

public interface CertificateAuthority {

    String getName();
    boolean revokeCertificate(X509Certificate certificate, int reason);
    X509Certificate issueCertificate(PKCS10CertificationRequest certificationRequest);
    boolean isCertificateRevoked(X509Certificate certificate);
    X509Certificate[] getTrustChain();
    List<CAValidationRule> getValidationRules();
}
