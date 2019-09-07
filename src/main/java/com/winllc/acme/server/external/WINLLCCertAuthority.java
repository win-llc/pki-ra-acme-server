package com.winllc.acme.server.external;

import com.winllc.acme.common.CertificateAuthoritySettings;
import com.winllc.acme.server.contants.ChallengeType;
import com.winllc.acme.server.model.acme.Identifier;
import com.winllc.acme.server.model.data.AccountData;
import com.winllc.acme.server.model.data.OrderData;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;

public class WINLLCCertAuthority extends AbstractCertAuthority {

    public WINLLCCertAuthority(CertificateAuthoritySettings settings) {
        super(settings);
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public boolean revokeCertificate(X509Certificate certificate, int reason) {
        return false;
    }

    @Override
    public X509Certificate issueCertificate(OrderData orderData, PKCS10CertificationRequest certificationRequest) {
        return null;
    }

    @Override
    public boolean isCertificateRevoked(X509Certificate certificate) {
        return false;
    }

    @Override
    public Certificate[] getTrustChain() {
        return new Certificate[0];
    }

    @Override
    public List<CAValidationRule> getValidationRules(AccountData accountData) {
        return null;
    }

    @Override
    public boolean canIssueToIdentifier(Identifier identifier, AccountData accountData) {
        return false;
    }

    @Override
    public List<ChallengeType> getIdentifierChallengeRequirements(Identifier identifier, AccountData accountData) {
        return null;
    }
}
