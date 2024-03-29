package com.winllc.acme.server;

import com.winllc.acme.common.*;
import com.winllc.acme.common.constants.ChallengeType;
import com.winllc.acme.common.constants.ProblemType;
import com.winllc.acme.common.model.acme.Identifier;
import com.winllc.acme.common.model.data.AccountData;
import com.winllc.acme.common.model.data.DirectoryData;
import com.winllc.acme.common.util.CertUtil;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.external.CertificateAuthority;
import com.winllc.ra.integration.ca.CertificateDetails;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class MockCertificateAuthority implements CertificateAuthority {

    private final String testX509Cert = "-----BEGIN CERTIFICATE-----\n" +
            "MIID1DCCArygAwIBAgIBJTANBgkqhkiG9w0BAQsFADBtMRMwEQYKCZImiZPyLGQB\n" +
            "GRYDY29tMRowGAYKCZImiZPyLGQBGRYKd2lubGxjLWRldjETMBEGCgmSJomT8ixk\n" +
            "ARkWA3BraTESMBAGCgmSJomT8ixkARkWAmNhMREwDwYDVQQDDAhXSU4gUk9PVDAe\n" +
            "Fw0yMDA0MjYxMzA4MjFaFw0yMjA0MTYxMzA4MjFaMCYxJDAiBgNVBAMMG2luZ3Jl\n" +
            "c3Mua3ViZS53aW5sbGMtZGV2LmNvbTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCC\n" +
            "AQoCggEBAMoI4iJlZAve6SZHBVL4mkmzLQ4NjyJTSx+tF9qAmh5/IP6c2bxqqKr2\n" +
            "aauNaYStU+7oWMxu7FMk/1atfWQ4ruZoSO9Eqx1sQqNr2obz23gHtAwWvOFxmmYQ\n" +
            "kvpJPPuuU8qUGpLKy1bQMLioptDLbnbbFcZcBJGbhdyJmyjxC9sOIOXqGrfBdpqw\n" +
            "dD6R3POL1CGchwO4C821x7ngGOqlfX/ysfuJsVsACYXiowHGvSBXsZt8gSb8EeFv\n" +
            "Kdcfziv4RGAqXB/jl4pF0WUcqXTo1ZdFtCi2KvLYOXD/Kdm2gMQ3GiCD7VfQb7bC\n" +
            "44vr4azxo0sC51sx6US/Jjidh+LbXqsCAwEAAaOBxTCBwjAfBgNVHSMEGDAWgBRG\n" +
            "EhS/MD8CvuMH3HIn1ytachRxuzAmBgNVHREEHzAdghtpbmdyZXNzLmt1YmUud2lu\n" +
            "bGxjLWRldi5jb20wSAYIKwYBBQUHAQEEPDA6MDgGCCsGAQUFBzABhixodHRwOi8v\n" +
            "ZG9ndGFnLWNhLndpbmxsYy1kZXYuY29tOjgwODAvY2Evb2NzcDAOBgNVHQ8BAf8E\n" +
            "BAMCBLAwHQYDVR0lBBYwFAYIKwYBBQUHAwEGCCsGAQUFBwMCMA0GCSqGSIb3DQEB\n" +
            "CwUAA4IBAQBI5COMesYLsSxc2Tx54QtvzeNdecLqdboUpnVY842WcXCtI/CtvBhV\n" +
            "qKq4nCB7znpItuB7cgVn0Hxwtxr2w0wUfVtWxAklmj0Y3+sHFR3EG6zO3pbqPRT7\n" +
            "IBJvnvNLlmxMKy5zP1edn0DV/DFGJuBbMXOsVqw9xMNQj0IM9tIsjTT2tuU5AqVa\n" +
            "whrg05qNTuU3XRGc605eyzek0kXd6zrjaGS4YrN/9U533ncsEs1M+SIlpocvinRD\n" +
            "+2/vl1YfoDobxdSbWXYrgpxMBRYMbLcOwrXChT1v5FLYJqtpPEO4VkSQZkOy4vdR\n" +
            "JJjhv4LdCnyD/RT6lxXzMVzBqX5721Hu\n" +
            "-----END CERTIFICATE-----";

    @Override
    public String getName() {
        return "test";
    }

    @Override
    public AcmeCertAuthorityType getType() {
        return AcmeCertAuthorityType.INTERNAL;
    }

    @Override
    public boolean revokeCertificate(X509Certificate certificate, int reason) throws AcmeServerException {
        return true;
    }

    @Override
    public X509Certificate issueCertificate(Collection<Identifier> identifiers, String eabKid,
                                            PKCS10CertificationRequest certificationRequest) throws AcmeServerException {
        try {
            return CertUtil.base64ToCert(testX509Cert);
        } catch (Exception e) {
            e.printStackTrace();
            throw new AcmeServerException(ProblemType.SERVER_INTERNAL);
        }
    }

    @Override
    public Optional<CertificateDetails> getCertificateDetails(String serial) {
        CertificateDetails certificateDetails = new CertificateDetails();
        certificateDetails.setCertificateBase64(testX509Cert);

        return Optional.of(certificateDetails);
    }

    @Override
    public CertRevocationStatus isCertificateRevoked(X509Certificate certificate) {
        return CertRevocationStatus.REVOKED;
    }

    @Override
    public Certificate[] getTrustChain() throws AcmeServerException {
        return new Certificate[0];
    }

    @Override
    public CertIssuanceValidationResponse getValidationRules(AccountData accountData, DirectoryData directoryData)
            throws AcmeServerException {
        CertIssuanceValidationRule validationRule = new CertIssuanceValidationRule();

        CertIssuanceValidationResponse validationResponse = new CertIssuanceValidationResponse("account1");
        validationResponse.setAccountIsValid(true);
        validationResponse.setCertIssuanceValidationRules(Collections.singletonList(validationRule));
        return validationResponse;
    }

    @Override
    public boolean canIssueToIdentifier(Identifier identifier, AccountData accountData, DirectoryData directoryData)
            throws AcmeServerException {
        return true;
    }

    @Override
    public List<ChallengeType> getIdentifierChallengeRequirements(Identifier identifier, AccountData accountData, DirectoryData directoryData)
            throws AcmeServerException {
        return Collections.singletonList(ChallengeType.HTTP);
    }
}
