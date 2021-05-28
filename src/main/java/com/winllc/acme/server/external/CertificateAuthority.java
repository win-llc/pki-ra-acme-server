package com.winllc.acme.server.external;

import com.winllc.acme.common.AcmeCertAuthorityType;
import com.winllc.acme.common.CertIssuanceValidationResponse;
import com.winllc.acme.common.CertRevocationStatus;
import com.winllc.acme.common.CertificateDetails;
import com.winllc.acme.common.contants.ChallengeType;
import com.winllc.acme.common.model.acme.Identifier;
import com.winllc.acme.common.model.data.AccountData;
import com.winllc.acme.common.model.data.DirectoryData;
import com.winllc.acme.server.exceptions.AcmeServerException;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CertificateAuthority {

    String getName();
    AcmeCertAuthorityType getType();
    boolean revokeCertificate(X509Certificate certificate, int reason) throws AcmeServerException;
    X509Certificate issueCertificate(Collection<Identifier> identifiers, String eabKid, PKCS10CertificationRequest certificationRequest) throws AcmeServerException;
    Optional<CertificateDetails> getCertificateDetails(String serial);
    CertRevocationStatus isCertificateRevoked(X509Certificate certificate);
    Certificate[] getTrustChain() throws AcmeServerException;
    CertIssuanceValidationResponse getValidationRules(AccountData accountData, DirectoryData directoryData) throws AcmeServerException;
    boolean canIssueToIdentifier(Identifier identifier, AccountData accountData, DirectoryData directoryData) throws AcmeServerException;
    List<ChallengeType> getIdentifierChallengeRequirements(Identifier identifier, AccountData accountData, DirectoryData directoryData) throws AcmeServerException;
    static List<String> getRequiredProperties(){return new ArrayList<>();}
}
