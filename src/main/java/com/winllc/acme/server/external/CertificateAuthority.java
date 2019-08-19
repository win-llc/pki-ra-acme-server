package com.winllc.acme.server.external;

import com.winllc.acme.server.contants.ChallengeType;
import com.winllc.acme.server.model.acme.Challenge;
import com.winllc.acme.server.model.acme.Identifier;
import com.winllc.acme.server.model.data.OrderData;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;

public interface CertificateAuthority {

    String getName();
    boolean revokeCertificate(X509Certificate certificate, int reason);
    X509Certificate issueCertificate(OrderData orderData, PKCS10CertificationRequest certificationRequest);
    boolean isCertificateRevoked(X509Certificate certificate);
    Certificate[] getTrustChain();
    List<CAValidationRule> getValidationRules();
    boolean canIssueToIdentifier(Identifier identifier);
    List<ChallengeType> getIdentifierChallengeRequirements(Identifier identifier);
}
