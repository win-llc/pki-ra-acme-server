package com.winllc.acme.server.external;

import com.winllc.acme.common.*;
import com.winllc.acme.common.contants.ChallengeType;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.common.model.acme.Identifier;
import com.winllc.acme.common.model.data.AccountData;
import com.winllc.acme.common.model.data.OrderData;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface CertificateAuthority {

    String getName();
    AcmeCertAuthorityType getType();
    boolean revokeCertificate(X509Certificate certificate, int reason) throws AcmeServerException;
    X509Certificate issueCertificate(OrderData orderData, String eabKid, PKCS10CertificationRequest certificationRequest) throws AcmeServerException;
    Optional<CertificateDetails> getCertificateDetails(String serial);
    CertRevocationStatus isCertificateRevoked(X509Certificate certificate);
    Certificate[] getTrustChain() throws AcmeServerException;
    AccountValidationResponse getValidationRules(AccountData accountData) throws AcmeServerException;
    boolean canIssueToIdentifier(Identifier identifier, AccountData accountData) throws AcmeServerException;
    List<ChallengeType> getIdentifierChallengeRequirements(Identifier identifier, AccountData accountData) throws AcmeServerException;
    static List<String> getRequiredProperties(){return new ArrayList<>();}
}
