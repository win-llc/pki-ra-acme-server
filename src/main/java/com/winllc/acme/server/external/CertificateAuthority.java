package com.winllc.acme.server.external;

import com.winllc.acme.common.CAValidationRule;
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

public interface CertificateAuthority {

    String getName();
    boolean revokeCertificate(X509Certificate certificate, int reason) throws AcmeServerException;
    X509Certificate issueCertificate(OrderData orderData, PKCS10CertificationRequest certificationRequest) throws AcmeServerException;
    boolean isCertificateRevoked(X509Certificate certificate);
    Certificate[] getTrustChain();
    List<CAValidationRule> getValidationRules(AccountData accountData);
    boolean canIssueToIdentifier(Identifier identifier, AccountData accountData);
    List<ChallengeType> getIdentifierChallengeRequirements(Identifier identifier, AccountData accountData);
}
