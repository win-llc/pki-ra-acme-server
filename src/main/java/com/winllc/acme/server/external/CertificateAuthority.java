package com.winllc.acme.server.external;

import com.winllc.acme.common.AccountValidationResponse;
import com.winllc.acme.common.CAValidationRule;
import com.winllc.acme.common.CertificateDetails;
import com.winllc.acme.server.contants.ChallengeType;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.model.acme.Account;
import com.winllc.acme.server.model.acme.Challenge;
import com.winllc.acme.server.model.acme.Identifier;
import com.winllc.acme.server.model.data.AccountData;
import com.winllc.acme.server.model.data.OrderData;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;

public interface CertificateAuthority {

    String getName();
    boolean revokeCertificate(X509Certificate certificate, int reason) throws AcmeServerException;
    X509Certificate issueCertificate(OrderData orderData, PKCS10CertificationRequest certificationRequest) throws AcmeServerException;
    Optional<CertificateDetails> getCertificateDetails(String serial);
    boolean isCertificateRevoked(X509Certificate certificate);
    Certificate[] getTrustChain() throws AcmeServerException;
    AccountValidationResponse getValidationRules(AccountData accountData) throws AcmeServerException;
    boolean canIssueToIdentifier(Identifier identifier, AccountData accountData) throws AcmeServerException;
    List<ChallengeType> getIdentifierChallengeRequirements(Identifier identifier, AccountData accountData) throws AcmeServerException;
}
